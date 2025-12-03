package dev.donhempsmyer.huntcozy.ui.home;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;

import java.util.ArrayList;
import java.util.List;

public class GearAdapter extends RecyclerView.Adapter<GearAdapter.GearViewHolder> {

    private static final String TAG = "GearAdapter";

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
                .inflate(R.layout.item_gear_recommendation, parent, false);
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
        TextView textCategory;

        GearViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_gear_name);
            textCategory = itemView.findViewById(R.id.text_gear_category);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onGearClicked(items.get(pos));
                }
            });
        }

        void bind(GearItem item) {
            textName.setText(item.getName());
            textCategory.setText(item.getCategory());
        }
    }
}
