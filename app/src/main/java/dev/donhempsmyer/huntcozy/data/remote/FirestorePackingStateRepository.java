package dev.donhempsmyer.huntcozy.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;

public class FirestorePackingStateRepository {

    private static final String TAG = "FirestorePackingStateRepo";

    public interface LoadCallback {
        void onLoaded(@NonNull PackingStateSnapshot snapshot);
        void onEmpty();     // doc exists but no meaningful data, or not found
        void onError(@NonNull Throwable t);
    }

    // Simple POJO snapshot of the packing lists + context
    public static class PackingStateSnapshot {
        @NonNull public final List<PackingItem> staged;
        @NonNull public final List<PackingItem> packed;
        @NonNull public final List<PackingItem> weapon;
        @NonNull public final List<PackingItem> style;
        @NonNull public final List<PackingItem> every;
        @NonNull public final List<PackingItem> optional;

        @Nullable public final WeaponType weaponType;
        @Nullable public final HuntingStyle huntingStyle;

        public PackingStateSnapshot(@NonNull List<PackingItem> staged,
                                    @NonNull List<PackingItem> packed,
                                    @NonNull List<PackingItem> weapon,
                                    @NonNull List<PackingItem> style,
                                    @NonNull List<PackingItem> every,
                                    @NonNull List<PackingItem> optional,
                                    @Nullable WeaponType weaponType,
                                    @Nullable HuntingStyle huntingStyle) {
            this.staged = staged;
            this.packed = packed;
            this.weapon = weapon;
            this.style = style;
            this.every = every;
            this.optional = optional;
            this.weaponType = weaponType;
            this.huntingStyle = huntingStyle;
        }
    }

    // --- Singleton plumbing --------------------------------------------------

    private static FirestorePackingStateRepository INSTANCE;

    public static synchronized void init(@NonNull UserFirestore userFs) {
        if (INSTANCE == null) {
            INSTANCE = new FirestorePackingStateRepository(userFs);
        }
    }

    @NonNull
    public static synchronized FirestorePackingStateRepository get() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "FirestorePackingStateRepository not initialized. " +
                            "Call FirestorePackingStateRepository.init(...) after sign-in."
            );
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------

    private final UserFirestore userFs;

    private FirestorePackingStateRepository(@NonNull UserFirestore userFs) {
        this.userFs = userFs;
    }

    private DocumentReference currentDoc() {
        return userFs.packingStateCollection().document("current");
    }

    // --- Load once -----------------------------------------------------------

    public void loadOnce(@NonNull LoadCallback callback) {
        currentDoc().get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.d(TAG, "loadOnce: no packingState/current doc");
                        callback.onEmpty();
                        return;
                    }
                    PackingStateSnapshot snapshot = fromDoc(doc);
                    if (snapshot == null) {
                        callback.onEmpty();
                    } else {
                        callback.onLoaded(snapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadOnce: failed", e);
                    callback.onError(e);
                });
    }

    // --- Save ----------------------------------------------------------------

    public void save(@NonNull PackingStateSnapshot snapshot) {
        Map<String, Object> map = new HashMap<>();
        map.put("staged",
                PackingItemFirestoreMapper.toMapList(snapshot.staged));
        map.put("packed",
                PackingItemFirestoreMapper.toMapList(snapshot.packed));
        map.put("weaponLoadout",
                PackingItemFirestoreMapper.toMapList(snapshot.weapon));
        map.put("styleLoadout",
                PackingItemFirestoreMapper.toMapList(snapshot.style));
        map.put("everyHunt",
                PackingItemFirestoreMapper.toMapList(snapshot.every));
        map.put("optional",
                PackingItemFirestoreMapper.toMapList(snapshot.optional));

        map.put("weaponType",
                snapshot.weaponType != null ? snapshot.weaponType.name() : null);
        map.put("huntingStyle",
                snapshot.huntingStyle != null ? snapshot.huntingStyle.name() : null);

        map.put("lastUpdated", FieldValue.serverTimestamp());

        currentDoc()
                .set(map, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "save: packingState/current updated"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "save: failed", e));
    }

    // --- Mapping from Firestore doc -----------------------------------------

    @Nullable
    private PackingStateSnapshot fromDoc(@NonNull DocumentSnapshot doc) {
        // If all arrays are absent, treat as empty
        boolean hasAnyList =
                doc.contains("staged") ||
                        doc.contains("packed") ||
                        doc.contains("weaponLoadout") ||
                        doc.contains("styleLoadout") ||
                        doc.contains("everyHunt") ||
                        doc.contains("optional");

        if (!hasAnyList) {
            return null;
        }

        List<PackingItem> staged = readList(doc, "staged", PackingItem.Source.STAGED);
        List<PackingItem> packed = readList(doc, "packed", PackingItem.Source.EVERY_HUNT);
        List<PackingItem> weapon = readList(doc, "weaponLoadout", PackingItem.Source.WEAPON);
        List<PackingItem> style = readList(doc, "styleLoadout", PackingItem.Source.STYLE);
        List<PackingItem> every = readList(doc, "everyHunt", PackingItem.Source.EVERY_HUNT);
        List<PackingItem> optional = readList(doc, "optional", PackingItem.Source.OPTIONAL);

        String weaponTypeStr = doc.getString("weaponType");
        String huntingStyleStr = doc.getString("huntingStyle");

        WeaponType weaponType = null;
        if (weaponTypeStr != null) {
            try {
                weaponType = WeaponType.valueOf(weaponTypeStr);
            } catch (IllegalArgumentException ignored) { }
        }

        HuntingStyle huntingStyle = null;
        if (huntingStyleStr != null) {
            try {
                huntingStyle = HuntingStyle.valueOf(huntingStyleStr);
            } catch (IllegalArgumentException ignored) { }
        }

        return new PackingStateSnapshot(
                staged, packed, weapon, style, every, optional,
                weaponType, huntingStyle
        );
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private List<PackingItem> readList(@NonNull DocumentSnapshot doc,
                                       @NonNull String field,
                                       @NonNull PackingItem.Source defaultSource) {
        Object raw = doc.get(field);
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        return PackingItemFirestoreMapper.fromMapList(
                (List<?>) raw,
                defaultSource
        );
    }
}
