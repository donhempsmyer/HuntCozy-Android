package dev.donhempsmyer.huntcozy.ui.closet;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;

public class ClosetViewModel extends ViewModel {

    private static final String TAG = "ClosetViewModel";

    private final ClosetRepository repository;

    public ClosetViewModel() {
        Log.d(TAG, "constructor: using ClosetRepositoryProvider");
        repository = ClosetRepositoryProvider.get();
    }

    /**
     * Live stream of all gear items in the user's closet.
     * Backed by Firestore via FirestoreClosetRepository / FirestoreGearRepository.
     */
    public LiveData<List<GearItem>> getGear() {
        return repository.getClosetLiveData();
    }

    /**
     * Add a new gear item to the closet.
     * Internally uses addOrUpdateGear on the repository.
     */
    public void addGear(GearItem item) {
        Log.d(TAG, "addGear via ViewModel: " + (item != null ? item.getName() : "null"));
        if (item == null) return;

        repository.addOrUpdateGear(item, new ClosetRepository.SimpleCallback() {
            @Override
            public void onComplete() {
                Log.d(TAG, "addGear: completed successfully");
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Log.e(TAG, "addGear: failed", t);
            }
        });
    }

    /**
     * Update an existing gear item.
     * Also uses addOrUpdateGear, but you can call it from an "edit" flow.
     */
    public void updateGear(GearItem item) {
        Log.d(TAG, "updateGear via ViewModel: " + (item != null ? item.getId() : "null"));
        if (item == null) return;

        repository.addOrUpdateGear(item, new ClosetRepository.SimpleCallback() {
            @Override
            public void onComplete() {
                Log.d(TAG, "updateGear: completed successfully");
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Log.e(TAG, "updateGear: failed", t);
            }
        });
    }

    /**
     * Delete a gear item by its id.
     */
    public void deleteGear(String gearId) {
        Log.d(TAG, "deleteGear via ViewModel: " + gearId);
        if (gearId == null) return;

        repository.deleteGear(gearId, new ClosetRepository.SimpleCallback() {
            @Override
            public void onComplete() {
                Log.d(TAG, "deleteGear: completed successfully");
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Log.e(TAG, "deleteGear: failed", t);
            }
        });
    }

    // Optional convenience if you ever want a manual "refresh" (not strictly needed
    // since Firestore listeners stream updates automatically).
    public void refreshCloset() {
        repository.loadCloset(new ClosetRepository.LoadClosetCallback() {
            @Override
            public void onClosetLoaded(@NonNull List<GearItem> items) {
                Log.d(TAG, "refreshCloset: loaded snapshot size=" + items.size());
                // No need to push into LiveData here; Firestore listeners already do that.
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Log.e(TAG, "refreshCloset: failed", t);
            }
        });
    }
}