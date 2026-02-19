package dev.donhempsmyer.huntcozy.ui.locations;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

public class LocationsViewModel extends ViewModel {

    private final LocationsRepository locationsRepository;

    public LocationsViewModel() {
        locationsRepository = LocationsRepository.getInstance();
    }

    public LiveData<List<HuntLocation>> getLocations() {
        return locationsRepository.getLocations();
    }

    public LiveData<HuntLocation> getSelectedLocation() {
        return locationsRepository.getSelectedLocation();
    }

    public void selectLocation(HuntLocation loc) {
        if (loc != null) {
            locationsRepository.selectLocation(loc);
        }
    }
}
