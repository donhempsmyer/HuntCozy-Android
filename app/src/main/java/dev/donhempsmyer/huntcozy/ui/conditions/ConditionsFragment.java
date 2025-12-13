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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
    private LineChart chartPressure;
    private android.widget.Button buttonBackToWeekly; // NEW

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
        chartPressure = root.findViewById(R.id.chart_pressure);

        buttonBackToWeekly = root.findViewById(R.id.button_conditions_back_to_week);

        // Back button: return to weekly overview charts
        buttonBackToWeekly.setOnClickListener(v -> {
            if (lastWeather != null) {
                populateWeeklyCharts(lastWeather); // resets charts to weekly mode
            }
        });

        setupChartsAppearance();
    }

    // --- Location selection ---------------------------------------------------

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

            // 7-day strip at top
            populateDailyForecast(weather);

            // Default charts: weekly overview across 7 days (2-hour steps)
            populateWeeklyCharts(weather);

            // When user taps a day card, updateChartsForDay(dayIndex) will override
            // these charts with per-day detail.
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

    // --- 7-day DAILY strip (same as before, but wired to new adapter API) -----

    private void populateDailyForecast(WeatherResponse response) {
        if (response == null || response.daily == null) return;

        DailyWeather d = response.daily;

        List<String> labels = new ArrayList<>();
        List<String> primaryValues = new ArrayList<>();   // temp range
        List<String> secondaryValues = new ArrayList<>(); // precip/snow summary
        List<String> markers = new ArrayList<>();         // reserved for later

        java.text.SimpleDateFormat inputFormat =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        java.text.SimpleDateFormat outputFormat =
                new java.text.SimpleDateFormat("EEE MM/dd", java.util.Locale.US);

        int days = (d.time != null) ? d.time.size() : 0;
        for (int i = 0; i < days; i++) {
            String isoDate = d.time.get(i);

            String dayLabel = isoDate;
            try {
                java.util.Date parsed = inputFormat.parse(isoDate);
                if (parsed != null) {
                    dayLabel = outputFormat.format(parsed);
                }
            } catch (Exception ignored) {
            }
            labels.add(dayLabel);

            double tMin = (d.temperatureMin != null && d.temperatureMin.size() > i)
                    ? d.temperatureMin.get(i)
                    : 0.0;
            double tMax = (d.temperatureMax != null && d.temperatureMax.size() > i)
                    ? d.temperatureMax.get(i)
                    : 0.0;
            String range = String.format(java.util.Locale.US, "%.0f° / %.0f°", tMin, tMax);
            primaryValues.add(range);

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

            markers.add(""); // could mark "Best" day later
        }

        textForecastTitle.setText("7-Day Forecast");
        forecastAdapter.setMode(ForecastAdapter.Mode.DAILY);
        forecastAdapter.setData(labels, primaryValues, secondaryValues, markers);
    }

    // --- Weekly charts across 7 days (2h step) --------------------------------

    private void populateWeeklyCharts(WeatherResponse response) {
        if (response == null || response.hourly == null) {
            Log.w(TAG, "populateWeeklyCharts: missing hourly data");
            return;
        }

        HourlyWeather h = response.hourly;

        int totalHours = (h.time != null) ? h.time.size() : 0;
        int maxHours = Math.min(totalHours, 7 * 24);
        if (maxHours <= 0) {
            Log.w(TAG, "populateWeeklyCharts: no hourly points");
            return;
        }

        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> windEntries = new ArrayList<>();
        List<BarEntry> precipEntries = new ArrayList<>();
        List<Entry> pressureEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        java.text.SimpleDateFormat inputFormat =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US);
        java.text.SimpleDateFormat labelFormat =
                new java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.US); // "Mon 06:00"

        int stepIndex = 0;
        for (int i = 0; i < maxHours; i += 2, stepIndex++) {

            String timeStr = (h.time != null && h.time.size() > i)
                    ? h.time.get(i)
                    : null;

            String label = "";
            if (timeStr != null) {
                try {
                    java.util.Date parsed = inputFormat.parse(timeStr);
                    if (parsed != null) {
                        label = labelFormat.format(parsed);
                    }
                } catch (Exception ignored) {
                    label = timeStr; // fallback
                }
            }
            xLabels.add(label);

            double temp = safeGetDouble(h.temperature2m, i,
                    response.current != null ? response.current.temperature2m : 0.0);
            double windSpeed = safeGetDouble(h.windSpeed10m, i,
                    response.current != null ? response.current.windSpeed10m : 0.0);
            double precip = safeGetDouble(h.precipitation, i, 0.0);
            double pressureHpa = safeGetDouble(h.barometricPressure, i,
                    response.current != null ? response.current.barometricPressure : 0.0);
            double pressureInHg = hPaToInHg(pressureHpa);


            float x = stepIndex; // index-based x; labeled via xLabels

            tempEntries.add(new Entry(x, (float) temp));
            windEntries.add(new Entry(x, (float) windSpeed));
            precipEntries.add(new BarEntry(x, (float) precip));
            pressureEntries.add(new Entry(x, (float) pressureInHg));
        }

        textChartTitle.setText("Weekly overview (2-hour steps)");

        bindLineChart(chartTemp, "Temp (°F)", tempEntries, xLabels);
        bindLineChart(chartWind, "Wind (mph)", windEntries, xLabels);
        bindBarChart(chartPrecip, "Precip (in)", precipEntries, xLabels);
        bindLineChart(chartPressure, "Pressure (inHg)", pressureEntries, xLabels);

        if (buttonBackToWeekly != null) {
            buttonBackToWeekly.setVisibility(View.GONE);
        }
    }

    /**
     * Per-day detail when user taps a card in the 7-day strip.
     * Uses the same charts but focuses only on that date.
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
        List<Entry> pressureEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        int hourIndexForX = 0;
        for (int i = 0; i < h.time.size(); i++) {
            String iso = h.time.get(i); // "YYYY-MM-DDTHH:MM"
            if (iso == null || !iso.startsWith(dayDate)) continue;

            String hourLabel = iso.length() >= 16 ? iso.substring(11, 16) : iso;
            xLabels.add(hourLabel);

            double temp = safeGetDouble(h.temperature2m, i,
                    lastWeather.current != null ? lastWeather.current.temperature2m : 0.0);
            double wind = safeGetDouble(h.windSpeed10m, i,
                    lastWeather.current != null ? lastWeather.current.windSpeed10m : 0.0);
            double precip = safeGetDouble(h.precipitation, i, 0.0);
            double pressureHpa = safeGetDouble(h.barometricPressure, i,
                    lastWeather.current != null ? lastWeather.current.barometricPressure : 0.0);
            double pressureInHg = hPaToInHg(pressureHpa);


            tempEntries.add(new Entry(hourIndexForX, (float) temp));
            windEntries.add(new Entry(hourIndexForX, (float) wind));
            precipEntries.add(new BarEntry(hourIndexForX, (float) precip));
            pressureEntries.add(new Entry(hourIndexForX, (float) pressureInHg));

            hourIndexForX++;
        }

        if (tempEntries.isEmpty()) {
            Snackbar.make(requireView(), "No hourly data for this day", Snackbar.LENGTH_SHORT).show();
            return;
        }

        bindLineChart(chartTemp, "Temp (°F)", tempEntries, xLabels);
        bindLineChart(chartWind, "Wind (mph)", windEntries, xLabels);
        bindBarChart(chartPrecip, "Precip (in)", precipEntries, xLabels);
        bindLineChart(chartPressure, "Pressure (inHg)", pressureEntries, xLabels);

        if (buttonBackToWeekly != null) {
            buttonBackToWeekly.setVisibility(View.VISIBLE);
        }
    }

    // --- Chart appearance ----------------------------------------------------

    /**
     * Configure chart colors for dark background:
     *  - white axis/labels/legend text
     *  - soft grid lines
     *  - transparent background to blend with fragment bg
     */
    private void setupChartsAppearance() {
        styleLineChart(chartTemp);
        styleLineChart(chartWind);
        styleLineChart(chartPressure);
        styleBarChart(chartPrecip);
    }

    private void styleLineChart(LineChart chart) {
        if (chart == null) return;

        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        chart.setNoDataText("No data");
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

    private void styleBarChart(BarChart chart) {
        if (chart == null) return;

        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        chart.setNoDataText("No data");
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

    // --- Reusable binding helpers -------------------------------------------

    private void bindLineChart(LineChart chart,
                               String label,
                               List<Entry> entries,
                               List<String> xLabels) {
        if (chart == null) return;
        if (entries == null || entries.isEmpty()) {
            chart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(0xFFFFFFFF);     // white line
        dataSet.setHighLightColor(0xFFAAAAAA);

        LineData data = new LineData(dataSet);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        YAxis left = chart.getAxisLeft();
        left.setTextColor(0xFFFFFFFF);
        left.setDrawGridLines(true);

        YAxis right = chart.getAxisRight();
        right.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(0xFFFFFFFF);

        chart.invalidate();
    }

    private void bindBarChart(BarChart chart,
                              String label,
                              List<BarEntry> entries,
                              List<String> xLabels) {
        if (chart == null) return;
        if (entries == null || entries.isEmpty()) {
            chart.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setDrawValues(false);
        dataSet.setColor(0xFFFFFFFF);   // white bars

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);

        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        YAxis left = chart.getAxisLeft();
        left.setTextColor(0xFFFFFFFF);
        left.setDrawGridLines(true);

        YAxis right = chart.getAxisRight();
        right.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(0xFFFFFFFF);

        chart.setFitBars(true);
        chart.invalidate();
    }

    // --- Small utility -------------------------------------------------------

    // Convert from hPa (Open-Meteo default) to inHg for display
    private double hPaToInHg(double hPa) {
        return hPa * 0.0295299830714; // 1 hPa ≈ 0.02953 inHg
    }
    private double safeGetDouble(List<Double> list, int index, double fallback) {
        if (list == null || index < 0 || index >= list.size()) return fallback;
        Double v = list.get(index);
        return (v != null) ? v : fallback;
    }
}