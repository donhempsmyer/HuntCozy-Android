package dev.donhempsmyer.huntcozy.ui.home;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.LocationModel;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.weather.CurrentWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;
import dev.donhempsmyer.huntcozy.data.repository.OpenMeteoWeatherRepository;
import dev.donhempsmyer.huntcozy.data.repository.WeatherRepository;
import dev.donhempsmyer.huntcozy.algo.GearRecommender;

import java.util.Arrays;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    private final WeatherRepository weatherRepository = new OpenMeteoWeatherRepository();
    private final ClosetRepository closetRepository = ClosetRepositoryProvider.get();
    private final GearRecommender gearRecommender = new GearRecommender();

    private final MutableLiveData<List<LocationModel>> locationsLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<LocationModel> selectedLocationLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<WeaponType> weaponTypeLiveData =
            new MutableLiveData<>(WeaponType.RIFLE);

    private final MutableLiveData<HuntingStyle> huntingStyleLiveData =
            new MutableLiveData<>(HuntingStyle.TREESTAND);

    // Recommended gear (output)
    private final MutableLiveData<List<GearItem>> gearLiveData =
            new MutableLiveData<>();

    public HomeViewModel() {
        Log.d(TAG, "constructor: initializing sample locations");
        List<LocationModel> sample = Arrays.asList(
                new LocationModel("loc1", "North Ridge Stand", 44.5, -89.5),
                new LocationModel("loc2", "Marsh Blind", 44.3, -89.3)
        );
        locationsLiveData.setValue(sample);
        if (!sample.isEmpty()) {
            selectedLocationLiveData.setValue(sample.get(0));
            refreshWeather();
        }
    }

    public LiveData<List<LocationModel>> getLocations() {
        return locationsLiveData;
    }

    public LiveData<LocationModel> getSelectedLocation() {
        return selectedLocationLiveData;
    }

    public void selectLocation(LocationModel location) {
        Log.d(TAG, "selectLocation: " + location.getName());
        selectedLocationLiveData.setValue(location);
        refreshWeather();
    }

    public LiveData<WeatherResponse> getWeather() {
        return weatherRepository.getWeatherLiveData();
    }

    public LiveData<List<GearItem>> getGear() {
        return gearLiveData;
    }

    public void setWeaponType(WeaponType type) {
        Log.d(TAG, "setWeaponType: " + type);
        weaponTypeLiveData.setValue(type);
        recomputeGear();
    }

    public void setHuntingStyle(HuntingStyle style) {
        Log.d(TAG, "setHuntingStyle: " + style);
        huntingStyleLiveData.setValue(style);
        recomputeGear();
    }

    public LiveData<WeaponType> getWeaponType() {
        return weaponTypeLiveData;
    }

    public LiveData<HuntingStyle> getHuntingStyle() {
        return huntingStyleLiveData;
    }

    private void refreshWeather() {
        LocationModel loc = selectedLocationLiveData.getValue();
        if (loc != null) {
            Log.d(TAG, "refreshWeather: " + loc.getName());
            weatherRepository.fetchWeatherFor(loc);
        }
    }

    public void onWeatherUpdated(WeatherResponse response) {
        Log.d(TAG, "onWeatherUpdated");
        recomputeGear();
    }

    private void recomputeGear() {
        WeatherResponse response = weatherRepository.getWeatherLiveData().getValue();
        WeaponType weaponType = weaponTypeLiveData.getValue();
        HuntingStyle huntingStyle = huntingStyleLiveData.getValue();

        if (response == null || response.current == null ||
                weaponType == null || huntingStyle == null) {
            Log.d(TAG, "recomputeGear: missing weather or selections, skipping");
            return;
        }

        List<GearItem> closet = closetRepository.getAllGear().getValue();
        if (closet == null || closet.isEmpty()) {
            Log.w(TAG, "recomputeGear: closet is empty, no recommendations");
            gearLiveData.setValue(null);
            return;
        }

        CurrentWeather current = response.current;
        List<GearItem> recommended = gearRecommender.recommendFromCloset(
                closet,
                current,
                weaponType,
                huntingStyle
        );

        gearLiveData.setValue(recommended);
    }

    // Alternate approach:
    // - Expose closet LiveData as well (getClosetGear()) if HomeFragment
    //   ever needs to show “all gear vs recommended gear” side by side.
}
