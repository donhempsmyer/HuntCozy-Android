package dev.donhempsmyer.huntcozy.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;

/**
 * Firestore-backed implementation of ClosetRepository.
 *
 * Uses FirestoreGearRepository for the live "stream" of closet items,
 * and UserFirestore for one-shot read/write operations with callbacks.
 */
public class FirestoreClosetRepository implements ClosetRepository {

    private static final String TAG = "FirestoreClosetRepo";

    private final UserFirestore userFs;
    private final FirestoreGearRepository gearStream;

    public FirestoreClosetRepository(@NonNull UserFirestore userFs) {
        this.userFs = userFs;
        this.gearStream = new FirestoreGearRepository(userFs);
    }

    // ------------------------------------------------------------------------
    // LiveData stream
    // ------------------------------------------------------------------------

    @NonNull
    @Override
    public LiveData<List<GearItem>> getClosetLiveData() {
        // Just expose the underlying FirestoreGearRepository LiveData
        return gearStream.getGear();
    }

    // ------------------------------------------------------------------------
    // One-shot read
    // ------------------------------------------------------------------------

    @Override
    public void loadCloset(@NonNull LoadClosetCallback callback) {
        userFs.gearCollection()
                .whereNotEqualTo("id", "_meta")
                .get()
                .addOnSuccessListener(snap -> {
                    List<GearItem> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        if ("_meta".equals(doc.getId())) continue;
                        GearItem item = GearFirestoreMapper.fromSnapshot(doc);
                        if (item != null) {
                            result.add(item);
                        }
                    }
                    Log.d(TAG, "loadCloset: loaded count=" + result.size());
                    callback.onClosetLoaded(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadCloset: failed", e);
                    callback.onError(e);
                });
    }

    // ------------------------------------------------------------------------
    // Bulk save (v1: upsert-only, no deletes)
    // ------------------------------------------------------------------------

    @Override
    public void saveCloset(@NonNull List<GearItem> items,
                           @NonNull SaveClosetCallback callback) {
        if (items == null) {
            items = Collections.emptyList();
        }

        if (items.isEmpty()) {
            Log.d(TAG, "saveCloset: nothing to save (no deletes handled yet)");
            callback.onClosetSaved();
            return;
        }

        final int total = items.size();
        final AtomicInteger remaining = new AtomicInteger(total);
        final AtomicBoolean errorFlag = new AtomicBoolean(false);

        for (GearItem item : items) {
            if (item == null) {
                if (remaining.decrementAndGet() == 0 && !errorFlag.get()) {
                    callback.onClosetSaved();
                }
                continue;
            }

            String docId = item.getId();
            Map<String, Object> data = GearFirestoreMapper.toMap(item);

            userFs.gearCollection()
                    .document(docId)
                    .set(data)
                    .addOnSuccessListener(v -> {
                        if (remaining.decrementAndGet() == 0 && !errorFlag.get()) {
                            Log.d(TAG, "saveCloset: finished upserts total=" + total);
                            callback.onClosetSaved();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "saveCloset: failed for " + docId, e);
                        if (errorFlag.compareAndSet(false, true)) {
                            callback.onError(e);
                        }
                    });
        }

        // NOTE:
        // This v1 implementation does NOT delete gear that was removed
        // from the list. If you want "replace everything", we can add
        // a pass that fetches existing docs and deletes ones not in `items`.
    }

    // ------------------------------------------------------------------------
    // Single-item write helpers
    // ------------------------------------------------------------------------

    @Override
    public void addOrUpdateGear(@NonNull GearItem item,
                                @NonNull SimpleCallback callback) {
        String docId = item.getId();
        Map<String, Object> data = GearFirestoreMapper.toMap(item);

        userFs.gearCollection()
                .document(docId)
                .set(data)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "addOrUpdateGear: saved " + docId);
                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addOrUpdateGear: failed " + docId, e);
                    callback.onError(e);
                });
    }

    @Override
    public void deleteGear(@NonNull String gearId,
                           @NonNull SimpleCallback callback) {
        userFs.gearCollection()
                .document(gearId)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "deleteGear: removed " + gearId);
                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteGear: failed " + gearId, e);
                    callback.onError(e);
                });
    }
}