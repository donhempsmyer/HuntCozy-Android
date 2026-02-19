package dev.donhempsmyer.huntcozy.data.model.weather;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyWeather {

    @SerializedName("temperature_2m_max")
    public List<Double> temperatureMax;

    @SerializedName("temperature_2m_min")
    public List<Double> temperatureMin;

    @SerializedName("precipitation_sum")
    public List<Double> precipitationSum;

    @SerializedName("snowfall_sum")
    public List<Double> snowfallSum;

    @SerializedName("time")
    public List<String> time;        // "2025-12-06"

    @SerializedName("sunrise")
    public List<String> sunrise;     // "2025-12-06T07:14"

    @SerializedName("sunset")
    public List<String> sunset;      // "2025-12-06T16:30"
}
