package dev.donhempsmyer.huntcozy.data.locations;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

/**
 * LocationsRepository manages saved hunting locations.
 *
 * v1:
 *  - in-memory list seeded at startup.
 * v2:
 *  - back with Room / Firebase (but keep this interface as a façade).
 */
public class LocationsRepository {

    private static final String TAG = "LocationsRepository";

    private static LocationsRepository INSTANCE;

    private final MutableLiveData<List<HuntLocation>> locationsLiveData =
            new MutableLiveData<>();

    // NEW: currently selected location
    private final MutableLiveData<HuntLocation> selectedLocationLiveData =
            new MutableLiveData<>();

    private long nextId = 1000L;

    private LocationsRepository() {
        List<HuntLocation> seeded = LocationsSeeder.seedDefaultLocations();
        locationsLiveData.setValue(seeded);
        nextId = seeded.size() + 1L;
        Log.d(TAG, "init: seeded " + seeded.size() + " locations");

        // Default to first location if present
        if (!seeded.isEmpty()) {
            selectedLocationLiveData.setValue(seeded.get(0));
            Log.d(TAG, "init: default selectedLocation=" + seeded.get(0));
        }
    }

    public static synchronized LocationsRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LocationsRepository();
        }
        return INSTANCE;
    }

    public LiveData<List<HuntLocation>> getLocations() {
        return locationsLiveData;
    }

    // NEW
    public LiveData<HuntLocation> getSelectedLocation() {
        return selectedLocationLiveData;
    }

    // NEW
    public void selectLocation(HuntLocation location) {
        if (location == null) return;
        Log.d(TAG, "selectLocation: " + location);
        selectedLocationLiveData.setValue(location);
    }

    public void addLocation(HuntLocation location) {
        if (location == null) return;

        List<HuntLocation> current = locationsLiveData.getValue();
        if (current == null) {
            current = new ArrayList<>();
        } else {
            // avoid mutating list that observers may be holding
            current = new ArrayList<>(current);
        }

        current.add(location);
        locationsLiveData.setValue(current);
    }

    public void addLocation(String name, double lat, double lon) {
        List<HuntLocation> current = locationsLiveData.getValue();
        if (current == null) current = new ArrayList<>();

        HuntLocation location = new HuntLocation(nextId++, name, lat, lon);
        current = new ArrayList<>(current);
        current.add(location);

        Log.d(TAG, "addLocation: " + location);
        locationsLiveData.setValue(current);

        // Alternate approach:
        // - Only auto-select new location if none selected yet.
    }
}
