package dev.donhempsmyer.huntcozy.data.model.weather;


import com.google.gson.annotations.SerializedName;

public class CurrentWeather {

    public String time;

    @SerializedName("temperature_2m")
    public double temperature2m;

    @SerializedName("apparent_temperature")
    public double apparentTemperature;

    @SerializedName("precipitation")
    public double precipitation;

    @SerializedName("snowfall")
    public double snowfall;

    @SerializedName("wind_speed_10m")
    public double windSpeed10m;

    @SerializedName("wind_direction_10m")
    public double windDirection10m;

    @SerializedName("surface_pressure")
    public double barometricPressure;

    @SerializedName("relative_humidity_2m")
    public double relativeHumidity;

    @SerializedName("cloud_cover")
    public double cloudCover;
}