package dev.donhempsmyer.huntcozy.data.repository;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import dev.donhempsmyer.huntcozy.data.model.LocationModel;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.network.OpenMeteoClient;
import dev.donhempsmyer.huntcozy.data.network.WeatherApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpenMeteoWeatherRepository implements WeatherRepository {

    private static final String TAG = "OpenMeteoWeatherRepo";

    private static final String CURRENT_PARAMS =
            "temperature_2m,apparent_temperature,precipitation,wind_speed_10m,wind_direction_10m,pressure_msl";

    private static final String HOURLY_PARAMS =
            "temperature_2m,precipitation,snowfall,snow_depth,wind_speed_10m,wind_direction_10m,pressure_msl";

    private static final String DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,snowfall_sum,sunrise,sunset";

    private final WeatherApiService api;
    private final MutableLiveData<WeatherResponse> weatherLiveData = new MutableLiveData<>();

    public OpenMeteoWeatherRepository() {
        this.api = OpenMeteoClient.getInstance();
    }

    @Override
    public LiveData<WeatherResponse> getWeatherLiveData() {
        return weatherLiveData;
    }

    @Override
    public void fetchWeatherFor(LocationModel location) {
        Log.d(TAG, "fetchWeatherFor: " + location.getName());

        Call<WeatherResponse> call = api.getHuntingWeather(
                location.getLatitude(),
                location.getLongitude(),
                CURRENT_PARAMS,
                HOURLY_PARAMS,
                DAILY_PARAMS,
                "auto",
                7,
                "fahrenheit",   // temperature_unit
                "mph",          // wind_speed_unit
                "inch",         // precipitation_unit
                "inch"          // snowfall_unit
        );

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call,
                                   Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: success");
                    weatherLiveData.postValue(response.body());
                } else {
                    Log.w(TAG, "onResponse: HTTP error code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: error fetching weather", t);
            }
        });
    }

    // Alternate approach:
    // - Use Kotlin coroutines + Flow with Retrofit suspend functions (requires Kotlin).
    // - Wrap responses in Result<> type to handle errors explicitly.
}