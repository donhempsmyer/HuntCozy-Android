package dev.donhempsmyer.huntcozy.data.repository;


import androidx.lifecycle.LiveData;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

import java.util.List;

/**
 * ClosetRepository is the abstraction for accessing the user's gear closet.
 *
 * v1: in-memory seeded data.
 * future: Room or Firebase implementation behind the same interface.
 */
public interface ClosetRepository {

    LiveData<List<GearItem>> getAllGear();

    // v2+ (for Firebase / Room):
    // void addGear(GearItem item);
    // void updateGear(GearItem item);
    // void deleteGear(String gearId);
}
