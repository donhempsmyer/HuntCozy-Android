package dev.donhempsmyer.huntcozy.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.SavedLoadout;

/**
 * Firestore-backed repository for SavedLoadout objects.
 *
 * Firestore structure:
 *   users/{uid}/loadouts/_meta  (from FirebaseSeeder)
 *   users/{uid}/loadouts/{loadoutId}  (actual loadout docs)
 */
public class FirestoreLoadoutRepository {

    private static final String TAG = "FirestoreLoadoutRepo";

    private static FirestoreLoadoutRepository INSTANCE;

    public static synchronized void init(@NonNull UserFirestore userFs) {
        if (INSTANCE == null) {
            INSTANCE = new FirestoreLoadoutRepository(userFs);
        }
    }

    @NonNull
    public static synchronized FirestoreLoadoutRepository get() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "FirestoreLoadoutRepository not initialized. " +
                            "Call FirestoreLoadoutRepository.init(...) after sign-in."
            );
        }
        return INSTANCE;
    }

    private final UserFirestore userFs;
    private final MutableLiveData<List<SavedLoadout>> savedLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration registration;

    private FirestoreLoadoutRepository(@NonNull UserFirestore userFs) {
        this.userFs = userFs;
        startListening();
    }

    private void startListening() {
        registration = userFs.loadoutsCollection()
                .whereNotEqualTo("id", "_meta")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "startListening: error", e);
                        return;
                    }
                    if (snap == null) return;

                    List<SavedLoadout> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        if ("_meta".equals(doc.getId())) continue;

                        SavedLoadout lo = SavedLoadoutFirestoreMapper.fromSnapshot(doc);
                        if (lo != null) {
                            list.add(lo);
                        }
                    }
                    Log.d(TAG, "startListening: loadouts count=" + list.size());
                    savedLiveData.setValue(list);
                });
    }

    @NonNull
    public LiveData<List<SavedLoadout>> getSavedLoadouts() {
        return savedLiveData;
    }

    public void saveLoadout(@NonNull SavedLoadout loadout) {
        String docId = loadout.getId();
        userFs.loadoutsCollection()
                .document(docId)
                .set(SavedLoadoutFirestoreMapper.toMap(loadout))
                .addOnSuccessListener(v ->
                        Log.d(TAG, "saveLoadout: saved " + docId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveLoadout: failed " + docId, e));
    }

    public void deleteLoadout(@NonNull String loadoutId) {
        userFs.loadoutsCollection()
                .document(loadoutId)
                .delete()
                .addOnSuccessListener(v ->
                        Log.d(TAG, "deleteLoadout: deleted " + loadoutId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "deleteLoadout: failed " + loadoutId, e));
    }

    public void clear() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
