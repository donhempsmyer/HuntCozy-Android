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

import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;
import dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment;
import dev.donhempsmyer.huntcozy.ui.locations.AddLocationDialogFragment;
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
            // v1: open a simple dialog to capture name/lat/lon
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
        Log.d(TAG, "onLocationClicked: " + location);

        // 1) Mark this as the selected location in the shared repository
        LocationsRepository.getInstance().selectLocation(location);

        // 2) Ask MainActivity to switch to the Conditions tab
        if (requireActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) requireActivity();
            activity.openConditionsTab();   // this calls bottomNav.setSelectedItemId(R.id.nav_conditions)
        } else {
            // Fallback: if for some reason this fragment isn't hosted by MainActivity
            Log.w(TAG, "onLocationClicked: host is not MainActivity, using direct transaction fallback");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container,
                            ConditionsFragment.newInstance(location.getId()))
                    .addToBackStack(null)
                    .commit();
        }
    }
}