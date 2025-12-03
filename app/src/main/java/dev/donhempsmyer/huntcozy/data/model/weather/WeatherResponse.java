package dev.donhempsmyer.huntcozy.data.model.weather;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {

    public double latitude;
    public double longitude;
    public double elevation;
    public String timezone;

    @SerializedName("current")
    public CurrentWeather current;

    @SerializedName("hourly")
    public HourlyWeather hourly;

    @SerializedName("daily")
    public DailyWeather daily;

    // Note: we keep it simple; getters can be added as needed.
}