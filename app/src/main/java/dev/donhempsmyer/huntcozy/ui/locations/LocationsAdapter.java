package dev.donhempsmyer.huntcozy.ui.locations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(HuntLocation location);
    }

    private final List<HuntLocation> items = new ArrayList<>();
    private OnLocationClickListener clickListener;

    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<HuntLocation> locations) {
        items.clear();
        if (locations != null) {
            items.addAll(locations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        HuntLocation loc = items.get(position);
        holder.bind(loc);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textCoords;

        LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_location_name);
            textCoords = itemView.findViewById(R.id.text_location_coords);
        }

        void bind(HuntLocation location) {
            textName.setText(location.getName());
            String coordText = String.format("Lat %.3f, Lon %.3f",
                    location.getLatitude(), location.getLongitude());
            textCoords.setText(coordText);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onLocationClick(location);
                }
            });
        }
    }
}
