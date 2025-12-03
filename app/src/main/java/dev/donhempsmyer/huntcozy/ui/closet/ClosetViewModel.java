package dev.donhempsmyer.huntcozy.ui.closet;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;

import java.util.List;

/**
 * ClosetViewModel exposes the list of gear items to the Closet UI.
 *
 * v1: backed by InMemoryClosetRepository.
 * future: switch to Firebase/Room by providing a different ClosetRepository implementation.
 */
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
}