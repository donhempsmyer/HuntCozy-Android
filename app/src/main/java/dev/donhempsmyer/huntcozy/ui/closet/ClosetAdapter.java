package dev.donhempsmyer.huntcozy.ui.closet;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearAttributes;
import dev.donhempsmyer.huntcozy.data.model.GearItem;

import java.util.ArrayList;
import java.util.List;

/**
 * ClosetAdapter displays the user's gear in a RecyclerView.
 */
public class ClosetAdapter extends RecyclerView.Adapter<ClosetAdapter.GearViewHolder> {

    private static final String TAG = "ClosetAdapter";

    public interface OnGearClickListener {
        void onGearClicked(GearItem item);
    }

    private final List<GearItem> items = new ArrayList<>();
    private OnGearClickListener clickListener;

    public void setOnGearClickListener(OnGearClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<GearItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        Log.d(TAG, "setItems: count=" + items.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_closet_gear, parent, false);
        return new GearViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GearViewHolder holder, int position) {
        GearItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class GearViewHolder extends RecyclerView.ViewHolder {

        TextView textName;
        TextView textZoneLayer;
        TextView textMaterial;
        TextView textAttributes;

        GearViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_gear_name);
            textZoneLayer = itemView.findViewById(R.id.text_gear_zone_layer);
            textMaterial = itemView.findViewById(R.id.text_gear_material);
            textAttributes = itemView.findViewById(R.id.text_gear_attributes);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onGearClicked(items.get(pos));
                }
            });
        }

        void bind(GearItem item) {
            textName.setText(item.getName());

            String zoneLayer = item.getBodyZone() + " · " + item.getLayerType();
            textZoneLayer.setText(zoneLayer);

            String material = item.getMaterialType().name().replace('_', ' ');
            textMaterial.setText(material);

            GearAttributes a = item.getAttributes();
            if (a != null) {
                String attrText = "Ins " + a.getInsulationLevel()
                        + " • Wind " + a.getWindProofLevel()
                        + " • Water " + a.getWaterProofLevel()
                        + " • Breath " + a.getBreathabilityLevel();
                textAttributes.setText(attrText);
            } else {
                textAttributes.setText("No attributes");
            }
        }
    }

    // Alternate approach:
    // - Use ListAdapter + DiffUtil for smoother animations when updating the list.
}
