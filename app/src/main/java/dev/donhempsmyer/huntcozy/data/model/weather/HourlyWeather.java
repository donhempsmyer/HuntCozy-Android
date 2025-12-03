package dev.donhempsmyer.huntcozy.data.model.weather;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HourlyWeather {

    public List<String> time;

    @SerializedName("temperature_2m")
    public List<Double> temperature2m;

    @SerializedName("precipitation")
    public List<Double> precipitation;

    @SerializedName("snowfall")
    public List<Double> snowfall;

    @SerializedName("snow_depth")
    public List<Double> snowDepth;

    @SerializedName("wind_speed_10m")
    public List<Double> windSpeed10m;

    @SerializedName("wind_direction_10m")
    public List<Double> windDirection10m;

    @SerializedName("pressure_msl")
    public List<Double> pressureMsl;
}
