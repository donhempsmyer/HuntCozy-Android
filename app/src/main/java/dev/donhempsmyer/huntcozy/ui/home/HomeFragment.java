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
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;

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

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

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
                viewModel.setWeaponType(types[position]);
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
                viewModel.setHuntingStyle(styles[position]);
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
        // Default to All-Day
        groupHuntWindow.check(R.id.btn_window_all_day);
        viewModel.setHuntWindow(HuntWindow.ALL_DAY);

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

            viewModel.setHuntWindow(window);
        });

        // Keep toggle in sync with ViewModel (e.g. if HuntWindow is changed elsewhere)
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

        String windText = String.format("Wind %.0f mph %.0f°",
                response.current.windSpeed10m,
                response.current.windDirection10m);
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

    private void populateDailyForecast(WeatherResponse response) {
        if (response == null || response.daily == null) return;

        textForecastTitle.setText("7-Day Forecast");
        buttonBackToDaily.setVisibility(View.GONE);
        forecastAdapter.setMode(ForecastAdapter.Mode.DAILY);

        DailyWeather d = response.daily;

        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> markers = new ArrayList<>();

        for (int i = 0; i < d.time.size(); i++) {
            String isoDate = d.time.get(i); // "2025-12-02"
            String dayLabel = isoDate;      // TODO: prettify
            labels.add(dayLabel);

            double tMin = d.temperatureMin.get(i);
            double tMax = d.temperatureMax.get(i);
            String range = String.format("%.0f° / %.0f°", tMin, tMax);
            values.add(range);

            double snow = d.snowfallSum != null && d.snowfallSum.size() > i
                    ? d.snowfallSum.get(i)
                    : 0.0;
            double precip = d.precipitationSum != null && d.precipitationSum.size() > i
                    ? d.precipitationSum.get(i)
                    : 0.0;
            String marker = String.format("Precip %.1f mm · Snow %.1f cm", precip, snow);
            markers.add(marker);
        }

        forecastAdapter.setData(labels, values, markers);
    }

    private void showHourlyForecastForDay(int dayIndex) {
        if (lastWeather == null || lastWeather.hourly == null || lastWeather.daily == null) {
            Log.w(TAG, "showHourlyForecastForDay: missing data");
            return;
        }

        textForecastTitle.setText("Hourly (selected day)");
        buttonBackToDaily.setVisibility(View.VISIBLE);
        forecastAdapter.setMode(ForecastAdapter.Mode.HOURLY);

        HourlyWeather h = lastWeather.hourly;
        DailyWeather d = lastWeather.daily;

        String dayDate = d.time.get(dayIndex); // e.g., "2025-12-02"
        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> markers = new ArrayList<>();

        for (int i = 0; i < h.time.size(); i++) {
            String iso = h.time.get(i); // "2025-12-02T07:00"
            if (!iso.startsWith(dayDate)) continue;

            String hourLabel = iso.substring(11, 16); // "HH:MM"
            labels.add(hourLabel);

            double temp = h.temperature2m.get(i);
            values.add(String.format("%.0f°", temp));

            String marker = "";
            String sunrise = d.sunrise.get(dayIndex);
            String sunset = d.sunset.get(dayIndex);
            if (iso.equals(sunrise)) {
                marker = "Sunrise";
            } else if (iso.equals(sunset)) {
                marker = "Sunset";
            }

            markers.add(marker);
        }

        // TODO: In v2, compute hi/lo temp times of day
        forecastAdapter.setData(labels, values, markers);
    }

    private void showDailyForecast() {
        Log.d(TAG, "showDailyForecast");
        if (lastWeather != null) {
            populateDailyForecast(lastWeather);
        }
    }

    private void navigateToPackingList(GearItem item) {
        Log.d(TAG, "navigateToPackingList: clicked gear " + item.getName());

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, PackingListFragment.newInstance())
                .addToBackStack(null)
                .commit();
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

    // Alternate approach:
    // - Extract the UI wiring into a HomeUiController to keep Fragment thinner.
    // - Use View Binding / Data Binding to avoid findViewById.
}
