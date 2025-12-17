package dev.donhempsmyer.huntcozy.ui.packing;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.data.model.SavedLoadout;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;

public class PackingListViewModel extends ViewModel {

    private static final String TAG = "PackingListViewModel";

    // Recommended clothing from the GearRecommender
    private final MutableLiveData<List<PackingItem>> stagedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Legacy "flat" loadout list (not really used in the new UI, but kept for saved-loadouts plumbing)
    private final MutableLiveData<List<PackingItem>> loadoutItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Items that the user has marked as packed
    private final MutableLiveData<List<PackingItem>> packedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Saved full loadouts (weapon + style + every-hunt + optional)
    private final MutableLiveData<List<SavedLoadout>> savedLoadoutsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Sectioned loadouts
    private final MutableLiveData<List<PackingItem>> weaponLoadoutItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<PackingItem>> styleLoadoutItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<PackingItem>> everyHuntItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<PackingItem>> optionalItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Catalog of all optional candidates
    private final List<PackingItem> optionalCatalog = new ArrayList<>();

    public PackingListViewModel() {
        Log.d(TAG, "constructor");
        seedDefaultLoadout();
    }

    // ---- LiveData getters ---------------------------------------------------

    public LiveData<List<PackingItem>> getStagedItems() {
        return stagedItemsLiveData;
    }

    public LiveData<List<PackingItem>> getPackedItems() {
        return packedItemsLiveData;
    }

    public LiveData<List<PackingItem>> getWeaponLoadoutItems() {
        return weaponLoadoutItemsLiveData;
    }

    public LiveData<List<PackingItem>> getStyleLoadoutItems() {
        return styleLoadoutItemsLiveData;
    }

    public LiveData<List<PackingItem>> getEveryHuntItems() {
        return everyHuntItemsLiveData;
    }

    public LiveData<List<PackingItem>> getOptionalItems() {
        return optionalItemsLiveData;
    }

    // ---- Seed & initialization ----------------------------------------------

    /**
     * Called from PackingListFragment when it first opens, using the current
     * recommended gear from HomeViewModel.
     */
    public void setStagedFromRecommendedIfEmpty(List<GearItem> recommended) {
        List<PackingItem> currentStaged = stagedItemsLiveData.getValue();
        boolean alreadyHasItems = currentStaged != null && !currentStaged.isEmpty();

        int recommendedCount = (recommended != null) ? recommended.size() : 0;
        Log.d(TAG, "setStagedFromRecommendedIfEmpty: alreadyHasItems=" + alreadyHasItems
                + " recommendedCount=" + recommendedCount);

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

        for (int i = 0; i < recommended.size(); i++) {
            GearItem gear = recommended.get(i);
            if (gear == null) {
                Log.w(TAG, "setStagedFromRecommendedIfEmpty: recommended[" + i + "] is null, skipping");
                continue;
            }

            String rawId = gear.getId();
            String label = (gear.getName() != null && !gear.getName().trim().isEmpty())
                    ? gear.getName().trim()
                    : ("Gear #" + i);

            // Make sure id is never null/blank
            String id = (rawId != null && !rawId.trim().isEmpty())
                    ? rawId.trim()
                    : ("gear_" + label.hashCode() + "_" + i);

            Log.d(TAG, "setStagedFromRecommendedIfEmpty: adding staged[" + i + "]"
                    + " id=" + id
                    + " label=" + label
                    + " zone=" + gear.getBodyZone()
                    + " layer=" + gear.getLayerType());

            staged.add(new PackingItem(
                    id,
                    label,
                    gear,
                    PackingItem.Source.STAGED
            ));
        }

        Log.d(TAG, "setStagedFromRecommendedIfEmpty: final staged count=" + staged.size());
        stagedItemsLiveData.setValue(staged);
    }

