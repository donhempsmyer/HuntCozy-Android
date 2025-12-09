package dev.donhempsmyer.huntcozy.ui.packing;

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

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.ui.home.HomeViewModel;

import java.util.List;

/**
 * PackingListFragment shows:
 *  - Staged clothing from the recommender
 *  - Loadout items (non-clothing)
 *  - Packed items (checked-off)
 *
 * v2: connect to persistence (Firebase/Room) and saved loadouts.
 */
public class PackingListFragment extends Fragment {

    private static final String TAG = "PackingListFragment";

    public static PackingListFragment newInstance() {
        return new PackingListFragment();
    }

    private PackingListViewModel packingViewModel;
    private HomeViewModel homeViewModel;

    private RecyclerView recyclerStaged;
    private RecyclerView recyclerLoadout;
    private RecyclerView recyclerPacked;

    private PackingListAdapter stagedAdapter;
    private PackingListAdapter loadoutAdapter;
    private PackingListAdapter packedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_packing_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        // Shared HomeViewModel (for recommended gear)
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Local viewmodel for packing UI state
        packingViewModel = new ViewModelProvider(this).get(PackingListViewModel.class);

        bindViews(view);
        setupAdapters();
        observeViewModels();
    }

    private void bindViews(View root) {
        recyclerStaged = root.findViewById(R.id.recycler_packing_staged);
        recyclerLoadout = root.findViewById(R.id.recycler_packing_loadout);
        recyclerPacked = root.findViewById(R.id.recycler_packing_packed);

        recyclerStaged.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerLoadout.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerPacked.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
    }

    private void setupAdapters() {
        stagedAdapter = new PackingListAdapter(PackingListAdapter.Mode.STAGED);
        loadoutAdapter = new PackingListAdapter(PackingListAdapter.Mode.LOADOUT);
        packedAdapter = new PackingListAdapter(PackingListAdapter.Mode.PACKED);

        recyclerStaged.setAdapter(stagedAdapter);
        recyclerLoadout.setAdapter(loadoutAdapter);
        recyclerPacked.setAdapter(packedAdapter);

        // Handle check events
        stagedAdapter.setOnItemCheckedListener((item, checked) -> {
            if (checked) {
                packingViewModel.onStagedItemChecked(item);
            }
        });

        loadoutAdapter.setOnItemCheckedListener((item, checked) -> {
            if (checked) {
                packingViewModel.onLoadoutItemChecked(item);
            }
        });

        packedAdapter.setOnItemCheckedListener((item, checked) -> {
            if (!checked) {
                // Unchecking moves back out of packed
                packingViewModel.onPackedItemUnchecked(item);
            }
        });

        // Optional: future "swap gear" behavior on staged item click
        stagedAdapter.setOnItemClickListener(item -> {
            Log.d(TAG, "onItemClicked staged: " + item);
            // v2: open dialog to pick alternate gear from Closet for this zone/layer.
        });
    }

    private void observeViewModels() {
        // Packings own lists
        packingViewModel.getStagedItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "observe staged: count=" + (items != null ? items.size() : 0));
            stagedAdapter.setItems(items);
        });

        packingViewModel.getLoadoutItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "observe loadout: count=" + (items != null ? items.size() : 0));
            loadoutAdapter.setItems(items);
        });

        packingViewModel.getPackedItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "observe packed: count=" + (items != null ? items.size() : 0));
            packedAdapter.setItems(items);
        });

        // 🔥 NEW: observe recommended gear from HomeViewModel
        homeViewModel.getGear().observe(getViewLifecycleOwner(), recommended -> {
            int count = (recommended != null) ? recommended.size() : 0;
            Log.d(TAG, "PackingListFragment: observed recommended gear count=" + count);
            packingViewModel.setStagedFromRecommendedIfEmpty(recommended);
        });
    }

    /**
     * Seed staged list from current recommended gear the first time
     * the fragment is shown (or whenever recommended gear changed).
     */


    // Alternate approach:
    // - Use Navigation Component, passing a list of gear IDs as fragment arguments,
    //   then resolve them in PackingListFragment via ClosetRepository.
}