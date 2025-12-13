package dev.donhempsmyer.huntcozy.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.HuntWindow;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.model.weather.DailyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.HourlyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.ui.main.MainActivity;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListViewModel;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private HomeViewModel viewModel;

    // Header / selectors
    private TextView textLocationName;
    private TextView textCurrentTemp;
    private TextView textCurrentWind;
    private TextView textCurrentPrecip;

    private Spinner spinnerLocation;
    private Spinner spinnerWeapon;
    private Spinner spinnerHuntingStyle;

    private MaterialButtonToggleGroup groupHuntWindow;
    private MaterialButton btnMorningMid;
    private MaterialButton btnMidEvening;
    private MaterialButton btnAllDay;
    private View cardCurrentWeather;

    // Forecast + gear
    private RecyclerView recyclerForecast;
    private RecyclerView recyclerGear;
    private TextView textForecastTitle;
    private Button buttonBackToDaily;

    private ForecastAdapter forecastAdapter;
    private GearAdapter gearAdapter;

    // Location spinner backing data
    private ArrayAdapter<String> locationAdapter;
    private final List<HuntLocation> locationItems = new ArrayList<>();

    // Keep last weather response for toggling daily/hourly
    private WeatherResponse lastWeather;
    private int selectedDailyIndex = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        bindViews(view);
        setupLocationSpinner();
        setupWeaponSpinner();
        setupHuntingStyleSpinner();
        setupForecastRecycler();
        setupGearRecycler();
        setupHuntWindowToggle();
        observeViewModel();
    }

    private void bindViews(View root) {
        textLocationName = root.findViewById(R.id.text_location_name);
        textCurrentTemp = root.findViewById(R.id.text_current_temp);
        textCurrentWind = root.findViewById(R.id.text_current_wind);
        textCurrentPrecip = root.findViewById(R.id.text_current_precip);

        cardCurrentWeather = root.findViewById(R.id.card_current_weather);


        spinnerLocation = root.findViewById(R.id.spinner_location);
        spinnerWeapon = root.findViewById(R.id.spinner_weapon);
        spinnerHuntingStyle = root.findViewById(R.id.spinner_hunting_style);

        recyclerForecast = root.findViewById(R.id.recycler_forecast);
        recyclerGear = root.findViewById(R.id.recycler_gear);
        textForecastTitle = root.findViewById(R.id.text_forecast_title);
        buttonBackToDaily = root.findViewById(R.id.button_back_to_daily);

        groupHuntWindow = root.findViewById(R.id.group_hunt_window);
        btnMorningMid = root.findViewById(R.id.btn_window_morning_mid);
        btnMidEvening = root.findViewById(R.id.btn_window_mid_evening);
        btnAllDay = root.findViewById(R.id.btn_window_all_day);

        buttonBackToDaily.setOnClickListener(v -> showDailyForecast());

        if (cardCurrentWeather != null) {
            cardCurrentWeather.setOnClickListener(v -> openConditionsFromHome());
        }
    }

    private void openConditionsFromHome() {
        Log.d(TAG, "openConditionsFromHome: current weather card clicked");

        if (requireActivity() instanceof MainActivity) {
            // Use MainActivity so bottom nav highlights "Conditions"
            MainActivity activity = (MainActivity) requireActivity();
            activity.openConditionsTab();
        } else {
            // Fallback: direct fragment transaction if host isn't MainActivity
            Log.w(TAG, "openConditionsFromHome: host is not MainActivity, using direct transaction fallback");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container,
                            dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Location spinner is driven by:
     *  - locations list (names in dropdown)
     *  - selectedLocation (for header + keeping spinner selection in sync)
     */
    private void setupLocationSpinner() {
        // Adapter holds only the location names
        locationAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        // User selection → ViewModel
        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                if (position < 0 || position >= locationItems.size()) return;
                HuntLocation selected = locationItems.get(position);
                viewModel.onLocationSelected(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        // Locations list → update items + names
        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            int count = (locations != null) ? locations.size() : 0;
            Log.d(TAG, "setupLocationSpinner: locations size=" + count);

            locationItems.clear();
            locationAdapter.clear();

            if (locations != null) {
                locationItems.addAll(locations);
                for (HuntLocation loc : locations) {
                    locationAdapter.add(loc.getName());
                }
            }

            locationAdapter.notifyDataSetChanged();
            syncSpinnerWithSelectedLocation();
        });

        // Selected location → header text + keep spinner aligned
        viewModel.getSelectedLocation().observe(getViewLifecycleOwner(), selected -> {
            if (selected == null) {
                textLocationName.setText("Selected Location");
            } else {
                textLocationName.setText("Location: " + selected.getName());
            }
            syncSpinnerWithSelectedLocation();
        });
    }

    /**
     * Ensures spinner selection matches ViewModel's selectedLocation,
     * even when selection is changed outside of this Fragment (e.g. LocationsFragment).
     */
    private void syncSpinnerWithSelectedLocation() {
        HuntLocation selected = viewModel.getSelectedLocation().getValue();
        if (selected == null || locationItems.isEmpty()) return;

        int index = -1;
        for (int i = 0; i < locationItems.size(); i++) {
            if (locationItems.get(i).getId() == selected.getId()) {
                index = i;
                break;
            }
        }

        if (index >= 0 && spinnerLocation.getSelectedItemPosition() != index) {
            Log.d(TAG, "syncSpinnerWithSelectedLocation: setting position=" + index);
            spinnerLocation.setSelection(index);
        }
    }

    private void setupWeaponSpinner() {
        WeaponType[] types = WeaponType.values();
        ArrayAdapter<WeaponType> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeapon.setAdapter(adapter);

        spinnerWeapon.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                WeaponType selected = types[position];
                WeaponType current = viewModel.getWeaponType().getValue();
                if (current != selected) {
                    viewModel.setWeaponType(selected);
                }
            }
        });
        // ViewModel → Spinner (restore selection when returning)
        viewModel.getWeaponType().observe(getViewLifecycleOwner(), type -> {
            if (type == null) return;

            int index = -1;
            for (int i = 0; i < types.length; i++) {
                if (types[i] == type) {
                    index = i;
                    break;
                }
            }

            if (index >= 0 && spinnerWeapon.getSelectedItemPosition() != index) {
                spinnerWeapon.setSelection(index);
            }
        });
    }

    private void setupHuntingStyleSpinner() {
        HuntingStyle[] styles = HuntingStyle.values();
        ArrayAdapter<HuntingStyle> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                styles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHuntingStyle.setAdapter(adapter);

        spinnerHuntingStyle.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                HuntingStyle selected = styles[position];
                HuntingStyle current = viewModel.getHuntingStyle().getValue();
                if (current != selected) {
                    viewModel.setHuntingStyle(selected);
                }
            }
        });

        // ViewModel → Spinner
        viewModel.getHuntingStyle().observe(getViewLifecycleOwner(), style -> {
            if (style == null) return;

            int index = -1;
            for (int i = 0; i < styles.length; i++) {
                if (styles[i] == style) {
                    index = i;
                    break;
                }
            }

            if (index >= 0 && spinnerHuntingStyle.getSelectedItemPosition() != index) {
                spinnerHuntingStyle.setSelection(index);
            }
        });
    }

    private void setupForecastRecycler() {
        forecastAdapter = new ForecastAdapter();
        forecastAdapter.setOnDayClickListener(dayIndex -> {
            Log.d(TAG, "onDayClicked: index=" + dayIndex);
            selectedDailyIndex = dayIndex;
            showHourlyForecastForDay(dayIndex);
        });

        recyclerForecast.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        recyclerForecast.setAdapter(forecastAdapter);
    }

    private void setupGearRecycler() {
        gearAdapter = new GearAdapter();
        gearAdapter.setOnGearClickListener(this::navigateToPackingList);

        recyclerGear.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerGear.setAdapter(gearAdapter);
    }

    private void setupHuntWindowToggle() {
        // 1) Listen for user changes: Toggle -> ViewModel
        groupHuntWindow.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            HuntWindow window;
            if (checkedId == R.id.btn_window_morning_mid) {
                window = HuntWindow.MORNING_MID;
            } else if (checkedId == R.id.btn_window_mid_evening) {
                window = HuntWindow.MID_EVENING;
            } else {
                window = HuntWindow.ALL_DAY;
            }

            // Only update if it actually changed
            HuntWindow current = viewModel.getHuntWindow().getValue();
            if (current != window) {
                viewModel.setHuntWindow(window);
            }
        });

        // 2) Initialize from ViewModel (or default once)
        HuntWindow current = viewModel.getHuntWindow().getValue();
        if (current == null) {
            current = HuntWindow.ALL_DAY;          // first-time default
            viewModel.setHuntWindow(current);
        }

        int initialId;
        switch (current) {
            case MORNING_MID:
                initialId = R.id.btn_window_morning_mid;
                break;
            case MID_EVENING:
                initialId = R.id.btn_window_mid_evening;
                break;
            case ALL_DAY:
            default:
                initialId = R.id.btn_window_all_day;
                break;
        }
        if (groupHuntWindow.getCheckedButtonId() != initialId) {
            groupHuntWindow.check(initialId);
        }

        // 3) ViewModel -> Toggle (keeps things in sync if changed elsewhere)
        viewModel.getHuntWindow().observe(getViewLifecycleOwner(), window -> {
            if (window == null) return;

            int id;
            switch (window) {
                case MORNING_MID:
                    id = R.id.btn_window_morning_mid;
                    break;
                case MID_EVENING:
                    id = R.id.btn_window_mid_evening;
                    break;
                case ALL_DAY:
                default:
                    id = R.id.btn_window_all_day;
                    break;
            }

            if (groupHuntWindow.getCheckedButtonId() != id) {
                groupHuntWindow.check(id);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getWeather().observe(getViewLifecycleOwner(), weather -> {
            Log.d(TAG, "observeViewModel: weather updated");
            lastWeather = weather;
            updateCurrentWeatherUI(weather);
            populateDailyForecast(weather);
            viewModel.onWeatherUpdated(weather);
        });

        viewModel.getGear().observe(getViewLifecycleOwner(), gearItems -> {
            Log.d(TAG, "observeViewModel: gear updated count=" +
                    (gearItems != null ? gearItems.size() : 0));
            gearAdapter.setItems(gearItems);
        });
    }

    private void updateCurrentWeatherUI(WeatherResponse response) {
        if (response == null || response.current == null) return;

        HuntLocation selected = viewModel.getSelectedLocation().getValue();
        if (selected != null) {
            textLocationName.setText("Location: " + selected.getName());
        } else {
            textLocationName.setText("Selected Location");
        }

        String tempText = String.format("%.0f°F (Feels %.0f°F)",
                response.current.temperature2m,
                response.current.apparentTemperature);
        textCurrentTemp.setText(tempText);

        double windSpeed = response.current.windSpeed10m;
        double windDirDeg = response.current.windDirection10m;
        String windDirText = bearingToCompass(windDirDeg);

        String windText = String.format(
                "Wind %.0f mph %s",
                windSpeed,
                windDirText,
                windDirDeg
        );
        textCurrentWind.setText(windText);

        double baroHpa = response.current.barometricPressure;
        double baroInHg = baroHpa * 0.02953;

        String precipText = String.format(
                "Precip %.2f in · Snow %.2f in · Pressure %.0f inHg",
                response.current.precipitation,
                response.current.snowfall,
                baroInHg
        );
        textCurrentPrecip.setText(precipText);
    }

    // HomeFragment.java
    private void populateDailyForecast(WeatherResponse response) {
        if (response == null || response.daily == null) return;

        // Reset UI to "daily" mode
        textForecastTitle.setText("7-Day Forecast");
        buttonBackToDaily.setVisibility(View.GONE);
        forecastAdapter.setMode(ForecastAdapter.Mode.DAILY);

        DailyWeather d = response.daily;

        List<String> labels = new ArrayList<>();
        List<String> primaryValues = new ArrayList<>();   // temp range
        List<String> secondaryValues = new ArrayList<>(); // precip/snow summary
        List<String> markers = new ArrayList<>();         // (unused for now)

        java.text.SimpleDateFormat inputFormat =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        java.text.SimpleDateFormat outputFormat =
                new java.text.SimpleDateFormat("EEE MM/dd", java.util.Locale.US); // "Mon 12/09"

        int days = (d.time != null) ? d.time.size() : 0;
        for (int i = 0; i < days; i++) {
            String isoDate = d.time.get(i); // "2025-12-02"

            // Pretty label
            String dayLabel = isoDate;
            try {
                java.util.Date parsed = inputFormat.parse(isoDate);
                if (parsed != null) {
                    dayLabel = outputFormat.format(parsed);
                }
            } catch (Exception ignored) {
            }
            labels.add(dayLabel);

            // Hi/Lo temps (defensive checks)
            double tMin = (d.temperatureMin != null && d.temperatureMin.size() > i)
                    ? d.temperatureMin.get(i)
                    : 0.0;
            double tMax = (d.temperatureMax != null && d.temperatureMax.size() > i)
                    ? d.temperatureMax.get(i)
                    : 0.0;
            String range = String.format(java.util.Locale.US, "%.0f° / %.0f°", tMin, tMax);
            primaryValues.add(range);

            // Precip & snow in inches (your OpenMeteo call already requests inch units)
            double snow = (d.snowfallSum != null && d.snowfallSum.size() > i)
                    ? d.snowfallSum.get(i)
                    : 0.0;
            double precip = (d.precipitationSum != null && d.precipitationSum.size() > i)
                    ? d.precipitationSum.get(i)
                    : 0.0;

            String precipSummary = String.format(
                    java.util.Locale.US,
                    "Precip %.2f in · Snow %.2f in",
                    precip,
                    snow
            );
            secondaryValues.add(precipSummary);

            // No special marker yet (could use "Best" day later)
            markers.add("");
        }

        // New 4-arg adapter API
        forecastAdapter.setData(labels, primaryValues, secondaryValues, markers);
    }

    private void showHourlyForecastForDay(int dayIndex) {
        if (lastWeather == null || lastWeather.hourly == null || lastWeather.daily == null) {
            Log.w(TAG, "showHourlyForecastForDay: missing data");
            return;
        }

        DailyWeather d = lastWeather.daily;
        HourlyWeather h = lastWeather.hourly;

        if (d.time == null || dayIndex < 0 || dayIndex >= d.time.size()) {
            Log.w(TAG, "showHourlyForecastForDay: invalid dayIndex=" + dayIndex);
            return;
        }

        textForecastTitle.setText("Hourly (selected day)");
        buttonBackToDaily.setVisibility(View.VISIBLE);
        forecastAdapter.setMode(ForecastAdapter.Mode.HOURLY);

        // e.g. "2025-12-02"
        String dayDate = d.time.get(dayIndex);

        List<String> labels = new ArrayList<>();
        List<String> primaryValues = new ArrayList<>();
        List<String> secondaryValues = new ArrayList<>();
        List<String> markers = new ArrayList<>();

        // Raw sunrise/sunset strings from OpenMeteo, e.g. "2025-12-02T07:03"
        String sunriseStr = (d.sunrise != null && d.sunrise.size() > dayIndex)
                ? d.sunrise.get(dayIndex)
                : null;
        String sunsetStr = (d.sunset != null && d.sunset.size() > dayIndex)
                ? d.sunset.get(dayIndex)
                : null;

        // We'll match by the hour prefix: "yyyy-MM-ddTHH"
        String sunrisePrefix = null;
        String sunsetPrefix = null;
        String sunriseTime = null; // "HH:mm"
        String sunsetTime = null;  // "HH:mm"

        if (sunriseStr != null && sunriseStr.startsWith(dayDate) && sunriseStr.length() >= 16) {
            sunrisePrefix = sunriseStr.substring(0, 13); // "2025-12-02T07"
            sunriseTime = sunriseStr.substring(11, 16);  // "07:03"
        }

        if (sunsetStr != null && sunsetStr.startsWith(dayDate) && sunsetStr.length() >= 16) {
            sunsetPrefix = sunsetStr.substring(0, 13);   // "2025-12-02T16"
            sunsetTime = sunsetStr.substring(11, 16);    // "16:41"
        }

        int hours = (h.time != null) ? h.time.size() : 0;
        for (int i = 0; i < hours; i++) {
            String iso = h.time.get(i); // e.g. "2025-12-02T07:00"
            if (iso == null || !iso.startsWith(dayDate)) continue;

            // Label: card shows the hour bucket "HH:MM" (e.g., "07:00")
            String hourLabel = (iso.length() >= 16) ? iso.substring(11, 16) : iso;
            labels.add(hourLabel);

            // Primary: temp + feels-like
            double temp = safeGet(
                    h.temperature2m,
                    i,
                    (lastWeather.current != null) ? lastWeather.current.temperature2m : 0.0
            );
            double apparent = safeGet(
                    h.apparentTemperature,
                    i,
                    (lastWeather.current != null)
                            ? lastWeather.current.apparentTemperature
                            : temp
            );

            String primary = String.format(
                    java.util.Locale.US,
                    "%.0f° (Feels %.0f°)",
                    temp,
                    apparent
            );
            primaryValues.add(primary);

            // Secondary: wind dir + speed + precip
            double windSpeed = safeGet(
                    h.windSpeed10m,
                    i,
                    (lastWeather.current != null) ? lastWeather.current.windSpeed10m : 0.0
            );
            double windDir = safeGet(
                    h.windDirection10m,
                    i,
                    (lastWeather.current != null) ? lastWeather.current.windDirection10m : 0.0
            );
            double precip = safeGet(h.precipitation, i, 0.0);

            String cardinal = bearingToCompass(windDir);
            String secondary = String.format(
                    java.util.Locale.US,
                    "%s %.0f mph · Precip %.2f in",
                    cardinal,
                    windSpeed,
                    precip
            );
            secondaryValues.add(secondary);

            // Marker: put Sunrise/Sunset on the *exact hour bucket*.
            // Example:
            //  - sunriseStr: "2025-12-02T07:03"
            //  - iso:        "2025-12-02T07:00"
            // Both share prefix "2025-12-02T07", so we label this card.
            String marker = "";

            if (sunrisePrefix != null && iso.startsWith(sunrisePrefix) && sunriseTime != null) {
                marker = "Sunrise: " + sunriseTime;
            }

            if (sunsetPrefix != null && iso.startsWith(sunsetPrefix) && sunsetTime != null) {
                if (!marker.isEmpty()) {
                    marker += " · ";
                }
                marker += "Sunset: " + sunsetTime;
            }

            markers.add(marker);
        }

        forecastAdapter.setData(labels, primaryValues, secondaryValues, markers);
    }

    private void showDailyForecast() {
        Log.d(TAG, "showDailyForecast");
        if (lastWeather != null) {
            populateDailyForecast(lastWeather);
        }
    }

    private void navigateToPackingList(GearItem item) {
        Log.d(TAG, "navigateToPackingList: clicked gear " +
                (item != null ? item.getName() : "null"));

        // 1) Share context with PackingListViewModel (weapon + style)
        PackingListViewModel packingVM =
                new ViewModelProvider(requireActivity()).get(PackingListViewModel.class);

        // Safely grab current weapon/style from HomeViewModel
        WeaponType weapon = viewModel.getWeaponType().getValue();
        HuntingStyle style = viewModel.getHuntingStyle().getValue();
        packingVM.setContextFromHome(weapon, style);

        // 2) Ask MainActivity to switch to the Packing tab so bottom nav stays in sync
        if (requireActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) requireActivity();
            activity.openPackingTab();   // internally calls bottomNav.setSelectedItemId(R.id.nav_packing_list)
        } else {
            // Fallback: direct fragment transaction if host isn't MainActivity
            Log.w(TAG, "navigateToPackingList: host is not MainActivity, using direct transaction fallback");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, PackingListFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
        }
    }



    /**
     * Simple helper class to avoid boilerplate in spinner listeners
     * for WeaponType / HuntingStyle only.
     */
    private abstract static class SimpleItemSelectedListener
            implements android.widget.AdapterView.OnItemSelectedListener {

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) { }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent,
                                   View view,
                                   int position,
                                   long id) {
            onItemSelected(position);
        }

        public abstract void onItemSelected(int position);
    }

    private double safeGet(List<Double> list, int index, double fallback) {
        if (list == null || index < 0 || index >= list.size()) return fallback;
        Double v = list.get(index);
        return (v != null) ? v : fallback;
    }

    private String bearingToCompass(double bearing) {
        String[] dirs = {
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };
        bearing = (bearing % 360 + 360) % 360; // normalize 0–359
        int index = (int) Math.round(bearing / 22.5) % 16;
        return dirs[index];
    }

    // Alternate approach:
    // - Extract the UI wiring into a HomeUiController to keep Fragment thinner.
    // - Use View Binding / Data Binding to avoid findViewById.
}
