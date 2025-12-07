package dev.donhempsmyer.huntcozy.ui.home;


import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntWindow;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.LocationModel;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.weather.CurrentWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.DailyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.HourlyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;
import dev.donhempsmyer.huntcozy.data.repository.OpenMeteoWeatherRepository;
import dev.donhempsmyer.huntcozy.data.repository.WeatherRepository;
import dev.donhempsmyer.huntcozy.algo.GearRecommender;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    private final WeatherRepository weatherRepository = new OpenMeteoWeatherRepository();
    private final ClosetRepository closetRepository = ClosetRepositoryProvider.get();
    private final GearRecommender gearRecommender = new GearRecommender();

    private final MutableLiveData<List<LocationModel>> locationsLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<LocationModel> selectedLocationLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<WeaponType> weaponTypeLiveData =
            new MutableLiveData<>(WeaponType.RIFLE);

    private final MutableLiveData<HuntingStyle> huntingStyleLiveData =
            new MutableLiveData<>(HuntingStyle.TREESTAND);


    private final MutableLiveData<HuntWindow> huntWindowLiveData =
            new MutableLiveData<>(HuntWindow.ALL_DAY);

    // Recommended gear (output)
    private final MutableLiveData<List<GearItem>> gearLiveData =
            new MutableLiveData<>();

    public HomeViewModel() {
        Log.d(TAG, "constructor: initializing sample locations");
        List<LocationModel> sample = Arrays.asList(
                new LocationModel("loc1", "North Ridge Stand", 44.5, -89.5),
                new LocationModel("loc2", "Marsh Blind", 54.3, -99.3),
                new LocationModel("loc3", "Marsh stand", 38.29, 122.3)

        );
        locationsLiveData.setValue(sample);
        if (!sample.isEmpty()) {
            selectedLocationLiveData.setValue(sample.get(0));
            refreshWeather();
        }
    }

    public LiveData<List<LocationModel>> getLocations() {
        return locationsLiveData;
    }

    public LiveData<LocationModel> getSelectedLocation() {
        return selectedLocationLiveData;
    }

    public void selectLocation(LocationModel location) {
        Log.d(TAG, "selectLocation: " + location.getName());
        selectedLocationLiveData.setValue(location);
        refreshWeather();
    }

    public LiveData<WeatherResponse> getWeather() {
        return weatherRepository.getWeatherLiveData();
    }

    public LiveData<HuntWindow> getHuntWindow() {
        return huntWindowLiveData;
    }

    public LiveData<List<GearItem>> getGear() {
        return gearLiveData;
    }

    public void setWeaponType(WeaponType type) {
        Log.d(TAG, "setWeaponType: " + type);
        weaponTypeLiveData.setValue(type);
        recomputeGear();
    }

    public void setHuntingStyle(HuntingStyle style) {
        Log.d(TAG, "setHuntingStyle: " + style);
        huntingStyleLiveData.setValue(style);
        recomputeGear();
    }

    public void setHuntWindow(HuntWindow window) {
        if (window == null) return;
        HuntWindow current = huntWindowLiveData.getValue();
        if (current == window) return;

        Log.d(TAG, "setHuntWindow: " + window);
        huntWindowLiveData.setValue(window);

        // Window change affects recommended outfit
        recomputeGear();
    }

    public LiveData<WeaponType> getWeaponType() {
        return weaponTypeLiveData;
    }

    public LiveData<HuntingStyle> getHuntingStyle() {
        return huntingStyleLiveData;
    }

    private void refreshWeather() {
        LocationModel loc = selectedLocationLiveData.getValue();
        if (loc != null) {
            Log.d(TAG, "refreshWeather: " + loc.getName());
            weatherRepository.fetchWeatherFor(loc);
        }
    }

    public void onWeatherUpdated(WeatherResponse response) {
        Log.d(TAG, "onWeatherUpdated");
        recomputeGear();
    }

    private void recomputeGear() {
        WeatherResponse response = weatherRepository.getWeatherLiveData().getValue();
        WeaponType weaponType = weaponTypeLiveData.getValue();
        HuntingStyle huntingStyle = huntingStyleLiveData.getValue();
        HuntWindow huntWindow = huntWindowLiveData.getValue();

        if (response == null || response.current == null ||
                weaponType == null || huntingStyle == null || huntWindow == null) {
            Log.d(TAG, "recomputeGear: missing weather, selections, or huntWindow; skipping");
            return;
        }

        List<GearItem> closet = closetRepository.getAllGear().getValue();
        if (closet == null || closet.isEmpty()) {
            Log.w(TAG, "recomputeGear: closet is empty, no outfit can be built");
            gearLiveData.setValue(null);
            return;
        }

        // v1: use current conditions as a proxy for the hunt window.
        // v2: replace this with an hourly-based min/max/wind/precip aggregation
        // for MORNING_MID / MID_EVENING / ALL_DAY.
        CurrentWeather windowWeather = deriveWindowWeather(response, huntWindow);

        List<GearItem> outfit = gearRecommender.buildOutfitFromCloset(
                closet,
                windowWeather,
                weaponType,
                huntingStyle
        );

        Log.d(TAG, "recomputeGear: window=" + huntWindow
                + " outfit size=" + (outfit != null ? outfit.size() : 0));
        gearLiveData.setValue(outfit);
    }

    /**
     * v1 implementation:
     *  - We simply return response.current as the representative weather.
     *
     * v2:
     *  - Use hourly forecast to compute:
     *      - min/max apparent temp
     *      - max wind
     *      - total precip
     *    across the selected HuntWindow for the selected day.
     */
    private CurrentWeather deriveWindowWeather(WeatherResponse response, HuntWindow window) {
        if (response == null || response.current == null) {
            Log.w(TAG, "deriveWindowWeather: no current weather, returning null");
            return null;
        }

        HourlyWeather hourly = response.hourly;
        DailyWeather daily = response.daily;
        if (hourly == null || daily == null ||
                hourly.time == null || hourly.time.isEmpty() ||
                daily.sunrise == null || daily.sunrise.isEmpty() ||
                daily.sunset == null || daily.sunset.isEmpty()) {
            Log.w(TAG, "deriveWindowWeather: missing hourly/daily sunrise/sunset, using current");
            return response.current;
        }

        // Build TimeZone from response.timezone, fallback to device default
        TimeZone tz;
        if (response.timezone != null && !response.timezone.isEmpty()) {
            tz = TimeZone.getTimeZone(response.timezone);
        } else {
            tz = TimeZone.getDefault();
        }

        // Parse sunrise / sunset strings (we use the first day for now)
        String sunriseStr = daily.sunrise.get(0);
        String sunsetStr = daily.sunset.get(0);

        // OpenMeteo "local time" is usually like "2025-12-06T07:14"
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        dateTimeFormat.setTimeZone(tz);

        Date sunriseDate;
        Date sunsetDate;
        try {
            sunriseDate = dateTimeFormat.parse(sunriseStr);
            sunsetDate = dateTimeFormat.parse(sunsetStr);
        } catch (ParseException e) {
            Log.w(TAG, "deriveWindowWeather: failed to parse sunrise/sunset, using current", e);
            return response.current;
        }

        // Build Calendar instances for sunrise & sunset
        Calendar sunriseCal = Calendar.getInstance(tz);
        sunriseCal.setTime(sunriseDate);

        Calendar sunsetCal = Calendar.getInstance(tz);
        sunsetCal.setTime(sunsetDate);

        // Define hunt window boundaries: 2h before sunrise / 2h after sunset, etc.
        Calendar windowStartCal = Calendar.getInstance(tz);
        Calendar windowEndCal = Calendar.getInstance(tz);

        switch (window) {
            case MORNING_MID:
                // 2 hours before sunrise through ~4 hours after sunrise
                windowStartCal.setTime(sunriseDate);
                windowStartCal.add(Calendar.HOUR_OF_DAY, -2);

                windowEndCal.setTime(sunriseDate);
                windowEndCal.add(Calendar.HOUR_OF_DAY, 4);
                break;

            case MID_EVENING:
                // Late morning (4h after sunrise) through 2 hours after sunset
                windowStartCal.setTime(sunriseDate);
                windowStartCal.add(Calendar.HOUR_OF_DAY, 4);

                windowEndCal.setTime(sunsetDate);
                windowEndCal.add(Calendar.HOUR_OF_DAY, 2);
                break;

            case ALL_DAY:
            default:
                // From 2 hours before sunrise through 2 hours after sunset
                windowStartCal.setTime(sunriseDate);
                windowStartCal.add(Calendar.HOUR_OF_DAY, -2);

                windowEndCal.setTime(sunsetDate);
                windowEndCal.add(Calendar.HOUR_OF_DAY, 2);
                break;
        }

        long windowStartMs = windowStartCal.getTimeInMillis();
        long windowEndMs = windowEndCal.getTimeInMillis();

        Log.d(TAG, "deriveWindowWeather: window=" + window
                + " start=" + dateTimeFormat.format(windowStartCal.getTime())
                + " end=" + dateTimeFormat.format(windowEndCal.getTime()));

        // Shortcuts to hourly arrays
        List<String> times = hourly.time;
        List<Double> temps = hourly.temperature2m;
        List<Double> apparentTemps = (hourly.apparentTemperature != null && !hourly.apparentTemperature.isEmpty())
                ? hourly.apparentTemperature
                : hourly.temperature2m;
        List<Double> winds = hourly.windSpeed10m;
        List<Double> precips = hourly.precipitation;
        List<Double> snows = hourly.snowfall;

        double sumTemp = 0.0;
        double sumApparent = 0.0;
        double maxWind = 0.0;
        double totalPrecip = 0.0;
        double totalSnow = 0.0;
        int count = 0;

        for (int i = 0; i < times.size(); i++) {
            String timeStr = times.get(i);
            if (timeStr == null) continue;

            Date hourDate;
            try {
                hourDate = dateTimeFormat.parse(timeStr);
            } catch (ParseException e) {
                Log.w(TAG, "deriveWindowWeather: failed to parse hourly time '" + timeStr + "'", e);
                continue;
            }

            long hourMs = hourDate.getTime();
            if (hourMs < windowStartMs || hourMs >= windowEndMs) {
                continue;
            }

            double t = safeGet(temps, i, response.current.temperature2m);
            double a = safeGet(apparentTemps, i, response.current.apparentTemperature);
            double w = safeGet(winds, i, response.current.windSpeed10m);
            double p = safeGet(precips, i, 0.0);
            double s = safeGet(snows, i, 0.0);

            sumTemp += t;
            sumApparent += a;
            totalPrecip += p;
            totalSnow += s;
            if (w > maxWind) maxWind = w;

            count++;
        }

        if (count == 0) {
            Log.w(TAG, "deriveWindowWeather: no hourly points matched window=" + window
                    + ", falling back to current");
            return response.current;
        }

        double avgTemp = sumTemp / count;
        double avgApparent = sumApparent / count;

        CurrentWeather base = response.current;
        CurrentWeather windowWeather = new CurrentWeather();

        // Temps
        windowWeather.temperature2m = avgTemp;
        windowWeather.apparentTemperature = avgApparent;

        // Wind: worst-case gustiness in the hunting window
        windowWeather.windSpeed10m = maxWind;
        windowWeather.windDirection10m = base.windDirection10m; // reuse direction for now

        // Precip/snow totals across the window
        windowWeather.precipitation = totalPrecip;
        windowWeather.snowfall = totalSnow;

        // Copy less-volatile fields from current
        windowWeather.pressureMsl = base.pressureMsl;
        windowWeather.relativeHumidity = base.relativeHumidity;
        windowWeather.cloudCover = base.cloudCover;

        Log.d(TAG, "deriveWindowWeather: window=" + window
                + " avgApparent=" + avgApparent
                + "F, avgTemp=" + avgTemp
                + "F, maxWind=" + maxWind
                + " mph, totalPrecip=" + totalPrecip
                + " in, totalSnow=" + totalSnow + " in");

        return windowWeather;
    }

    private double safeGet(List<Double> list, int index, double fallback) {
        if (list == null || index < 0 || index >= list.size()) return fallback;
        Double v = list.get(index);
        return v != null ? v : fallback;
    }

    // Alternate approach:
    // - Expose closet LiveData as well (getClosetGear()) if HomeFragment
    //   ever needs to show “all gear vs recommended gear” side by side.
}
