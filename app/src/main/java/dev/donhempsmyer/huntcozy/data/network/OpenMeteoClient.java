package dev.donhempsmyer.huntcozy.data.network;


import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenMeteoClient {

    private static final String TAG = "OpenMeteoClient";
    private static final String BASE_URL = "https://api.open-meteo.com/";

    private static WeatherApiService instance;

    public static WeatherApiService getInstance() {
        if (instance == null) {
            Log.d(TAG, "getInstance: building Retrofit");
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(WeatherApiService.class);
        }
        return instance;
    }
}
