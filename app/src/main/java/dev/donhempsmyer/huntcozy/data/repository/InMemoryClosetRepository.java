package dev.donhempsmyer.huntcozy.data.repository;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

import java.util.List;

/**
 * InMemoryClosetRepository uses a static seeded list for now.
 * No persistence – app restart resets the closet.
 *
 * Later, swap this out for a Room/Firebase implementation without
 * changing the UI layer.
 */
public class InMemoryClosetRepository implements ClosetRepository {

    private static final String TAG = "InMemoryClosetRepo";

    private final MutableLiveData<List<GearItem>> gearLiveData = new MutableLiveData<>();

    public InMemoryClosetRepository() {
        Log.d(TAG, "constructor: loading seeded closet");
        List<GearItem> seeded = ClosetSeeder.createSampleCloset();
        gearLiveData.setValue(seeded);
    }

    @Override
    public LiveData<List<GearItem>> getAllGear() {
        return gearLiveData;
    }

    // Alternate approach:
    // - Wrap this in a singleton so multiple ViewModels share the same source.
}