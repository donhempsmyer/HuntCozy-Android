package dev.donhempsmyer.huntcozy.ui.closet;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.BodyZone;
import dev.donhempsmyer.huntcozy.data.model.GearAttributes;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.LayerType;
import dev.donhempsmyer.huntcozy.data.model.MaterialType;

/**
 * DialogFragment for creating a new GearItem.
 * Uses dialog_gear_form.xml.
 */
public class AddGearDialogFragment extends DialogFragment {

    private static final String TAG = "AddGearDialogFragment";

    public interface Listener {
        void onGearCreated(GearItem item);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_gear_form, null, false);

        EditText editName = view.findViewById(R.id.edit_gear_name);
        EditText editTempMin = view.findViewById(R.id.edit_temp_min);
        EditText editTempMax = view.findViewById(R.id.edit_temp_max);

        Spinner spinnerZone = view.findViewById(R.id.spinner_body_zone);
        Spinner spinnerLayer = view.findViewById(R.id.spinner_layer_type);
        Spinner spinnerMaterial = view.findViewById(R.id.spinner_material_type);

        SeekBar seekInsulation = view.findViewById(R.id.seek_insulation);
        SeekBar seekWind = view.findViewById(R.id.seek_wind);
        SeekBar seekWater = view.findViewById(R.id.seek_water);
        SeekBar seekBreath = view.findViewById(R.id.seek_breath);
        SeekBar seekNoise = view.findViewById(R.id.seek_noise);
        SeekBar seekMobility = view.findViewById(R.id.seek_mobility);

        // Populate spinners with enum values
        ArrayAdapter<BodyZone> zoneAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                BodyZone.values()
        );
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZone.setAdapter(zoneAdapter);

        ArrayAdapter<LayerType> layerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                LayerType.values()
        );
        layerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLayer.setAdapter(layerAdapter);

        ArrayAdapter<MaterialType> materialAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                MaterialType.values()
        );
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaterial.setAdapter(materialAdapter);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Gear")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Log.w(TAG, "Save clicked with empty name – ignoring");
                        return;
                    }

                    double tempMin = parseDoubleSafe(editTempMin.getText().toString(), 0);
                    double tempMax = parseDoubleSafe(editTempMax.getText().toString(), 40);

                    BodyZone zone = (BodyZone) spinnerZone.getSelectedItem();
                    LayerType layer = (LayerType) spinnerLayer.getSelectedItem();
                    MaterialType material = (MaterialType) spinnerMaterial.getSelectedItem();

                    int insulation = seekInsulation.getProgress();
                    int wind = seekWind.getProgress();
                    int water = seekWater.getProgress();
                    int breath = seekBreath.getProgress();
                    int noise = seekNoise.getProgress();
                    int mobility = seekMobility.getProgress();

                    GearAttributes attrs = new GearAttributes(
                            insulation,
                            wind,
                            water,
                            breath,
                            noise,
                            mobility,
                            tempMin,
                            tempMax,
                            false, // scentControl (future checkbox?)
                            true,  // quietFaceFabric (reasonable default)
                            3,     // weight
                            3      // bulk
                    );

                    GearItem item = new GearItem(
                            name,
                            zone,
                            layer,
                            material,
                            attrs
                    );

                    Log.d(TAG, "onCreateDialog: created GearItem=" + item.getName());

                    if (getParentFragment() instanceof Listener) {
                        ((Listener) getParentFragment()).onGearCreated(item);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d(TAG, "onCreateDialog: cancelled");
                })
                .create();
    }

    private double parseDoubleSafe(String s, double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
        }
        return fallback;
    }

    // Alternate approach:
    // - Use TextInputLayout/TextInputEditText for richer Material text fields.
}