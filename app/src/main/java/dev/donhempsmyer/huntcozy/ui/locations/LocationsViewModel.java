package dev.donhempsmyer.huntcozy.ui.locations;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

/**
 * LocationsViewModel exposes the list of HuntLocation objects
 * for the LocationsFragment.
 */
public class LocationsViewModel extends ViewModel {

    private static final String TAG = "LocationsViewModel";

    private final LocationsRepository repository =
            LocationsRepository.getInstance();

    public LiveData<List<HuntLocation>> getLocations() {
        return repository.getLocations();
    }

    public void addLocation(String name, double lat, double lon) {
        Log.d(TAG, "addLocation: name=" + name + " lat=" + lat + " lon=" + lon);
        repository.addLocation(name, lat, lon);
    }

    // Alternate approach:
    // - Accept a DTO (LocationDraft) that has validation built-in
    //   and then call repository.addLocation(draft).
}