    /**
     * Seed some optional non-clothing items.
     * v2: this becomes user-configurable / pulled from persistence.
     */
    private void seedDefaultLoadout() {
        optionalCatalog.add(new PackingItem(
                "opt_binos",
                "Binoculars",
                null,
                PackingItem.Source.OPTIONAL
        ));
        optionalCatalog.add(new PackingItem(
                "opt_range",
                "Rangefinder",
                null,
                PackingItem.Source.OPTIONAL
        ));
        optionalCatalog.add(new PackingItem(
                "opt_thermos",
                "Thermos",
                null,
                PackingItem.Source.OPTIONAL
        ));
        optionalCatalog.add(new PackingItem(
                "opt_handwarmers",
                "Hand Warmers",
                null,
                PackingItem.Source.OPTIONAL
        ));
        optionalCatalog.add(new PackingItem(
                "opt_saw",
                "Folding Saw",
                null,
                PackingItem.Source.OPTIONAL
        ));

        // Seed optional list with entire catalog initially
        optionalItemsLiveData.setValue(new ArrayList<>(optionalCatalog));
    }

    // ---- Move between lists -------------------------------------------------

    // STAGED (recommended clothing)
    public void onStagedItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;

        if (checked) {
            // staged -> packed
            moveItem(stagedItemsLiveData, packedItemsLiveData, item, "staged->packed");
        }
    }

    // WEAPON LOADOUT
    public void onWeaponItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;

        if (checked) {
            // weapon -> packed
            moveItem(weaponLoadoutItemsLiveData, packedItemsLiveData, item, "weapon->packed");
        }
    }

    // STYLE LOADOUT
    public void onStyleItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;

        if (checked) {
            // style -> packed
            moveItem(styleLoadoutItemsLiveData, packedItemsLiveData, item, "style->packed");
        }
    }

    // EVERY-HUNT
    public void onEveryHuntItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;

        if (checked) {
            // every-hunt -> packed
            moveItem(everyHuntItemsLiveData, packedItemsLiveData, item, "every->packed");
        }
    }

    // OPTIONAL
    public void onOptionalItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;

        if (checked) {
            // optional -> packed
            moveItem(optionalItemsLiveData, packedItemsLiveData, item, "optional->packed");
        }
    }

    public void onPackedItemUnchecked(PackingItem item) {
        if (item == null) return;

        PackingItem.Source src = item.getSource();
        if (src == null) {
            // Fallback: treat as staged
            Log.w(TAG, "onPackedItemUnchecked: source was null, defaulting to staged");
            moveItem(packedItemsLiveData, stagedItemsLiveData, item, "packed->staged(default)");
            return;
        }

        switch (src) {
            case STAGED:
                moveItem(packedItemsLiveData, stagedItemsLiveData, item, "packed->staged");
                break;

            case WEAPON:
                moveItem(packedItemsLiveData, weaponLoadoutItemsLiveData, item, "packed->weapon");
                break;

            case STYLE:
                moveItem(packedItemsLiveData, styleLoadoutItemsLiveData, item, "packed->style");
                break;

            case EVERY_HUNT:
                moveItem(packedItemsLiveData, everyHuntItemsLiveData, item, "packed->every");
                break;

            case OPTIONAL:
                moveItem(packedItemsLiveData, optionalItemsLiveData, item, "packed->optional");
                break;
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

        boolean removed = from.removeIf(i ->
                i != null &&
                        i.getId() != null &&
                        i.getId().equals(item.getId())
        );
        if (!removed) {
            Log.w(TAG, "moveItem: item not found in 'from' list direction=" + directionTag);
        } else {
            to.add(item);
            Log.d(TAG, "moveItem: " + directionTag + " id=" + item.getId());
        }

        fromLive.setValue(from);
        toLive.setValue(to);
    }

    // ---- Edit text helpers for per-section dialogs -------------------------

    public String getWeaponLoadoutText() {
        return toMultiline(weaponLoadoutItemsLiveData.getValue());
    }

    public void setWeaponLoadoutFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.WEAPON);
        weaponLoadoutItemsLiveData.setValue(items);
    }

    public String getStyleLoadoutText() {
        return toMultiline(styleLoadoutItemsLiveData.getValue());
    }

    public void setStyleLoadoutFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.STYLE);
        styleLoadoutItemsLiveData.setValue(items);
    }

    public String getEveryHuntText() {
        return toMultiline(everyHuntItemsLiveData.getValue());
    }

    public void setEveryHuntFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.EVERY_HUNT);
        everyHuntItemsLiveData.setValue(items);
    }

    // ---- Saved loadouts (full snapshot: weapon + style + every + optional) --

    public void saveCurrentLoadoutAs(String name) {
        if (name == null) name = "";
        name = name.trim();
        if (name.isEmpty()) {
            Log.w(TAG, "saveCurrentLoadoutAs: empty name, ignoring");
            return;
        }

        // Combine all non-staged sections as the "loadout"
        List<PackingItem> current = new ArrayList<>();
        addAllSafe(current, weaponLoadoutItemsLiveData.getValue());
        addAllSafe(current, styleLoadoutItemsLiveData.getValue());
        addAllSafe(current, everyHuntItemsLiveData.getValue());
        addAllSafe(current, optionalItemsLiveData.getValue());

        // Deep-ish copy to decouple saved snapshot from live list
        List<PackingItem> copy = new ArrayList<>();
        for (PackingItem item : current) {
            if (item == null) continue;
            copy.add(new PackingItem(
                    item.getId(),
                    item.getLabel(),
                    item.getGearItem(),
                    item.getSource()
            ));
        }

        List<SavedLoadout> existing = savedLoadoutsLiveData.getValue();
        if (existing == null) {
            existing = new ArrayList<>();
        } else {
            existing = new ArrayList<>(existing); // copy to avoid mutating same list
        }

        String id = "loadout_" + System.currentTimeMillis();
        SavedLoadout loadout = new SavedLoadout(id, name, copy);

        Log.d(TAG, "saveCurrentLoadoutAs: name=" + name + " items=" + copy.size());
        existing.add(loadout);
        savedLoadoutsLiveData.setValue(existing);

        // Optional: keep a flattened view for legacy UI
        loadoutItemsLiveData.setValue(copy);
    }

    public void applyLoadout(String loadoutId) {
        if (loadoutId == null) return;
        List<SavedLoadout> saved = savedLoadoutsLiveData.getValue();
        if (saved == null || saved.isEmpty()) {
            Log.w(TAG, "applyLoadout: no saved loadouts");
            return;
        }

        SavedLoadout target = null;
        for (SavedLoadout lo : saved) {
            if (lo != null && loadoutId.equals(lo.getId())) {
                target = lo;
                break;
            }
        }

        if (target == null) {
            Log.w(TAG, "applyLoadout: id not found " + loadoutId);
            return;
        }

        // Split items back into sections by Source
        List<PackingItem> weapon = new ArrayList<>();
        List<PackingItem> style = new ArrayList<>();
        List<PackingItem> every = new ArrayList<>();
        List<PackingItem> optional = new ArrayList<>();

        if (target.getItems() != null) {
            for (PackingItem item : target.getItems()) {
                if (item == null) continue;

                PackingItem copy = new PackingItem(
                        item.getId(),
                        item.getLabel(),
                        item.getGearItem(),
                        item.getSource()
                );

                PackingItem.Source src = copy.getSource();
                if (src == null) src = PackingItem.Source.EVERY_HUNT;

                switch (src) {
                    case WEAPON:
                        weapon.add(copy);
                        break;
                    case STYLE:
                        style.add(copy);
                        break;
                    case OPTIONAL:
                        optional.add(copy);
                        break;
                    case STAGED:
                        // Typically staged is not part of saved loadout;
                        // if it is, we could push to staged or every-hunt. For now ignore.
                        break;
                    case EVERY_HUNT:
                    default:
                        every.add(copy);
                        break;
                }
            }
        }

        Log.d(TAG, "applyLoadout: id=" + target.getId()
                + " name=" + target.getName()
                + " weapon=" + weapon.size()
                + ", style=" + style.size()
                + ", every=" + every.size()
                + ", optional=" + optional.size());

        weaponLoadoutItemsLiveData.setValue(weapon);
        styleLoadoutItemsLiveData.setValue(style);
        everyHuntItemsLiveData.setValue(every);
        optionalItemsLiveData.setValue(optional);

        // Optional: flattened view
        List<PackingItem> flat = new ArrayList<>();
        flat.addAll(weapon);
        flat.addAll(style);
        flat.addAll(every);
        flat.addAll(optional);
        loadoutItemsLiveData.setValue(flat);
    }

    // ---- Unpack All ---------------------------------------------------------

    public void unpackAll() {
        List<PackingItem> packed = packedItemsLiveData.getValue();
        if (packed == null || packed.isEmpty()) {
            Log.d(TAG, "unpackAll: nothing packed");
            return;
        }

        // Snapshot to avoid concurrent modification while moveItem() mutates LiveData
        List<PackingItem> snapshot = new ArrayList<>(packed);
        for (PackingItem item : snapshot) {
            onPackedItemUnchecked(item);
        }

        Log.d(TAG, "unpackAll: moved " + snapshot.size()
                + " items back to their source lists");
    }

    // ---- Context from Home (weapon/style) -----------------------------------

    public void setContextFromHome(WeaponType weapon, HuntingStyle style) {
        Log.d(TAG, "setContextFromHome: weapon=" + weapon + " style=" + style);

        // v1: just ensure lists exist. v2: can load weapon/style-specific templates.
        if (weaponLoadoutItemsLiveData.getValue() == null) {
            weaponLoadoutItemsLiveData.setValue(new ArrayList<>());
        }
        if (styleLoadoutItemsLiveData.getValue() == null) {
            styleLoadoutItemsLiveData.setValue(new ArrayList<>());
        }
    }

    public void setStagedFromHome(List<GearItem> recommended) {
        if (recommended == null || recommended.isEmpty()) {
            Log.d(TAG, "setStagedFromHome: recommended empty or null");
            stagedItemsLiveData.setValue(new ArrayList<>());
            return;
        }

        List<PackingItem> staged = new ArrayList<>();
        for (GearItem gear : recommended) {
            if (gear == null) continue;
            String id = gear.getId();
            String label = gear.getName();

            if (id == null){
                Log.d(TAG, "setStagedFromHome: " + label + " has no id, skipping");
                continue;
            }

            Log.d(TAG, "setStagedFromHome: adding staged item id=" + id
                    + " label=" + label
                    + " zone=" + gear.getBodyZone()
                    + " layer=" + gear.getLayerType());
            staged.add(new PackingItem(
                    id,
                    label,
                    gear,
                    PackingItem.Source.STAGED
            ));
        }

        Log.d(TAG, "setStagedFromHome: staged count=" + staged.size());
        stagedItemsLiveData.setValue(staged);
    }

    // ---- Optional item helpers ----------------------------------------------

    public List<PackingItem> getAvailableOptionalCandidates() {
        // Gather all items currently in the entire packing list
        List<PackingItem> inUse = new ArrayList<>();
        addAllSafe(inUse, stagedItemsLiveData.getValue());
        addAllSafe(inUse, weaponLoadoutItemsLiveData.getValue());
        addAllSafe(inUse, styleLoadoutItemsLiveData.getValue());
        addAllSafe(inUse, everyHuntItemsLiveData.getValue());
        addAllSafe(inUse, optionalItemsLiveData.getValue());
        addAllSafe(inUse, packedItemsLiveData.getValue());

        List<PackingItem> result = new ArrayList<>();
        for (PackingItem candidate : optionalCatalog) {
            boolean exists = false;
            for (PackingItem used : inUse) {
                if (used != null &&
                        used.getId() != null &&
                        used.getId().equals(candidate.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(candidate);
            }
        }
        return result;
    }

    private void addAllSafe(List<PackingItem> dest, List<PackingItem> src) {
        if (src == null) return;
        dest.addAll(src);
    }

    public void addOptionalItems(List<PackingItem> newItems) {
        List<PackingItem> current = optionalItemsLiveData.getValue();
        if (current == null) current = new ArrayList<>();
        else current = new ArrayList<>(current);

        current.addAll(newItems);
        optionalItemsLiveData.setValue(current);
    }

    // ---- Text <-> items helpers ---------------------------------------------

    private String toMultiline(List<PackingItem> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PackingItem item : items) {
            if (item == null || item.getLabel() == null) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(item.getLabel());
        }
        return sb.toString();
    }

    private List<PackingItem> fromMultiline(String text, PackingItem.Source source) {
        List<PackingItem> list = new ArrayList<>();
        if (text == null) return list;

        String[] lines = text.split("\\r?\\n");
        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            // gearItem = null means non-clothing
            list.add(new PackingItem(
                    "custom_" + trimmed.hashCode(),
                    trimmed,
                    null,
                    source
            ));
        }
        return list;
    }

    // Alternate approach:
    // - Keep a single master list with a 'state' enum: STAGED / WEAPON / STYLE / EVERY / OPTIONAL / PACKED,
    //   and expose filtered LiveData via Transformations.map().
}