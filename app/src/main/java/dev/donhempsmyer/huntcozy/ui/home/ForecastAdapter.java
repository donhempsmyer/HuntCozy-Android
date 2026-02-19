package dev.donhempsmyer.huntcozy.ui.home;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;

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
    private final List<String> primaryValues = new ArrayList<>();
    private final List<String> secondaryValues = new ArrayList<>();
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

    /**
     * All cards (daily or hourly) are driven by 4 parallel string lists:
     *  - labels          (left/top)
     *  - primaryValues   (big number)
     *  - secondaryValues (smaller descriptive line)
     *  - markers         (badge: "Sunrise"/"Sunset"/etc.)
     */
    public void setData(List<String> labels,
                        List<String> primaryValues,
                        List<String> secondaryValues,
                        List<String> markers) {

        this.labels.clear();
        this.primaryValues.clear();
        this.secondaryValues.clear();
        this.markers.clear();

        if (labels != null) this.labels.addAll(labels);
        if (primaryValues != null) this.primaryValues.addAll(primaryValues);
        if (secondaryValues != null) this.secondaryValues.addAll(secondaryValues);
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
        return (mode == Mode.DAILY) ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == 0) {
            // Daily card layout
            View view = inflater.inflate(R.layout.item_forecast_day, parent, false);
            return new DailyViewHolder(view);
        } else {
            // Hourly card layout
            View view = inflater.inflate(R.layout.item_forecast_hour, parent, false);
            return new HourlyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        String label     = labels.get(position);
        String primary   = primaryValues.size()   > position ? primaryValues.get(position)   : "";
        String secondary = secondaryValues.size() > position ? secondaryValues.get(position) : "";
        String marker    = markers.size()         > position ? markers.get(position)         : "";

        if (holder instanceof DailyViewHolder) {
            ((DailyViewHolder) holder).bind(label, primary, secondary, marker);
        } else if (holder instanceof HourlyViewHolder) {
            ((HourlyViewHolder) holder).bind(label, primary, secondary, marker);
        }
    }

    // --- DAILY VIEW HOLDER (7-day forecast) ---------------------------------
    class DailyViewHolder extends RecyclerView.ViewHolder {

        private final TextView textLabel;
        private final TextView textPrimary;
        private final TextView textSecondary;
        private final TextView textMarker;

        DailyViewHolder(@NonNull View itemView) {
            super(itemView);
            // IDs must match item_forecast_day.xml
            textLabel     = itemView.findViewById(R.id.text_forecast_label);
            textPrimary   = itemView.findViewById(R.id.text_forecast_value);
            textSecondary = itemView.findViewById(R.id.text_forecast_secondary);
            textMarker    = itemView.findViewById(R.id.text_forecast_marker);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && dayClickListener != null) {
                    dayClickListener.onDayClicked(pos);
                }
            });
        }

        void bind(String label, String primary, String secondary, String marker) {
            textLabel.setText(label);
            textPrimary.setText(primary);
            textSecondary.setText(secondary);
            textMarker.setText(marker);
        }
    }

    // --- HOURLY VIEW HOLDER (hourly forecast) --------------------------------
    class HourlyViewHolder extends RecyclerView.ViewHolder {

        private final TextView textLabel;
        private final TextView textPrimary;
        private final TextView textSecondary;
        private final TextView textMarker;

        HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            // Same IDs in item_forecast_hour.xml for consistency
            textLabel     = itemView.findViewById(R.id.text_forecast_label);
            textPrimary   = itemView.findViewById(R.id.text_forecast_value);
            textSecondary = itemView.findViewById(R.id.text_forecast_secondary);
            textMarker    = itemView.findViewById(R.id.text_forecast_marker);
        }

        void bind(String label, String primary, String secondary, String marker) {
            textLabel.setText(label);
            textPrimary.setText(primary);
            textSecondary.setText(secondary);
            textMarker.setText(marker);
        }
    }
}
