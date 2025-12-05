package dev.donhempsmyer.huntcozy.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

public class InMemoryClosetRepository implements ClosetRepository {

    private static final String TAG = "InMemoryClosetRepo";

    private final MutableLiveData<List<GearItem>> gearLiveData = new MutableLiveData<>();

    public InMemoryClosetRepository() {
        Log.d(TAG, "constructor: loading seeded closet");
        List<GearItem> seeded = ClosetSeeder.createSampleCloset();
        gearLiveData.setValue(new ArrayList<>(seeded));
    }

    @Override
    public LiveData<List<GearItem>> getAllGear() {
        return gearLiveData;
    }

    @Override
    public void addGear(GearItem item) {
        Log.d(TAG, "addGear: " + (item != null ? item.getName() : "null"));
        List<GearItem> current = gearLiveData.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(item);
        gearLiveData.setValue(new ArrayList<>(current));
    }

    @Override
    public void updateGear(GearItem item) {
        if (item == null) return;
        Log.d(TAG, "updateGear: " + item.getId());
        List<GearItem> current = gearLiveData.getValue();
        if (current == null) return;

        List<GearItem> copy = new ArrayList<>(current);
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).getId().equals(item.getId())) {
                copy.set(i, item);
                break;
            }
        }
        gearLiveData.setValue(copy);
    }

    @Override
    public void deleteGear(String gearId) {
        Log.d(TAG, "deleteGear: " + gearId);
        List<GearItem> current = gearLiveData.getValue();
        if (current == null) return;

        List<GearItem> copy = new ArrayList<>(current);
        if (copy.removeIf(g -> g.getId().equals(gearId))) {
            gearLiveData.setValue(copy);
        }
    }

    // Alternate approach:
    // - Expose methods returning boolean / Result indicating success/failure.
}