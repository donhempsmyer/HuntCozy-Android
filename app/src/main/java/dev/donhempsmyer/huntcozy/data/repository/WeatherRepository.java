package dev.donhempsmyer.huntcozy.data.repository;


import androidx.lifecycle.LiveData;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;

public interface WeatherRepository {

    LiveData<WeatherResponse> getWeatherLiveData();

    void fetchWeatherFor(HuntLocation location);

    void refreshWeatherForLocation(double latitude, double longitude);

    // Optional future extension: add cache, error LiveData, loading state, etc.
}