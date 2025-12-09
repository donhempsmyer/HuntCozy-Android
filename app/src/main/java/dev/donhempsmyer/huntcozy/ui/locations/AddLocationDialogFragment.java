package dev.donhempsmyer.huntcozy.ui.locations;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

/**
 * Dialog for adding a new hunting location.
 * v1: simple name + lat + lon, directly inserts into LocationsRepository.
 * v2: we can add validation, notes, map picker, etc.
 */
public class AddLocationDialogFragment extends DialogFragment {

    private static final String TAG = "AddLocationDialog";

    public static AddLocationDialogFragment newInstance() {
        return new AddLocationDialogFragment();
    }

    private EditText editName;
    private EditText editLat;
    private EditText editLon;

    private LocationsRepository locationsRepository;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        locationsRepository = LocationsRepository.getInstance();

        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_add_location_dialog, null, false);

        editName = contentView.findViewById(R.id.edit_location_name);
        editLat = contentView.findViewById(R.id.edit_location_lat);
        editLon = contentView.findViewById(R.id.edit_location_lon);

        // We override the positive button in onStart() so we can prevent auto-dismiss on invalid input
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Location")
                .setView(contentView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // real handler is in onStart()
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Override "Save" button to handle validation without auto-dismissing
        Dialog dialog = getDialog();
        if (dialog == null) return;

        Button positiveButton = ((androidx.appcompat.app.AlertDialog) dialog)
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);

        positiveButton.setOnClickListener(v -> onSaveClicked());
    }

    private void onSaveClicked() {
        String name = editName.getText().toString().trim();
        String latStr = editLat.getText().toString().trim();
        String lonStr = editLon.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editName.setError("Required");
            return;
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(latStr);
            lon = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "onSaveClicked: invalid lat/lon", e);
            Toast.makeText(requireContext(),
                    "Please enter valid latitude and longitude",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // v1: simple range sanity check (not exhaustive)
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            Toast.makeText(requireContext(),
                    "Lat must be between -90 and 90, Lon between -180 and 180",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new HuntLocation.
        // Adjust this constructor to match your actual HuntLocation definition.
        // Here we use System.currentTimeMillis() as a simple unique id.
        long newId = System.currentTimeMillis();
        HuntLocation newLocation = new HuntLocation(newId, name, lat, lon);

        Log.d(TAG, "onSaveClicked: adding new location: " + newLocation);

        // Insert into repository (you may need to implement addLocation() if not present).
        locationsRepository.addLocation(newLocation);

        // Optionally also select it as current:
        locationsRepository.selectLocation(newLocation);

        Toast.makeText(requireContext(), "Location added", Toast.LENGTH_SHORT).show();

        dismiss();
    }

    // Alternate approach:
    // - Use FragmentResult API to send the new HuntLocation back to LocationsFragment,
    //   and let that Fragment decide whether/how to store it.
    // - Use a map picker instead of manual lat/lon entry.
}