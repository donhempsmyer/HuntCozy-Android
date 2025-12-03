package dev.donhempsmyer.huntcozy.ui.closet;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * ClosetFragment shows the user's gear closet.
 * v1: read-only list seeded by ClosetSeeder.
 * v2: add/edit gear and persist via Firebase/Room.
 */
public class ClosetFragment extends Fragment {

    private static final String TAG = "ClosetFragment";

    public static ClosetFragment newInstance() {
        return new ClosetFragment();
    }

    private ClosetViewModel viewModel;
    private ClosetAdapter adapter;

    private RecyclerView recyclerCloset;
    private TextView textEmptyCloset;
    private FloatingActionButton fabAddGear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_closet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        viewModel = new ViewModelProvider(this).get(ClosetViewModel.class);

        bindViews(view);
        setupRecycler();
        observeViewModel();
        setupFab();
    }

    private void bindViews(View root) {
        recyclerCloset = root.findViewById(R.id.recycler_closet);
        textEmptyCloset = root.findViewById(R.id.text_empty_closet);
        fabAddGear = root.findViewById(R.id.fab_add_gear);
    }

    private void setupRecycler() {
        adapter = new ClosetAdapter();
        adapter.setOnGearClickListener(this::onGearClicked);

        recyclerCloset.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerCloset.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getGear().observe(getViewLifecycleOwner(), gearItems -> {
            int count = gearItems != null ? gearItems.size() : 0;
            Log.d(TAG, "observeViewModel: closet gear count=" + count);
            adapter.setItems(gearItems);

            if (count == 0) {
                textEmptyCloset.setVisibility(View.VISIBLE);
                recyclerCloset.setVisibility(View.GONE);
            } else {
                textEmptyCloset.setVisibility(View.GONE);
                recyclerCloset.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupFab() {
        fabAddGear.setOnClickListener(v -> {
            Log.d(TAG, "onClick: fabAddGear (Add Gear stub)");
            // v1: just log. v2: navigate to AddGearFragment / dialog.
        });
    }

    private void onGearClicked(GearItem item) {
        Log.d(TAG, "onGearClicked: " + item.getName());
        // v1: no-op or maybe show a Toast.
        // v2: navigate to GearDetailFragment for editing, etc.
    }

    // Alternate approach:
    // - Use a shared ViewModel with HomeFragment so Home can consume this closet
    //   for recommendations directly (no reseeding or duplication).
}