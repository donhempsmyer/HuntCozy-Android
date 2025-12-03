package dev.donhempsmyer.huntcozy.ui.home;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.LocationModel;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.weather.DailyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.HourlyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private HomeViewModel viewModel;

    private TextView textLocationName;
    private TextView textCurrentTemp;
    private TextView textCurrentWind;
    private TextView textCurrentPrecip;
    private Spinner spinnerLocation;
    private Spinner spinnerWeapon;
    private Spinner spinnerHuntingStyle;
    private RecyclerView recyclerForecast;
    private RecyclerView recyclerGear;
    private TextView textForecastTitle;
    private Button buttonBackToDaily;

    private ForecastAdapter forecastAdapter;
    private GearAdapter gearAdapter;

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

        buttonBackToDaily.setOnClickListener(v -> showDailyForecast());
    }

    private void setupLocationSpinner() {
        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            Log.d(TAG, "setupLocationSpinner: locations size=" + (locations != null ? locations.size() : 0));
            if (locations == null) return;

            ArrayAdapter<LocationModel> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    locations
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLocation.setAdapter(adapter);

            LocationModel selected = viewModel.getSelectedLocation().getValue();
            if (selected != null) {
                int index = locations.indexOf(selected);
                if (index >= 0) spinnerLocation.setSelection(index);
            }

            spinnerLocation.setOnItemSelectedListener(new SimpleItemSelectedListener() {
                @Override
                public void onItemSelected(int position) {
                    LocationModel loc = locations.get(position);
                    viewModel.selectLocation(loc);
                }
            });
        });
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

        textLocationName.setText(viewModel.getSelectedLocation().getValue() != null
                ? viewModel.getSelectedLocation().getValue().getName()
                : "Selected Location");

        String tempText = String.format("%.0f°C (Feels %.0f°C)",
                response.current.temperature2m,
                response.current.apparentTemperature);
        textCurrentTemp.setText(tempText);

        String windText = String.format("Wind %.0f km/h %.0f°",
                response.current.windSpeed10m,
                response.current.windDirection10m);
        textCurrentWind.setText(windText);

        String precipText = String.format("Precip %.1f mm · Snow %.1f cm · Pressure %.0f hPa",
                response.current.precipitation,
                response.current.snowfall,
                response.current.pressureMsl);
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
            String dayLabel = isoDate; // TODO: prettify using LocalDate short day-of-week
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

        // Find indices in hourly.time that match this date
        for (int i = 0; i < h.time.size(); i++) {
            String iso = h.time.get(i); // "2025-12-02T07:00"
            if (!iso.startsWith(dayDate)) continue;

            String hourLabel = iso.substring(11, 16); // "HH:MM"
            labels.add(hourLabel);

            double temp = h.temperature2m.get(i);
            values.add(String.format("%.0f°", temp));

            // Marker: sunrise/sunset/hi/low (basic v1)
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

        // TODO: In v2, compute actual time-of-day for hi/lo temp within this day
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

        // For now just navigate to PackingListFragment (no arguments).
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, PackingListFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    // Simple helper class to avoid boilerplate in spinner listeners
    private abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) { }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                   int position, long id) {
            onItemSelected(position);
        }

        public abstract void onItemSelected(int position);
    }

    // Alternate approach:
    // - Put all this binding logic into a separate HomeUiController class and keep Fragment thinner.
    // - Use Data Binding / View Binding to avoid findViewById.
}
