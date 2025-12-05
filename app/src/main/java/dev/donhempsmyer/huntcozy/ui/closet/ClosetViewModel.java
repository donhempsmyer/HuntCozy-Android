package dev.donhempsmyer.huntcozy.ui.closet;

import android.util.Log;

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

    public LiveData<List<GearItem>> getGear() {
        return repository.getAllGear();
    }

    public void addGear(GearItem item) {
        Log.d(TAG, "addGear via ViewModel: " + (item != null ? item.getName() : "null"));
        repository.addGear(item);
    }

    public void updateGear(GearItem item) {
        Log.d(TAG, "updateGear via ViewModel: " + (item != null ? item.getId() : "null"));
        repository.updateGear(item);
    }

    public void deleteGear(String gearId) {
        Log.d(TAG, "deleteGear via ViewModel: " + gearId);
        repository.deleteGear(gearId);
    }

    // Alternate approach:
    // - Add updateGear(...) and call repository.updateGear for edit screen.
}