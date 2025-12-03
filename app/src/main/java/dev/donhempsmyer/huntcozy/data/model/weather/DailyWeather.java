package dev.donhempsmyer.huntcozy.data.model.weather;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyWeather {

    public List<String> time;

    @SerializedName("temperature_2m_max")
    public List<Double> temperatureMax;

    @SerializedName("temperature_2m_min")
    public List<Double> temperatureMin;

    @SerializedName("precipitation_sum")
    public List<Double> precipitationSum;

    @SerializedName("snowfall_sum")
    public List<Double> snowfallSum;

    public List<String> sunrise;
    public List<String> sunset;
}
