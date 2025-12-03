package dev.donhempsmyer.huntcozy.ui.home;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.donhempsmyer.huntcozy.R;

import java.util.ArrayList;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ForecastAdapter";

    public interface OnDayClickListener {
        void onDayClicked(int dayIndex);
    }

    public enum Mode {
        DAILY,
        HOURLY
    }

    private Mode mode = Mode.DAILY;
    private final List<String> labels = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private final List<String> markers = new ArrayList<>();

    private OnDayClickListener dayClickListener;

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.dayClickListener = listener;
    }

    public void setMode(Mode mode) {
        Log.d(TAG, "setMode: " + mode);
        this.mode = mode;
        notifyDataSetChanged();
    }

    public void setData(List<String> labels, List<String> values, List<String> markers) {
        this.labels.clear();
        this.values.clear();
        this.markers.clear();

        if (labels != null) this.labels.addAll(labels);
        if (values != null) this.values.addAll(values);
        if (markers != null) this.markers.addAll(markers);

        Log.d(TAG, "setData: count=" + this.labels.size());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mode == Mode.DAILY ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == 0) {
            View view = inflater.inflate(R.layout.item_forecast_day, parent, false);
            return new DailyViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_forecast_hour, parent, false);
            return new HourlyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        String label = labels.get(position);
        String value = values.get(position);
        String marker = markers.size() > position ? markers.get(position) : "";

        if (holder instanceof DailyViewHolder) {
            ((DailyViewHolder) holder).bind(label, value, marker, position);
        } else if (holder instanceof HourlyViewHolder) {
            ((HourlyViewHolder) holder).bind(label, value, marker);
        }
    }

    class DailyViewHolder extends RecyclerView.ViewHolder {

        TextView textDayLabel;
        TextView textTempRange;
        TextView textPrecip;

        DailyViewHolder(@NonNull View itemView) {
            super(itemView);
            textDayLabel = itemView.findViewById(R.id.text_day_label);
            textTempRange = itemView.findViewById(R.id.text_temp_range);
            textPrecip = itemView.findViewById(R.id.text_precip_summary);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && dayClickListener != null) {
                    dayClickListener.onDayClicked(pos);
                }
            });
        }

        void bind(String label, String tempRange, String precip, int position) {
            textDayLabel.setText(label);
            textTempRange.setText(tempRange);
            textPrecip.setText(precip);
        }
    }

    class HourlyViewHolder extends RecyclerView.ViewHolder {

        TextView textHourLabel;
        TextView textHourTemp;
        TextView textHourMarker;

        HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            textHourLabel = itemView.findViewById(R.id.text_hour_label);
            textHourTemp = itemView.findViewById(R.id.text_hour_temp);
            textHourMarker = itemView.findViewById(R.id.text_hour_marker);
        }

        void bind(String hourLabel, String temp, String marker) {
            textHourLabel.setText(hourLabel);
            textHourTemp.setText(temp);
            textHourMarker.setText(marker);
        }
    }
}
