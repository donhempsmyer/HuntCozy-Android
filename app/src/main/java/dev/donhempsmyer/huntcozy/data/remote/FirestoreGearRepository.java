package dev.donhempsmyer.huntcozy.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

public class FirestoreGearRepository {

    private static final String TAG = "FirestoreGearRepo";

    private final UserFirestore userFs;
    private final MutableLiveData<List<GearItem>> gearLiveData = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration registration;

    public FirestoreGearRepository(@NonNull UserFirestore userFs) {
        this.userFs = userFs;
        startListening();
    }

    private void startListening() {
        // Listen to all docs in /users/{uid}/gear excluding _meta
        registration = userFs.gearCollection()
                .whereNotEqualTo("id", "_meta") // extra safety; we already have doc name "_meta"
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "startListening: error", e);
                        return;
                    }
                    if (snap == null) {
                        Log.w(TAG, "startListening: snapshot null");
                        return;
                    }

                    List<GearItem> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        if ("_meta".equals(doc.getId())) continue;

                        GearItem item = GearFirestoreMapper.fromSnapshot(doc);
                        if (item != null) {
                            list.add(item);
                        }
                    }
                    Log.d(TAG, "startListening: loaded gear count=" + list.size());
                    gearLiveData.setValue(list);
                });
    }

    public LiveData<List<GearItem>> getGear() {
        return gearLiveData;
    }

    /**
     * Upsert: if item.id already exists, update; otherwise create.
     */
    public void upsert(@NonNull GearItem item) {
        String docId = item.getId();
        Map<String, Object> data = GearFirestoreMapper.toMap(item);
        userFs.gearCollection()
                .document(docId)
                .set(data)
                .addOnSuccessListener(v -> Log.d(TAG, "upsert: saved gear " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "upsert: failed " + docId, e));
    }

    public void delete(@NonNull GearItem item) {
        String docId = item.getId();
        userFs.gearCollection()
                .document(docId)
                .delete()
                .addOnSuccessListener(v -> Log.d(TAG, "delete: removed gear " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "delete: failed " + docId, e));
    }

    public void clear() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
