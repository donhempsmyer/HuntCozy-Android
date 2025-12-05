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

public class EditGearDialogFragment extends DialogFragment {

    private static final String TAG = "EditGearDialogFragment";

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ZONE = "zone";
    private static final String ARG_LAYER = "layer";
    private static final String ARG_MATERIAL = "material";

    private static final String ARG_INSULATION = "insulation";
    private static final String ARG_WIND = "wind";
    private static final String ARG_WATER = "water";
    private static final String ARG_BREATH = "breath";
    private static final String ARG_NOISE = "noise";
    private static final String ARG_MOBILITY = "mobility";
    private static final String ARG_TEMP_MIN = "temp_min";
    private static final String ARG_TEMP_MAX = "temp_max";
    private static final String ARG_SCENT = "scent";
    private static final String ARG_QUIET = "quiet";
    private static final String ARG_WEIGHT = "weight";
    private static final String ARG_BULK = "bulk";

    public interface Listener {
        void onGearEdited(GearItem item);
    }

    public static EditGearDialogFragment newInstance(GearItem item) {
        EditGearDialogFragment f = new EditGearDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, item.getId());
        args.putString(ARG_NAME, item.getName());
        args.putString(ARG_ZONE, item.getBodyZone().name());
        args.putString(ARG_LAYER, item.getLayerType().name());
        args.putString(ARG_MATERIAL, item.getMaterialType().name());

        GearAttributes a = item.getAttributes();
        if (a != null) {
            args.putInt(ARG_INSULATION, a.getInsulationLevel());
            args.putInt(ARG_WIND, a.getWindProofLevel());
            args.putInt(ARG_WATER, a.getWaterProofLevel());
            args.putInt(ARG_BREATH, a.getBreathabilityLevel());
            args.putInt(ARG_NOISE, a.getNoiseLevel());
            args.putInt(ARG_MOBILITY, a.getMobilityLevel());
            args.putDouble(ARG_TEMP_MIN, a.getComfortTempMinC());
            args.putDouble(ARG_TEMP_MAX, a.getComfortTempMaxC());
            args.putBoolean(ARG_SCENT, a.hasScentControl());
            args.putBoolean(ARG_QUIET, a.hasQuietFaceFabric());
            args.putInt(ARG_WEIGHT, a.getWeightLevel());
            args.putInt(ARG_BULK, a.getBulkLevel());
        }
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");

        Bundle args = getArguments();
        if (args == null) {
            Log.w(TAG, "onCreateDialog: no args, dismissing");
            dismiss();
            return super.onCreateDialog(savedInstanceState);
        }

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

        // Populate spinners
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

        // Read args
        String id = args.getString(ARG_ID);
        String name = args.getString(ARG_NAME, "");
        BodyZone zone = BodyZone.valueOf(args.getString(ARG_ZONE));
        LayerType layer = LayerType.valueOf(args.getString(ARG_LAYER));
        MaterialType material = MaterialType.valueOf(args.getString(ARG_MATERIAL));

        int insulation = args.getInt(ARG_INSULATION, 5);
        int wind = args.getInt(ARG_WIND, 3);
        int water = args.getInt(ARG_WATER, 3);
        int breath = args.getInt(ARG_BREATH, 5);
        int noise = args.getInt(ARG_NOISE, 3);
        int mobility = args.getInt(ARG_MOBILITY, 7);
        double tempMin = args.getDouble(ARG_TEMP_MIN, 0);
        double tempMax = args.getDouble(ARG_TEMP_MAX, 40);
        boolean scent = args.getBoolean(ARG_SCENT, false);
        boolean quiet = args.getBoolean(ARG_QUIET, true);
        int weight = args.getInt(ARG_WEIGHT, 3);
        int bulk = args.getInt(ARG_BULK, 3);

        // Pre-fill controls
        editName.setText(name);
        editTempMin.setText(String.valueOf((int) tempMin));
        editTempMax.setText(String.valueOf((int) tempMax));

        spinnerZone.setSelection(zone.ordinal());
        spinnerLayer.setSelection(layer.ordinal());
        spinnerMaterial.setSelection(material.ordinal());

        seekInsulation.setProgress(insulation);
        seekWind.setProgress(wind);
        seekWater.setProgress(water);
        seekBreath.setProgress(breath);
        seekNoise.setProgress(noise);
        seekMobility.setProgress(mobility);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Gear")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Log.w(TAG, "Save clicked with empty name – ignoring");
                        return;
                    }

                    double newTempMin = parseDoubleSafe(editTempMin.getText().toString(), tempMin);
                    double newTempMax = parseDoubleSafe(editTempMax.getText().toString(), tempMax);

                    BodyZone newZone = (BodyZone) spinnerZone.getSelectedItem();
                    LayerType newLayer = (LayerType) spinnerLayer.getSelectedItem();
                    MaterialType newMaterial = (MaterialType) spinnerMaterial.getSelectedItem();

                    int newInsulation = seekInsulation.getProgress();
                    int newWind = seekWind.getProgress();
                    int newWater = seekWater.getProgress();
                    int newBreath = seekBreath.getProgress();
                    int newNoise = seekNoise.getProgress();
                    int newMobility = seekMobility.getProgress();

                    GearAttributes newAttrs = new GearAttributes(
                            newInsulation,
                            newWind,
                            newWater,
                            newBreath,
                            newNoise,
                            newMobility,
                            newTempMin,
                            newTempMax,
                            scent,
                            quiet,
                            weight,
                            bulk
                    );

                    GearItem updated = new GearItem(
                            id,
                            newName,
                            newZone,
                            newLayer,
                            newMaterial,
                            newAttrs,
                            null,
                            null,
                            true
                    );

                    Log.d(TAG, "onCreateDialog: updated GearItem=" + updated.getName());

                    if (getParentFragment() instanceof Listener) {
                        ((Listener) getParentFragment()).onGearEdited(updated);
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
            return fallback;
        }
    }

    // Alternate approach:
    // - Introduce a separate "GearFormViewModel" so both Add/Edit dialog
    //   fragments share the same form logic and validation.
}