package dev.donhempsmyer.huntcozy.ui.packing;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;

public class PackingListViewModel extends ViewModel {

    private static final String TAG = "PackingListViewModel";

    private final MutableLiveData<List<PackingItem>> stagedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<PackingItem>> loadoutItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<PackingItem>> packedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public PackingListViewModel() {
        Log.d(TAG, "constructor");
        seedDefaultLoadout();
    }

    public LiveData<List<PackingItem>> getStagedItems() {
        return stagedItemsLiveData;
    }

    public LiveData<List<PackingItem>> getLoadoutItems() {
        return loadoutItemsLiveData;
    }

    public LiveData<List<PackingItem>> getPackedItems() {
        return packedItemsLiveData;
    }

    /**
     * Called from PackingListFragment when it first opens, using the current
     * recommended gear from HomeViewModel.
     */
    public void setStagedFromRecommendedIfEmpty(List<GearItem> recommended) {
        List<PackingItem> currentStaged = stagedItemsLiveData.getValue();
        boolean alreadyHasItems = currentStaged != null && !currentStaged.isEmpty();
        if (alreadyHasItems) {
            Log.d(TAG, "setStagedFromRecommendedIfEmpty: staged not empty, skipping seed");
            return;
        }

        if (recommended == null || recommended.isEmpty()) {
            Log.d(TAG, "setStagedFromRecommendedIfEmpty: recommended empty or null");
            stagedItemsLiveData.setValue(new ArrayList<>());
            return;
        }

        List<PackingItem> staged = new ArrayList<>();
        for (GearItem gear : recommended) {
            if (gear == null) continue;
            String id = gear.getId(); // adjust if your GearItem uses another identifier
            String label = gear.getName();
            staged.add(new PackingItem(id, label, gear));
        }

        Log.d(TAG, "setStagedFromRecommendedIfEmpty: staged count=" + staged.size());
        stagedItemsLiveData.setValue(staged);
    }

    /**
     * Seed some non-clothing loadout items for now.
     * v2: this becomes user-configurable saved loadouts.
     */
    private void seedDefaultLoadout() {
        List<PackingItem> loadout = new ArrayList<>();
        loadout.add(new PackingItem("10001L", "Rangefinder", null));
        loadout.add(new PackingItem("10002L", "Knife", null));
        loadout.add(new PackingItem("10003L", "Headlamp", null));
        loadout.add(new PackingItem("10004L", "Tag / License", null));
        loadout.add(new PackingItem("10005L", "Binoculars", null));

        loadoutItemsLiveData.setValue(loadout);
    }

    // ---- Move between lists -------------------------------------------------

    public void onStagedItemChecked(PackingItem item) {
        moveItem(stagedItemsLiveData, packedItemsLiveData, item, "staged->packed");
    }

    public void onLoadoutItemChecked(PackingItem item) {
        moveItem(loadoutItemsLiveData, packedItemsLiveData, item, "loadout->packed");
    }

    public void onPackedItemUnchecked(PackingItem item) {
        // For now, send it back to staged if it was originally a gear item,
        // or back to loadout if it was generic.
        if (item.getGearItem() != null) {
            moveItem(packedItemsLiveData, stagedItemsLiveData, item, "packed->staged");
        } else {
            moveItem(packedItemsLiveData, loadoutItemsLiveData, item, "packed->loadout");
        }
    }

    private void moveItem(MutableLiveData<List<PackingItem>> fromLive,
                          MutableLiveData<List<PackingItem>> toLive,
                          PackingItem item,
                          String directionTag) {
        if (item == null) return;

        List<PackingItem> from = fromLive.getValue();
        List<PackingItem> to = toLive.getValue();
        if (from == null) from = new ArrayList<>();
        if (to == null) to = new ArrayList<>();

        from = new ArrayList<>(from);
        to = new ArrayList<>(to);

        boolean removed = from.removeIf(i -> i.getId() == item.getId());
        if (!removed) {
            Log.w(TAG, "moveItem: item not found in 'from' list direction=" + directionTag);
        } else {
            to.add(item);
            Log.d(TAG, "moveItem: " + directionTag + " id=" + item.getId());
        }

        fromLive.setValue(from);
        toLive.setValue(to);
    }

    // Alternate approach:
    // - Keep a single master list with a 'state' enum: STAGED / LOADOUT / PACKED,
    //   and expose filtered LiveData via Transformations.map().
}