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

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

public class FirestoreLocationRepository {

    private static final String TAG = "FirestoreLocationRepo";

    private final UserFirestore userFs;
    private final MutableLiveData<List<HuntLocation>> locationsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration registration;

    public FirestoreLocationRepository(@NonNull UserFirestore userFs) {
        this.userFs = userFs;
        startListening();
    }

    private void startListening() {
        registration = userFs.locationsCollection()
                .whereNotEqualTo("id", "_meta")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "startListening: error", e);
                        return;
                    }
                    if (snap == null) return;

                    List<HuntLocation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        if ("_meta".equals(doc.getId())) continue;
                        HuntLocation loc = LocationFirestoreMapper.fromSnapshot(doc);
                        if (loc != null) list.add(loc);
                    }
                    Log.d(TAG, "startListening: locations count=" + list.size());
                    locationsLiveData.setValue(list);
                });
    }

    public LiveData<List<HuntLocation>> getLocations() {
        return locationsLiveData;
    }

    public void upsert(@NonNull HuntLocation loc) {
        String docId = loc.getId();
        Map<String, Object> data = LocationFirestoreMapper.toMap(loc);
        userFs.locationsCollection()
                .document(docId)
                .set(data)
                .addOnSuccessListener(v -> Log.d(TAG, "upsert: saved location " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "upsert: failed " + docId, e));
    }

    public void delete(@NonNull HuntLocation loc) {
        String docId = loc.getId();
        userFs.locationsCollection()
                .document(docId)
                .delete()
                .addOnSuccessListener(v -> Log.d(TAG, "delete: removed location " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "delete: failed " + docId, e));
    }

    public void clear() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
