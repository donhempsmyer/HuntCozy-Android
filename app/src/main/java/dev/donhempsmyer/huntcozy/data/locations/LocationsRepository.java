package dev.donhempsmyer.huntcozy.data.locations;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.UUID;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.remote.FirestoreLocationRepository;
import dev.donhempsmyer.huntcozy.data.remote.UserFirestore;

/**
 * LocationsRepository is a façade over FirestoreLocationRepository.
 *
 * Responsibilities:
 *  - Expose LiveData of all locations for the current user.
 *  - Track the "selected" location as separate LiveData.
 *  - Provide simple add/update/delete methods that delegate to Firestore.
 *
 * Usage:
 *
 *   // After Firebase sign-in (e.g. in MainActivity):
 *   UserFirestore userFs = new UserFirestore(db, user.getUid());
 *   LocationsRepository.init(userFs);
 *
 *   // Anywhere in ViewModels or Fragments:
 *   LocationsRepository repo = LocationsRepository.getInstance();
 *   LiveData<List<HuntLocation>> locations = repo.getLocations();
 *   LiveData<HuntLocation> selected = repo.getSelectedLocation();
 */
public final class LocationsRepository {

    private static final String TAG = "LocationsRepository";

    // --- Singleton wiring ---------------------------------------------------

    private static LocationsRepository INSTANCE;

    /**
     * Initialize the singleton with the current signed-in user.
     * Call this once after authentication, before any getInstance().
     */
    public static synchronized void init(@NonNull UserFirestore userFs) {
        if (INSTANCE != null) {
            Log.w(TAG, "init: LocationsRepository already initialized, ignoring");
            return;
        }
        Log.d(TAG, "init: creating LocationsRepository with Firestore backend");
        FirestoreLocationRepository remote = new FirestoreLocationRepository(userFs);
        INSTANCE = new LocationsRepository(remote);
    }

    /**
     * Get the singleton instance. Throws if init(...) has not been called yet.
     */
    @NonNull
    public static synchronized LocationsRepository getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "LocationsRepository not initialized. " +
                            "Call LocationsRepository.init(userFs) after sign-in."
            );
        }
        return INSTANCE;
    }

    // --- Instance state -----------------------------------------------------

    private final FirestoreLocationRepository remote;

    // Current selection for use by HomeViewModel, etc.
    private final MutableLiveData<HuntLocation> selectedLocationLiveData =
            new MutableLiveData<>();

    private LocationsRepository(@NonNull FirestoreLocationRepository remote) {
        this.remote = remote;

        // Keep selected location sane when remote list changes
        remote.getLocations().observeForever(locations -> {
            if (locations == null || locations.isEmpty()) {
                Log.d(TAG, "remote locations empty; clearing selection");
                selectedLocationLiveData.postValue(null);
                return;
            }

            HuntLocation current = selectedLocationLiveData.getValue();
            if (current == null) {
                // Default to first location if none selected yet
                Log.d(TAG, "no selection yet; defaulting to first location");
                selectedLocationLiveData.postValue(locations.get(0));
            } else {
                // If current selection was deleted, fall back to first
                boolean stillExists = false;
                for (HuntLocation loc : locations) {
                    if (loc != null && current.getId().equals(loc.getId())) {
                        stillExists = true;
                        break;
                    }
                }
                if (!stillExists) {
                    Log.d(TAG, "current selection removed; switching to first location");
                    selectedLocationLiveData.postValue(locations.get(0));
                }
            }
        });
    }

    // --- Public API ---------------------------------------------------------

    /** LiveData of all locations from Firestore. */
    public LiveData<List<HuntLocation>> getLocations() {
        return remote.getLocations();
    }

    /** LiveData of the currently selected location. */
    public LiveData<HuntLocation> getSelectedLocation() {
        return selectedLocationLiveData;
    }

    /** Set the currently selected location (does NOT write to Firestore). */
    public void selectLocation(@NonNull HuntLocation location) {
        Log.d(TAG, "selectLocation: " + location);
        selectedLocationLiveData.setValue(location);
    }

    /** Add or update a location in Firestore. */
    public void addOrUpdateLocation(@NonNull HuntLocation location) {
        Log.d(TAG, "addOrUpdateLocation: " + location);
        remote.upsert(location);
    }

    /**
     * Convenience method: create a new location with a generated String id,
     * write it to Firestore, and return the created HuntLocation.
     */
    @NonNull
    public HuntLocation addLocation(@NonNull String name, double lat, double lon) {
        String id = UUID.randomUUID().toString();
        // Adjust to your actual HuntLocation constructor (String id version)
        HuntLocation location = new HuntLocation(id, name, lat, lon, null, null);
        Log.d(TAG, "addLocation: " + location);
        remote.upsert(location);
        return location;
    }

    /** Delete a location from Firestore. */
    public void deleteLocation(@NonNull HuntLocation location) {
        Log.d(TAG, "deleteLocation: " + location);
        remote.delete(location);
    }

    /** Optional: stop listening to Firestore. */
    public void clear() {
        remote.clear();
    }
}
