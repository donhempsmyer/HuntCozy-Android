package dev.donhempsmyer.huntcozy.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

public interface ClosetRepository {

    LiveData<List<GearItem>> getAllGear();

    void addGear(GearItem item);

    void updateGear(GearItem item);

    void deleteGear(String gearId);

    // Alternate approach:
    // - Use callbacks / Result objects for async Firebase writes.
}
