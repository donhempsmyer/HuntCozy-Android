package dev.donhempsmyer.huntcozy.data.repository;


import androidx.lifecycle.LiveData;

import dev.donhempsmyer.huntcozy.data.model.LocationModel;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;

public interface WeatherRepository {

    LiveData<WeatherResponse> getWeatherLiveData();

    void fetchWeatherFor(LocationModel location);

    // Optional future extension: add cache, error LiveData, loading state, etc.
}