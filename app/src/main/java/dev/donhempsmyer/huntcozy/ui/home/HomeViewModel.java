package dev.donhempsmyer.huntcozy.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dev.donhempsmyer.huntcozy.algo.GearRecommender;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntWindow;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.model.weather.CurrentWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.DailyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.HourlyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;
import dev.donhempsmyer.huntcozy.data.repository.OpenMeteoWeatherRepository;
import dev.donhempsmyer.huntcozy.data.repository.WeatherRepository;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    // Weather + closet + locations
    // Weather: OpenMeteo-backed singleton
    private final WeatherRepository weatherRepository =
            OpenMeteoWeatherRepository.getInstance();

    // Closet: Firestore-backed, provided via ClosetRepositoryProvider.init(...)
    private final ClosetRepository closetRepository =
            ClosetRepositoryProvider.get();

    // Locations: current implementation (can later be swapped to Firestore-backed)
    private final LocationsRepository locationsRepository =
            LocationsRepository.getInstance();

    private final GearRecommender gearRecommender = new GearRecommender();

    // User selections
    private final MutableLiveData<WeaponType> weaponTypeLiveData =
            new MutableLiveData<>(WeaponType.RIFLE);

    private final MutableLiveData<HuntingStyle> huntingStyleLiveData =
            new MutableLiveData<>(HuntingStyle.TREESTAND);

    private final MutableLiveData<HuntWindow> huntWindowLiveData =
            new MutableLiveData<>(HuntWindow.ALL_DAY);

    // Recommended gear (output)
    private final MutableLiveData<List<GearItem>> gearLiveData =
            new MutableLiveData<>();

    private final androidx.lifecycle.Observer<List<GearItem>> closetObserver = closet -> {
        Log.d(TAG, "closetObserver: closet updated, count=" + (closet != null ? closet.size() : 0));
        recomputeGear();
    };

    public HomeViewModel() {
        Log.d(TAG, "constructor: HomeViewModel created");
        // NOTE:
        // - LocationsRepository is responsible for seeding default locations
        //   and setting an initial selectedLocation.
        // - When the user changes locations, HomeFragment calls onLocationSelected().

        // Observe closet changes so we recompute if the user adds/removes gear
        closetRepository.getClosetLiveData().observeForever(closetObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        closetRepository.getClosetLiveData().removeObserver(closetObserver);
    }

    // --- Locations API (used by HomeFragment) --------------------------------

    public LiveData<List<HuntLocation>> getLocations() {
        return locationsRepository.getLocations();
    }

    public LiveData<HuntLocation> getSelectedLocation() {
        return locationsRepository.getSelectedLocation();
    }

    /**
     * Called by HomeFragment when the user picks a new location from the spinner.
     * Also used indirectly if another screen (LocationsFragment) changes selection.
     */
    public void onLocationSelected(HuntLocation location) {
        if (location == null) return;
        Log.d(TAG, "onLocationSelected: " + location);
        locationsRepository.selectLocation(location);

        // Refresh weather for this location
        weatherRepository.refreshWeatherForLocation(
                location.getLatitude(),
                location.getLongitude()
        );

        // When weather finishes updating, HomeFragment's observer will call onWeatherUpdated().
    }

    // --- Weather & Hunt window API -------------------------------------------

    public LiveData<WeatherResponse> getWeather() {
        return weatherRepository.getWeatherLiveData();
    }

    public LiveData<HuntWindow> getHuntWindow() {
        return huntWindowLiveData;
    }

    public LiveData<List<GearItem>> getGear() {
        return gearLiveData;
    }

    public LiveData<WeaponType> getWeaponType() {
        return weaponTypeLiveData;
    }

    public LiveData<HuntingStyle> getHuntingStyle() {
        return huntingStyleLiveData;
    }

    /**
     * Optional: expose closet LiveData if the UI ever needs it directly.
     */
    public LiveData<List<GearItem>> getClosetGear() {
        return closetRepository.getClosetLiveData();
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

    /**
     * Called from HomeFragment when weather LiveData emits a new value.
     */
    public void onWeatherUpdated(WeatherResponse response) {
        Log.d(TAG, "onWeatherUpdated");
        recomputeGear();
    }

    // --- Core recompute logic: Weather + Closet + Selections → Outfit --------

    private void recomputeGear() {
        WeatherResponse response = weatherRepository.getWeatherLiveData().getValue();
        WeaponType weaponType = weaponTypeLiveData.getValue();
        HuntingStyle huntingStyle = huntingStyleLiveData.getValue();
        HuntWindow huntWindow = huntWindowLiveData.getValue();
        HuntLocation loc = locationsRepository.getSelectedLocation().getValue();

        Log.d(TAG, "recomputeGear: checking dependencies...");
        if (response == null) Log.d(TAG, "recomputeGear: weather response is null");
        if (weaponType == null) Log.d(TAG, "recomputeGear: weaponType is null");
        if (huntingStyle == null) Log.d(TAG, "recomputeGear: huntingStyle is null");
        if (huntWindow == null) Log.d(TAG, "recomputeGear: huntWindow is null");

        if (response == null || response.current == null ||
                weaponType == null || huntingStyle == null || huntWindow == null) {
            Log.d(TAG, "recomputeGear: missing weather, selections, or huntWindow; skipping");
            return;
        }

        // NOTE: ClosetRepository now exposes getClosetLiveData() instead of getAllGear().
        List<GearItem> closet = closetRepository.getClosetLiveData().getValue();
        if (closet == null || closet.isEmpty()) {
            Log.w(TAG, "recomputeGear: closet is " + (closet == null ? "null" : "empty") + ", no outfit can be built");
            gearLiveData.setValue(new ArrayList<>()); // Use empty list instead of null to clear UI
            return;
        }

        // Use aggregated "hunt window" weather rather than just current conditions
        CurrentWeather windowWeather = deriveWindowWeather(response, huntWindow);
        if (windowWeather == null) {
            Log.e(TAG, "recomputeGear: windowWeather derived as null, using current as fallback");
            windowWeather = response.current;
        }

        List<GearItem> outfit = gearRecommender.buildOutfitFromCloset(
                closet,
                windowWeather,
                weaponType,
                huntingStyle
        );

        Log.d(TAG, "recomputeGear: success! window=" + huntWindow
                + " closetSize=" + closet.size()
                + " outfitSize=" + (outfit != null ? outfit.size() : 0));

        Log.d(TAG, "recomputeGear: location="
                + (loc != null ? loc.getName() : "null")
                + " lat=" + (loc != null ? loc.getLatitude() : null)
                + " lon=" + (loc != null ? loc.getLongitude() : null));

        gearLiveData.setValue(outfit);
    }

    /**
     * Derive representative "hunt window" weather using high/low extremes:
     *  - lowest & highest apparent + actual temp in the window
     *  - lowest & highest wind speed in the window
     *  - total precip/snow over the window
     *  - barometric pressure from hourly surface_pressure
     *
     * For gear scoring we use:
     *  - lowest apparent temp (coldest point),
     *  - highest wind speed (worst wind),
     *  - total precip & snow (overall wetness),
     *  - average barometric pressure (for display / trend).
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

        // TimeZone from API, fallback to device default
        TimeZone tz;
        if (response.timezone != null && !response.timezone.isEmpty()) {
            tz = TimeZone.getTimeZone(response.timezone);
        } else {
            tz = TimeZone.getDefault();
        }

        // Use first day's sunrise/sunset for now
        String sunriseStr = daily.sunrise.get(0);
        String sunsetStr = daily.sunset.get(0);

        SimpleDateFormat dateTimeFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
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

        // Build Calendar instances
        Calendar sunriseCal = Calendar.getInstance(tz);
        sunriseCal.setTime(sunriseDate);

        Calendar sunsetCal = Calendar.getInstance(tz);
        sunsetCal.setTime(sunsetDate);

        // Define hunt window boundaries using +/- 2h around sunrise/sunset
        Calendar windowStartCal = Calendar.getInstance(tz);
        Calendar windowEndCal = Calendar.getInstance(tz);

        switch (window) {
            case MORNING_MID:
                // 2h before sunrise through ~4h after sunrise
                windowStartCal.setTime(sunriseDate);
                windowStartCal.add(Calendar.HOUR_OF_DAY, -2);

                windowEndCal.setTime(sunriseDate);
                windowEndCal.add(Calendar.HOUR_OF_DAY, 4);
                break;

            case MID_EVENING:
                // Late morning (4h after sunrise) through 2h after sunset
                windowStartCal.setTime(sunriseDate);
                windowStartCal.add(Calendar.HOUR_OF_DAY, 4);

                windowEndCal.setTime(sunsetDate);
                windowEndCal.add(Calendar.HOUR_OF_DAY, 2);
                break;

            case ALL_DAY:
            default:
                // 2h before sunrise through 2h after sunset
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
        List<Double> apparentTemps = (hourly.apparentTemperature != null &&
                !hourly.apparentTemperature.isEmpty())
                ? hourly.apparentTemperature
                : hourly.temperature2m;
        List<Double> winds = hourly.windSpeed10m;
        List<Double> precips = hourly.precipitation;
        List<Double> snows = hourly.snowfall;
        List<Double> baros = hourly.barometricPressure; // surface_pressure

        // High/low trackers
        double minTemp = Double.POSITIVE_INFINITY;
        double maxTemp = Double.NEGATIVE_INFINITY;

        double minApparent = Double.POSITIVE_INFINITY;
        double maxApparent = Double.NEGATIVE_INFINITY;

        double minWind = Double.POSITIVE_INFINITY;
        double maxWind = Double.NEGATIVE_INFINITY;

        double minPrecip = Double.POSITIVE_INFINITY;
        double maxPrecip = Double.NEGATIVE_INFINITY;

        double minSnow = Double.POSITIVE_INFINITY;
        double maxSnow = Double.NEGATIVE_INFINITY;

        double minBaro = Double.POSITIVE_INFINITY;
        double maxBaro = Double.NEGATIVE_INFINITY;
        double sumBaro = 0.0;
        int baroCount = 0;

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
            double b = (baros != null && !baros.isEmpty())
                    ? safeGet(baros, i, response.current.barometricPressure)
                    : response.current.barometricPressure;

            // Update min/max
            if (t < minTemp) minTemp = t;
            if (t > maxTemp) maxTemp = t;

            if (a < minApparent) minApparent = a;
            if (a > maxApparent) maxApparent = a;

            if (w < minWind) minWind = w;
            if (w > maxWind) maxWind = w;

            if (p < minPrecip) minPrecip = p;
            if (p > maxPrecip) maxPrecip = p;

            if (s < minSnow) minSnow = s;
            if (s > maxSnow) maxSnow = s;

            if (!Double.isNaN(b)) {
                if (b < minBaro) minBaro = b;
                if (b > maxBaro) maxBaro = b;
                sumBaro += b;
                baroCount++;
            }

            totalPrecip += p;
            totalSnow += s;

            count++;
        }

        if (count == 0 || Double.isInfinite(minApparent)) {
            Log.w(TAG, "deriveWindowWeather: no hourly points matched window=" + window
                    + ", falling back to current");
            return response.current;
        }

        double avgBaro;
        if (baroCount > 0) {
            avgBaro = sumBaro / baroCount;
        } else {
            avgBaro = response.current.barometricPressure;
            minBaro = avgBaro;
            maxBaro = avgBaro;
        }

        // Use "worst-case" for main gear inputs
        double chosenTemp = minTemp;
        double chosenApparent = minApparent;
        double chosenWind = maxWind;

        CurrentWeather base = response.current;
        CurrentWeather windowWeather = new CurrentWeather();

        windowWeather.temperature2m = chosenTemp;
        windowWeather.apparentTemperature = chosenApparent;

        windowWeather.windSpeed10m = chosenWind;
        windowWeather.windDirection10m = base.windDirection10m;

        windowWeather.precipitation = totalPrecip;
        windowWeather.snowfall = totalSnow;

        // Barometric pressure (surface_pressure) aggregated over window
        windowWeather.barometricPressure = avgBaro;

        // Copy less-volatile fields from current
        windowWeather.relativeHumidity = base.relativeHumidity;
        windowWeather.cloudCover = base.cloudCover;

        Log.d(TAG, "deriveWindowWeather: window=" + window
                + " minApparent=" + minApparent
                + "F, maxApparent=" + maxApparent
                + "F, minTemp=" + minTemp
                + "F, maxTemp=" + maxTemp
                + "F, minWind=" + minWind
                + " mph, maxWind=" + maxWind
                + " mph, totalPrecip=" + totalPrecip
                + " in (maxHourly=" + maxPrecip
                + "), totalSnow=" + totalSnow
                + " in (maxHourly=" + maxSnow
                + "), baroAvg=" + avgBaro
                + " (min=" + minBaro
                + ", max=" + maxBaro + ")");

        return windowWeather;
    }

    private double safeGet(List<Double> list, int index, double fallback) {
        if (list == null || index < 0 || index >= list.size()) return fallback;
        Double v = list.get(index);
        return v != null ? v : fallback;
    }
}
