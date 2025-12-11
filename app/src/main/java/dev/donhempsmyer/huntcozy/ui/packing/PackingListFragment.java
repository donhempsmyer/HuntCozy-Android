package dev.donhempsmyer.huntcozy.ui.packing;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.ui.home.HomeViewModel;

/**
 * PackingListFragment shows:
 *  - Staged clothing from the recommender
 *  - Loadout items (weapon, style, every-hunt, optional)
 *  - Packed items (checked-off)
 *
 * v2: connect to persistence (Firebase/Room) and richer loadout management.
 */
public class PackingListFragment extends Fragment {

    private static final String TAG = "PackingListFragment";

    public static PackingListFragment newInstance() {
        return new PackingListFragment();
    }

    private PackingListViewModel packingViewModel;
    private HomeViewModel homeViewModel;

    // RecyclerViews
    private RecyclerView recyclerStaged;
    private RecyclerView recyclerWeapon;
    private RecyclerView recyclerStyle;
    private RecyclerView recyclerEvery;
    private RecyclerView recyclerOptional;
    private RecyclerView recyclerPacked;

    // Adapters
    private PackingListAdapter stagedAdapter;
    private PackingListAdapter weaponAdapter;
    private PackingListAdapter styleAdapter;
    private PackingListAdapter everyAdapter;
    private PackingListAdapter optionalAdapter;
    private PackingListAdapter packedAdapter;

    // Loadout section headers / buttons
    private TextView textWeaponSubtitle;
    private TextView textStyleSubtitle;

    private Button buttonEditWeapon;
    private Button buttonEditStyle;
    private Button buttonEditEvery;
    private Button buttonEditOptional;
    private Button buttonUnpackAll;

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

        // Shared HomeViewModel (for recommended gear, weapon, style)
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Local ViewModel for packing UI state
        packingViewModel = new ViewModelProvider(requireActivity()).get(PackingListViewModel.class);

