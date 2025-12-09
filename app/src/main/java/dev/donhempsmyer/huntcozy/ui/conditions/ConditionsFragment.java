package dev.donhempsmyer.huntcozy.ui.conditions;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.data.model.weather.DailyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.HourlyWeather;
import dev.donhempsmyer.huntcozy.data.model.weather.WeatherResponse;
import dev.donhempsmyer.huntcozy.data.repository.OpenMeteoWeatherRepository;
import dev.donhempsmyer.huntcozy.ui.home.ForecastAdapter;

public class ConditionsFragment extends Fragment {

    private static final String TAG = "ConditionsFragment";
    private static final String ARG_LOCATION_ID = "arg_location_id";

    public static ConditionsFragment newInstance(long locationId) {
        ConditionsFragment f = new ConditionsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_LOCATION_ID, locationId);
        f.setArguments(args);
        return f;
    }

    public static ConditionsFragment newInstance() {
        // No specific location id passed; fragment will rely on
        // LocationsRepository.getSelectedLocation() or default.
        return new ConditionsFragment();
    }

    private LocationsRepository locationsRepository;
    private OpenMeteoWeatherRepository weatherRepository;

    // UI
    private Spinner spinnerLocation;
    private TextView textForecastTitle;
    private RecyclerView recyclerForecast;
    private TextView textChartTitle;
    private LineChart chartTemp;
    private LineChart chartWind;
    private BarChart chartPrecip;

    private ForecastAdapter forecastAdapter;
    private ArrayAdapter<String> locationAdapter;
    private final List<HuntLocation> locationItems = new ArrayList<>();

    private WeatherResponse lastWeather;
    private int selectedDayIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_conditions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        locationsRepository = LocationsRepository.getInstance();
        weatherRepository = OpenMeteoWeatherRepository.getInstance();

        bindViews(view);
        setupLocationSpinner();
        setupForecastRecycler();
        observeData();

        // If a location id was passed in, select it
        long passedId = getArguments() != null ? getArguments().getLong(ARG_LOCATION_ID, -1) : -1;
        if (passedId != -1) {
            selectLocationById(passedId);
        }

    }

    private void bindViews(View root) {
        spinnerLocation = root.findViewById(R.id.spinner_location_conditions);
        textForecastTitle = root.findViewById(R.id.text_conditions_forecast_title);
        recyclerForecast = root.findViewById(R.id.recycler_conditions_forecast);
        textChartTitle = root.findViewById(R.id.text_conditions_chart_title);
        chartTemp = root.findViewById(R.id.chart_temp);
        chartWind = root.findViewById(R.id.chart_wind);
        chartPrecip = root.findViewById(R.id.chart_precip);
        setupChartsAppearance();

    }

    private void setupLocationSpinner() {
        locationAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                if (position < 0 || position >= locationItems.size()) return;
                HuntLocation loc = locationItems.get(position);
                Log.d(TAG, "spinnerLocation onItemSelected: " + loc);
                locationsRepository.selectLocation(loc);
                weatherRepository.refreshWeatherForLocation(
                        loc.getLatitude(),
                        loc.getLongitude()
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupForecastRecycler() {
        forecastAdapter = new ForecastAdapter();
        forecastAdapter.setOnDayClickListener(dayIndex -> {
            Log.d(TAG, "onDayClick: " + dayIndex);
            selectedDayIndex = dayIndex;
            updateChartsForDay(dayIndex);
        });

        recyclerForecast.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        recyclerForecast.setAdapter(forecastAdapter);
    }

    private void observeData() {
        locationsRepository.getLocations().observe(getViewLifecycleOwner(), locations -> {
            Log.d(TAG, "observeData: locations size=" + (locations != null ? locations.size() : 0));
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

        locationsRepository.getSelectedLocation().observe(getViewLifecycleOwner(), selected -> {
            Log.d(TAG, "observeData: selectedLocation=" + selected);
            syncSpinnerWithSelectedLocation();
        });

        weatherRepository.getWeatherLiveData().observe(getViewLifecycleOwner(), weather -> {
            Log.d(TAG, "observeData: weather updated in ConditionsFragment");
            lastWeather = weather;
            populateDailyForecast(weather);
            updateChartsForDay(selectedDayIndex);
        });
    }

    private void syncSpinnerWithSelectedLocation() {
        HuntLocation selected = locationsRepository.getSelectedLocation().getValue();
        if (selected == null || locationItems.isEmpty()) return;

        int index = -1;
        for (int i = 0; i < locationItems.size(); i++) {
            if (locationItems.get(i).getId() == selected.getId()) {
                index = i;
                break;
            }
        }

        if (index >= 0 && spinnerLocation.getSelectedItemPosition() != index) {
            Log.d(TAG, "syncSpinnerWithSelectedLocation: index=" + index);
            spinnerLocation.setSelection(index);
        }
    }

    private void selectLocationById(long id) {
        HuntLocation selected = null;
        for (HuntLocation loc : locationItems) {
            if (loc.getId() == id) {
                selected = loc;
                break;
            }
        }
        if (selected != null) {
            locationsRepository.selectLocation(selected);
        }
    }

    private void populateDailyForecast(WeatherResponse response) {
        if (response == null || response.daily == null) return;

        textForecastTitle.setText("7-Day Forecast");
        forecastAdapter.setMode(ForecastAdapter.Mode.DAILY);

        DailyWeather d = response.daily;
        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> markers = new ArrayList<>();

        for (int i = 0; i < d.time.size(); i++) {
            String isoDate = d.time.get(i); // "2025-12-02"
            labels.add(isoDate);

            double tMin = d.temperatureMin.get(i);
            double tMax = d.temperatureMax.get(i);
            values.add(String.format("%.0f° / %.0f°", tMin, tMax));

            double precip = (d.precipitationSum != null && d.precipitationSum.size() > i)
                    ? d.precipitationSum.get(i)
                    : 0.0;
            double snow = (d.snowfallSum != null && d.snowfallSum.size() > i)
                    ? d.snowfallSum.get(i)
                    : 0.0;
            markers.add(String.format("Precip %.1f in · Snow %.1f in", precip, snow));
        }

        forecastAdapter.setData(labels, values, markers);
    }

    /**
     * Build chart data for the selected day using hourly arrays.
     */
    private void updateChartsForDay(int dayIndex) {
        if (lastWeather == null || lastWeather.hourly == null || lastWeather.daily == null) {
            Log.w(TAG, "updateChartsForDay: missing weather data");
            return;
        }

        DailyWeather d = lastWeather.daily;
        HourlyWeather h = lastWeather.hourly;

        if (dayIndex < 0 || dayIndex >= d.time.size()) {
            Log.w(TAG, "updateChartsForDay: invalid dayIndex=" + dayIndex);
            return;
        }

        String dayDate = d.time.get(dayIndex); // "YYYY-MM-DD"
        textChartTitle.setText("Hourly for " + dayDate);

        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> windEntries = new ArrayList<>();
        List<BarEntry> precipEntries = new ArrayList<>();

        int hourIndexForX = 0;
        for (int i = 0; i < h.time.size(); i++) {
            String iso = h.time.get(i); // "YYYY-MM-DDTHH:MM"
            if (!iso.startsWith(dayDate)) continue;

            double temp = h.temperature2m.get(i);
            double wind = h.windSpeed10m.get(i);
            double precip = h.precipitation.get(i);

            tempEntries.add(new Entry(hourIndexForX, (float) temp));
            windEntries.add(new Entry(hourIndexForX, (float) wind));
            precipEntries.add(new BarEntry(hourIndexForX, (float) precip));

            hourIndexForX++;
        }

        if (tempEntries.isEmpty()) {
            Snackbar.make(requireView(), "No hourly data for this day", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Temp chart
        LineDataSet tempSet = new LineDataSet(tempEntries, "Temp (°F)");
        tempSet.setDrawCircles(false);
        tempSet.setLineWidth(2f);
        LineData tempData = new LineData(tempSet);
        chartTemp.setData(tempData);
        chartTemp.invalidate();

        // Wind chart
        LineDataSet windSet = new LineDataSet(windEntries, "Wind (mph)");
        windSet.setDrawCircles(false);
        windSet.setLineWidth(2f);
        LineData windData = new LineData(windSet);
        chartWind.setData(windData);
        chartWind.invalidate();

        // Precip chart
        BarDataSet precipSet = new BarDataSet(precipEntries, "Precip (in)");
        BarData precipData = new BarData(precipSet);
        chartPrecip.setData(precipData);
        chartPrecip.invalidate();

        // Alternate approach:
        // - Use a single CombinedChart with multiple axes instead of three charts.
        // - Add time-of-day labels on X axis via ValueFormatter for actual hours.
    }


    /**
     * Configure chart colors for dark background:
     *  - white axis/labels/legend text
     *  - soft grid lines
     *  - no opaque background so it blends with fragment bg
     */
    private void setupChartsAppearance() {
        styleLineChart(chartTemp);
        styleLineChart(chartWind);
        styleBarChart(chartPrecip);
    }

    private void styleLineChart(LineChart chart) {
        if (chart == null) return;

        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        // "No data" text
        chart.setNoDataText("No data for selected day");
        chart.setNoDataTextColor(Color.WHITE);

        // X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisLineColor(0x80FFFFFF);   // semi-transparent white
        xAxis.setGridColor(0x40FFFFFF);       // faint grid

        // Left Y axis
        YAxis left = chart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setAxisLineColor(0x80FFFFFF);
        left.setGridColor(0x40FFFFFF);

        // Right Y axis
        YAxis right = chart.getAxisRight();
        right.setTextColor(Color.WHITE);
        right.setAxisLineColor(0x80FFFFFF);
        right.setGridColor(0x40FFFFFF);

        // Legend
        Legend legend = chart.getLegend();
        legend.setTextColor(Color.WHITE);

        // Description (if you use it)
        Description desc = chart.getDescription();
        if (desc != null) {
            desc.setTextColor(Color.WHITE);
        }
    }

    private void styleBarChart(BarChart chart) {
        if (chart == null) return;

        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        chart.setNoDataText("No data for selected day");
        chart.setNoDataTextColor(Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisLineColor(0x80FFFFFF);
        xAxis.setGridColor(0x40FFFFFF);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setAxisLineColor(0x80FFFFFF);
        left.setGridColor(0x40FFFFFF);

        YAxis right = chart.getAxisRight();
        right.setTextColor(Color.WHITE);
        right.setAxisLineColor(0x80FFFFFF);
        right.setGridColor(0x40FFFFFF);

        Legend legend = chart.getLegend();
        legend.setTextColor(Color.WHITE);

        Description desc = chart.getDescription();
        if (desc != null) {
            desc.setTextColor(Color.WHITE);
        }
    }
}