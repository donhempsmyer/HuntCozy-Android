package dev.donhempsmyer.huntcozy.ui.packing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;

/**
 * Generic adapter used for staged, loadout, and packed lists, with a mode flag.
 */
public class PackingListAdapter extends RecyclerView.Adapter<PackingListAdapter.ViewHolder> {

    public enum Mode {
        STAGED,
        LOADOUT,
        PACKED
    }

    public interface OnItemCheckedListener {
        void onItemChecked(PackingItem item, boolean checked);
    }

    public interface OnItemClickListener {
        void onItemClicked(PackingItem item);
    }

    private final Mode mode;
    private final List<PackingItem> items = new ArrayList<>();

    private OnItemCheckedListener checkedListener;
    private OnItemClickListener clickListener;

    public PackingListAdapter(Mode mode) {
        this.mode = mode;
    }

    public void setItems(List<PackingItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        this.checkedListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public PackingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (mode == Mode.PACKED)
                ? R.layout.item_packing_packed
                : R.layout.item_packing_checkable;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PackingListAdapter.ViewHolder holder, int position) {
        PackingItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox checkBox;
        private final TextView label;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.check_packing_item);
            label = itemView.findViewById(R.id.text_packing_label);
        }

        void bind(PackingItem item) {
            label.setText(item.getLabel());

            // 🔥 1) Remove listener before changing checked state
            checkBox.setOnCheckedChangeListener(null);

            // 🔥 2) Set correct state based on mode
            boolean isChecked = (mode == Mode.PACKED);
            checkBox.setChecked(isChecked);

            // Optional: ensure visual state is applied immediately
            checkBox.jumpDrawablesToCurrentState();

            // 🔥 3) Reattach listener
            checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
                if (checkedListener != null) {
                    checkedListener.onItemChecked(item, checked);
                }
            });

            // Row click support
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClicked(item);
                }
            });
        }
    }
}
