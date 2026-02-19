package dev.donhempsmyer.huntcozy.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.network.OpenMeteoClient;
import dev.donhempsmyer.huntcozy.data.network.WeatherApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpenMeteoWeatherRepository implements WeatherRepository {

    private static final String TAG = "OpenMeteoWeatherRepo";

    private static final String CURRENT_PARAMS =
            "temperature_2m,apparent_temperature,precipitation,wind_speed_10m,wind_direction_10m,surface_pressure";

    private static final String HOURLY_PARAMS =
            "temperature_2m,apparent_temperature,precipitation,snowfall,snow_depth,wind_speed_10m,wind_direction_10m,surface_pressure";

    private static final String DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,snowfall_sum,sunrise,sunset";

    // --- Singleton instance ---
    private static OpenMeteoWeatherRepository INSTANCE;

    public static synchronized OpenMeteoWeatherRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OpenMeteoWeatherRepository();
        }
        return INSTANCE;
    }

    private final WeatherApiService api;
    private final MutableLiveData<WeatherResponse> weatherLiveData = new MutableLiveData<>();

    // Private constructor for singleton
    private OpenMeteoWeatherRepository() {
        this.api = OpenMeteoClient.getInstance();
    }

    @Override
    public LiveData<WeatherResponse> getWeatherLiveData() {
        return weatherLiveData;
    }

    /**
     * Convenience method: takes a HuntLocation and delegates to refreshWeatherForLocation.
     */
    @Override
    public void fetchWeatherFor(HuntLocation location) {
        if (location == null) {
            Log.w(TAG, "fetchWeatherFor: location is null, ignoring");
            return;
        }
        Log.d(TAG, "fetchWeatherFor: " + location.getName());
        refreshWeatherForLocation(location.getLatitude(), location.getLongitude());
    }

    /**
     * Core network call: fetches weather for the given lat/lon and posts to LiveData.
     */
    @Override
    public void refreshWeatherForLocation(double latitude, double longitude) {
        Log.d(TAG, "refreshWeatherForLocation: lat=" + latitude + ", lon=" + longitude);

        Call<WeatherResponse> call = api.getHuntingWeather(
                latitude,
                longitude,
                CURRENT_PARAMS,
                HOURLY_PARAMS,
                DAILY_PARAMS,
                "auto",
                7,
                "fahrenheit",   // temperature_unit
                "mph",          // wind_speed_unit
                "inHg",         // pressure_unit? (depends on your Retrofit signature)
                "inch",         // precipitation_unit
                "inch"          // snowfall_unit
        );

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call,
                                   Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse body = response.body();
                    if (body != null) {
                        Log.d(TAG, "onResponse: success, api timezone=" + body.timezone);
                        if (body.daily != null && body.daily.sunrise != null
                                && !body.daily.sunrise.isEmpty()) {
                            Log.d(TAG, "onResponse: first sunrise=" + body.daily.sunrise.get(0));
                        }
                    }
                    weatherLiveData.postValue(body);
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
    // - Use Kotlin coroutines + Flow with Retrofit suspend functions.
    // - Wrap responses in Result<> to surface errors to UI.
}