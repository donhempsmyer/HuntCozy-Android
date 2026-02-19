package dev.donhempsmyer.huntcozy.ui.locations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment;
import dev.donhempsmyer.huntcozy.ui.main.MainActivity;

public class LocationsFragment extends Fragment {

    private static final String TAG = "LocationsFragment";

    public static LocationsFragment newInstance() {
        return new LocationsFragment();
    }

    private LocationsViewModel viewModel;
    private LocationsAdapter adapter;
    private RecyclerView recyclerLocations;
    private FloatingActionButton fabAddLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        viewModel = new ViewModelProvider(this).get(LocationsViewModel.class);

        recyclerLocations = view.findViewById(R.id.recycler_locations);
        fabAddLocation = view.findViewById(R.id.fab_add_location);

        setupRecycler();
        setupFab();
        observeViewModel();
    }

    private void setupRecycler() {
        adapter = new LocationsAdapter();
        adapter.setOnLocationClickListener(this::onLocationClicked);

        recyclerLocations.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerLocations.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddLocation.setOnClickListener(v -> {
            Log.d(TAG, "onClick: fabAddLocation");
            AddLocationDialogFragment dialog = AddLocationDialogFragment.newInstance();
            dialog.show(getChildFragmentManager(), "AddLocationDialog");
        });
    }

    private void observeViewModel() {
        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            int count = locations != null ? locations.size() : 0;
            Log.d(TAG, "observeViewModel: locations count=" + count);
            adapter.setItems(locations);
        });
    }

    private void onLocationClicked(HuntLocation location) {
        if (location == null) return;

        Log.d(TAG, "onLocationClicked: " + location);

        // 1) Mark this as the selected location in the shared repository
        LocationsRepository.getInstance().selectLocation(location);

        // 2) Ask MainActivity to switch to the Conditions tab
        if (requireActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) requireActivity();
            activity.openConditionsTab();
        } else {
            // Fallback: host is not MainActivity; navigate directly
            Log.w(TAG, "onLocationClicked: host is not MainActivity, using direct transaction fallback");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container,
                            ConditionsFragment.newInstance()) // no ID; uses selectedLocation
                    .addToBackStack(null)
                    .commit();
        }
    }
}