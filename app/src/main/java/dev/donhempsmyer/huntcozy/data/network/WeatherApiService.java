package dev.donhempsmyer.huntcozy.data.network;


import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("v1/forecast")
    Call<WeatherResponse> getHuntingWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,
            @Query("hourly") String hourly,
            @Query("daily") String daily,
            @Query("timezone") String timezone,
            @Query("forecast_days") int forecastDays
    );

    // Alternate approach:
    // - Create multiple endpoints (one for current, one for forecast) with more specific models.
}