        bindViews(view);
        setupAdapters();
        setupButtons();
        observeViewModels();
    }

    private void bindViews(View root) {
        // Subtitles
        textWeaponSubtitle = root.findViewById(R.id.text_packing_weapon_subtitle);
        textStyleSubtitle = root.findViewById(R.id.text_packing_style_subtitle);

        // Recyclers
        recyclerStaged = root.findViewById(R.id.recycler_packing_staged);
        recyclerWeapon = root.findViewById(R.id.recycler_packing_weapon);
        recyclerStyle = root.findViewById(R.id.recycler_packing_style);
        recyclerEvery = root.findViewById(R.id.recycler_packing_every);
        recyclerOptional = root.findViewById(R.id.recycler_packing_optional);
        recyclerPacked = root.findViewById(R.id.recycler_packing_packed);

        // Buttons
        buttonEditWeapon = root.findViewById(R.id.button_packing_edit_weapon);
        buttonEditStyle = root.findViewById(R.id.button_packing_edit_style);
        buttonEditEvery = root.findViewById(R.id.button_packing_edit_every);
        buttonEditOptional = root.findViewById(R.id.button_packing_edit_optional);
        buttonUnpackAll = root.findViewById(R.id.button_packing_unpack_all);

        // Layout managers
        recyclerStaged.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerWeapon.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerStyle.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerEvery.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerOptional.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerPacked.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
    }

    private void setupAdapters() {
        // Staged clothing (from recommender)
        stagedAdapter = new PackingListAdapter(PackingListAdapter.Mode.STAGED);
        recyclerStaged.setAdapter(stagedAdapter);
        stagedAdapter.setOnItemCheckedListener((item, checked) -> {
            // When staged item is checked, move it to PACKED (via ViewModel)
            if (packingViewModel != null) {
                Log.d(TAG, "stagedAdapter: onItemChecked item=" + item.getLabel() + " checked=" + checked);
                packingViewModel.onStagedItemChecked(item, checked);
            }
        });

        // Weapon loadout
        weaponAdapter = new PackingListAdapter(PackingListAdapter.Mode.LOADOUT);
        recyclerWeapon.setAdapter(weaponAdapter);
        weaponAdapter.setOnItemCheckedListener((item, checked) -> {
            if (packingViewModel != null) {
                Log.d(TAG, "weaponAdapter: onItemChecked item=" + item.getLabel() + " checked=" + checked);
                packingViewModel.onWeaponItemChecked(item, checked);
            }
        });

        // Style loadout
        styleAdapter = new PackingListAdapter(PackingListAdapter.Mode.LOADOUT);
        recyclerStyle.setAdapter(styleAdapter);
        styleAdapter.setOnItemCheckedListener((item, checked) -> {
            if (packingViewModel != null) {
                Log.d(TAG, "styleAdapter: onItemChecked item=" + item.getLabel() + " checked=" + checked);
                packingViewModel.onStyleItemChecked(item, checked);
            }
        });

        // Every-hunt items
        everyAdapter = new PackingListAdapter(PackingListAdapter.Mode.LOADOUT);
        recyclerEvery.setAdapter(everyAdapter);
        everyAdapter.setOnItemCheckedListener((item, checked) -> {
            if (packingViewModel != null) {
                Log.d(TAG, "everyAdapter: onItemChecked item=" + item.getLabel() + " checked=" + checked);
                packingViewModel.onEveryHuntItemChecked(item, checked);
            }
        });

        // Optional items
        optionalAdapter = new PackingListAdapter(PackingListAdapter.Mode.LOADOUT);
        recyclerOptional.setAdapter(optionalAdapter);
        optionalAdapter.setOnItemCheckedListener((item, checked) -> {
            if (packingViewModel != null) {
                Log.d(TAG, "optionalAdapter: onItemChecked item=" + item.getLabel() + " checked=" + checked);
                packingViewModel.onOptionalItemChecked(item, checked);
            }
        });

        // Packed items (read-only checkboxes, still visually checked)
        packedAdapter = new PackingListAdapter(PackingListAdapter.Mode.PACKED);
        recyclerPacked.setAdapter(packedAdapter);

        packedAdapter.setOnItemCheckedListener((item, checked) -> {
            // For packed list, we care about UN-check (moving back)
            if (!checked && packingViewModel != null) {
                packingViewModel.onPackedItemUnchecked(item);
            }
        });

    }

    private void setupButtons() {
        // Weapon loadout edit (multiline)
        buttonEditWeapon.setOnClickListener(v -> {
            String initial = packingViewModel.getWeaponLoadoutText();
            showMultilineEditDialog(
                    "Edit Weapon Loadout",
                    initial,
                    text -> packingViewModel.setWeaponLoadoutFromText(text)
            );
        });

        // Style loadout edit (multiline)
        buttonEditStyle.setOnClickListener(v -> {
            String initial = packingViewModel.getStyleLoadoutText();
            showMultilineEditDialog(
                    "Edit Style Loadout",
                    initial,
                    text -> packingViewModel.setStyleLoadoutFromText(text)
            );
        });

        // Every-hunt edit (multiline)
        buttonEditEvery.setOnClickListener(v -> {
            String initial = packingViewModel.getEveryHuntText();
            showMultilineEditDialog(
                    "Edit Every-Hunt Items",
                    initial,
                    text -> packingViewModel.setEveryHuntFromText(text)
            );
        });

        // Optional items: multi-choice from candidates not already used
        buttonEditOptional.setOnClickListener(v -> {
            List<PackingItem> candidates = packingViewModel.getAvailableOptionalCandidates();
            if (candidates == null || candidates.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setMessage("No additional optional items available.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            CharSequence[] labels = new CharSequence[candidates.size()];
            final boolean[] checked = new boolean[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                labels[i] = candidates.get(i).getLabel();
                checked[i] = false;
            }

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Add Optional Items")
                    .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                        checked[which] = isChecked;
                    })
                    .setPositiveButton("Add", (dialog, which) -> {
                        List<PackingItem> selected = new ArrayList<>();
                        for (int i = 0; i < candidates.size(); i++) {
                            if (checked[i]) {
                                selected.add(candidates.get(i));
                            }
                        }
                        packingViewModel.addOptionalItems(selected);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Unpack All with confirmation
        buttonUnpackAll.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Unpack All")
                    .setMessage("Are you sure you want to move all packed items back into the lists?")
                    .setPositiveButton("Yes", (dialog, which) -> packingViewModel.unpackAll())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void observeViewModels() {
        // Packing lists
        packingViewModel.getStagedItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "observe staged: count=" + (items != null ? items.size() : 0));
            stagedAdapter.setItems(items);
        });

        packingViewModel.getWeaponLoadoutItems().observe(getViewLifecycleOwner(), items -> {
            weaponAdapter.setItems(items);
        });

        packingViewModel.getStyleLoadoutItems().observe(getViewLifecycleOwner(), items -> {
            styleAdapter.setItems(items);
        });

        packingViewModel.getEveryHuntItems().observe(getViewLifecycleOwner(), items -> {
            everyAdapter.setItems(items);
        });

        packingViewModel.getOptionalItems().observe(getViewLifecycleOwner(), items -> {
            optionalAdapter.setItems(items);
        });

        packingViewModel.getPackedItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "observe packed: count=" + (items != null ? items.size() : 0));
            packedAdapter.setItems(items);
        });

        // Recommended clothing from Home → staged list (if empty)
        homeViewModel.getGear().observe(getViewLifecycleOwner(), recommended -> {
            int count = (recommended != null) ? recommended.size() : 0;
            Log.d(TAG, "PackingListFragment: observed recommended gear count=" + count);
            packingViewModel.setStagedFromRecommendedIfEmpty(recommended);
        });

        // Weapon & style subtitles under section headers
        homeViewModel.getWeaponType().observe(getViewLifecycleOwner(), type -> {
            if (type != null) {
                textWeaponSubtitle.setText("(" + type.name() + ")");
            } else {
                textWeaponSubtitle.setText("");
            }
        });

        homeViewModel.getHuntingStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                textStyleSubtitle.setText("(" + style.name() + ")");
            } else {
                textStyleSubtitle.setText("");
            }
        });
    }

    // ===== Dialog helpers =====

    private interface TextSaveCallback {
        void onSave(String text);
    }

    private void showMultilineEditDialog(String title,
                                         String initialText,
                                         TextSaveCallback callback) {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("One item per line");
        input.setMinLines(4);
        input.setText(initialText);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText() != null ? input.getText().toString() : "";
                    callback.onSave(text);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Alternate approach:
    // - Use Navigation Component with Safe Args to pass weapon/style context.
    // - Persist loadout definitions via Room and let the ViewModel query them.
}