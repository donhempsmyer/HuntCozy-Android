package dev.donhempsmyer.huntcozy.ui.packing;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.data.model.SavedLoadout;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.remote.UserFirestore;

public class PackingListViewModel extends ViewModel {

    private static final String TAG = "PackingListViewModel";

    // Firestore user context
    private final UserFirestore userFs;

    // Track current context so we only react on real changes
    private WeaponType currentWeapon = null;
    private HuntingStyle currentStyle = null;

    // Session doc id for current packing state
    private static final String SESSION_DOC_ID = "session_current";

    // Session restore state
    private boolean sessionRestoreRequested = false;
    private boolean sessionRestored = false; // true only if a session doc existed


    // Recommended clothing from the GearRecommender
    private final MutableLiveData<List<PackingItem>> stagedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Legacy "flat" loadout list (used only as a flattened view when applying/saving)
    private final MutableLiveData<List<PackingItem>> loadoutItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Items that the user has marked as packed
    private final MutableLiveData<List<PackingItem>> packedItemsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    // Saved full loadouts (weapon + style + every-hunt + optional) – in-memory only for now
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

    // Optional catalog: dynamic; built from optional items and edits (no seeding)
    private final List<PackingItem> optionalCatalog = new ArrayList<>();

    public PackingListViewModel() {
        Log.d(TAG, "constructor");
        userFs = UserFirestore.fromCurrentUser();
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

    // ---- RESTORE session from Firestore ------------------------------------

    /**
     * Restore packing session (staged/every/optional/packed) from Firestore if we
     * haven't already tried. Safe to call multiple times; work is only done once per VM.
     */
    public void restoreStateIfNeeded() {
        if (sessionRestoreRequested) {
            Log.d(TAG, "restoreStateIfNeeded: already requested, skipping");
            return;
        }
        sessionRestoreRequested = true;

        DocumentReference docRef = userFs
                .packingStateCollection()
                .document(SESSION_DOC_ID);

        Log.d(TAG, "restoreStateIfNeeded: fetching packingState/" + SESSION_DOC_ID);

        docRef.get()
                .addOnSuccessListener(this::applySessionSnapshot)
                .addOnFailureListener(e ->
                        Log.e(TAG, "restoreStateIfNeeded: failed to load packing session", e));
    }

    private void applySessionSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            Log.d(TAG, "applySessionSnapshot: no existing packing session; starting fresh");
            sessionRestored = false;
            return;
        }

        sessionRestored = true;

        Object stagedRaw = snapshot.get("staged");
        Object everyRaw = snapshot.get("every");
        Object optionalRaw = snapshot.get("optional");
        Object packedRaw = snapshot.get("packed");

        List<PackingItem> staged = fromStateList(stagedRaw, PackingItem.Source.STAGED);
        List<PackingItem> every = fromStateList(everyRaw, PackingItem.Source.EVERY_HUNT);
        List<PackingItem> optional = fromStateList(optionalRaw, PackingItem.Source.OPTIONAL);
        List<PackingItem> packed = fromStateList(packedRaw, PackingItem.Source.STAGED);

        Log.d(TAG, "applySessionSnapshot: restored"
                + " staged=" + staged.size()
                + " every=" + every.size()
                + " optional=" + optional.size()
                + " packed=" + packed.size());

        stagedItemsLiveData.setValue(staged);
        everyHuntItemsLiveData.setValue(every);
        optionalItemsLiveData.setValue(optional);
        packedItemsLiveData.setValue(packed);

        rebuildOptionalCatalog(optional);
    }

    private List<PackingItem> fromStateList(Object raw, PackingItem.Source defaultSource) {
        List<PackingItem> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }

        List<?> rawList = (List<?>) raw;

        for (Object entry : rawList) {
            if (!(entry instanceof Map)) {
                continue;
            }

            Map<?, ?> map = (Map<?, ?>) entry;

            Object idObj = map.get("id");
            Object labelObj = map.get("label");
            Object sourceObj = map.get("source");

            String id = (idObj != null) ? idObj.toString() : null;
            String label = (labelObj != null) ? labelObj.toString() : null;

            PackingItem.Source src = defaultSource;
            if (sourceObj != null) {
                try {
                    src = PackingItem.Source.valueOf(sourceObj.toString());
                } catch (IllegalArgumentException ignored) {
                    // fall back to default
                }
            }

            if (label == null || label.trim().isEmpty()) {
                if (id == null) continue;
                label = id;
            }
            if (id == null) {
                id = "auto_" + label.hashCode();
            }

            result.add(new PackingItem(
                    id,
                    label,
                    null,   // we don't rehydrate GearItem here; label is enough for UI
                    src
            ));
        }
        return result;
    }

    // ---- SAVE session to Firestore -----------------------------------------

    /**
     * Persist the current packing session to Firestore.
     * Called from Fragment.onPause() and after major state changes.
     */
    public void saveCurrentState() {
        DocumentReference docRef = userFs
                .packingStateCollection()
                .document(SESSION_DOC_ID);

        Map<String, Object> data = new HashMap<>();
        data.put("staged", toStateList(stagedItemsLiveData.getValue(), PackingItem.Source.STAGED));
        data.put("every", toStateList(everyHuntItemsLiveData.getValue(), PackingItem.Source.EVERY_HUNT));
        data.put("optional", toStateList(optionalItemsLiveData.getValue(), PackingItem.Source.OPTIONAL));
        data.put("packed", toStateList(packedItemsLiveData.getValue(), PackingItem.Source.STAGED));
        data.put("lastUpdated", FieldValue.serverTimestamp());

        Log.d(TAG, "saveCurrentState: writing packingState/" + SESSION_DOC_ID + everyHuntItemsLiveData.getValue());

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "saveCurrentState: success"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveCurrentState: failure", e));
    }

    private List<Map<String, Object>> toStateList(List<PackingItem> items,
                                                  PackingItem.Source defaultSource) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (items == null) return out;

        for (PackingItem item : items) {
            if (item == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("label", item.getLabel());
            PackingItem.Source src = item.getSource() != null ? item.getSource() : defaultSource;
            map.put("source", src.name());
            out.add(map);
        }
        return out;
    }

    // ---- Seed & initialization ----------------------------------------------

    /**
     * Called from PackingListFragment when it first opens, using the current
     * recommended gear from HomeViewModel.
     * Only seeds if the staged list is currently empty, so restored sessions win.
     */
    public void setStagedFromRecommendedIfEmpty(List<GearItem> recommended) {
        // Only treat staged + packed as "session state"
        List<PackingItem> staged = stagedItemsLiveData.getValue();
        List<PackingItem> packed = packedItemsLiveData.getValue();

        boolean hasSessionItems =
                (staged != null && !staged.isEmpty()) ||
                        (packed != null && !packed.isEmpty());

        Log.d(TAG, "setStagedFromRecommendedIfEmpty: " +
                "staged=" + (staged != null ? staged.size() : 0) +
                " packed=" + (packed != null ? packed.size() : 0));

        // If there is any active session state (either staged or packed),
        // do NOT auto-seed from the recommender.
        if (hasSessionItems) {
            Log.d(TAG, "setStagedFromRecommendedIfEmpty: existing packing session detected," +
                    " skipping auto-seed from recommender");
            return;
        }

        int recommendedCount = (recommended != null) ? recommended.size() : 0;
        Log.d(TAG, "setStagedFromRecommendedIfEmpty: no existing session, recommendedCount=" + recommendedCount);

        if (recommended == null || recommended.isEmpty()) {
            Log.d(TAG, "setStagedFromRecommendedIfEmpty: recommended empty or null");
            stagedItemsLiveData.setValue(new ArrayList<>());
            // IMPORTANT: DO NOT CALL saveCurrentState() HERE
            return;
        }

        List<PackingItem> stagedList = new ArrayList<>();

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

            String id = (rawId != null && !rawId.trim().isEmpty())
                    ? rawId.trim()
                    : ("gear_" + label.hashCode() + "_" + i);

            Log.d(TAG, "setStagedFromRecommendedIfEmpty: adding staged[" + i + "]"
                    + " id=" + id
                    + " label=" + label
                    + " zone=" + gear.getBodyZone()
                    + " layer=" + gear.getLayerType());

            stagedList.add(new PackingItem(
                    id,
                    label,
                    gear,
                    PackingItem.Source.STAGED
            ));
        }

        Log.d(TAG, "setStagedFromRecommendedIfEmpty: final staged count=" + stagedList.size());
        stagedItemsLiveData.setValue(stagedList);

    }

    // ---- Move between lists (session state) --------------------------------

    public void onStagedItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;
        if (checked) {
            moveItem(stagedItemsLiveData, packedItemsLiveData, item, "staged->packed");
        }
    }

    public void onWeaponItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;
        if (checked) {
            moveItem(weaponLoadoutItemsLiveData, packedItemsLiveData, item, "weapon->packed");
        }
    }

    public void onStyleItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;
        if (checked) {
            moveItem(styleLoadoutItemsLiveData, packedItemsLiveData, item, "style->packed");
        }
    }

    public void onEveryHuntItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;
        if (checked) {
            moveItem(everyHuntItemsLiveData, packedItemsLiveData, item, "every->packed");
        }
    }

    public void onOptionalItemChecked(PackingItem item, boolean checked) {
        if (item == null) return;
        if (checked) {
            moveItem(optionalItemsLiveData, packedItemsLiveData, item, "optional->packed");
        }
    }

    public void onPackedItemUnchecked(PackingItem item) {
        if (item == null) return;

        PackingItem.Source src = item.getSource();
        if (src == null) {
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

        // Any move changes the current packing session
        saveCurrentState();
    }

    // ---- Edit text helpers for per-section dialogs -------------------------

    public String getWeaponLoadoutText() {
        return toMultiline(weaponLoadoutItemsLiveData.getValue());
    }

    public void setWeaponLoadoutFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.WEAPON);
        weaponLoadoutItemsLiveData.setValue(items);
        saveWeaponLoadoutForCurrentWeapon();
    }

    public String getStyleLoadoutText() {
        return toMultiline(styleLoadoutItemsLiveData.getValue());
    }

    public void setStyleLoadoutFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.STYLE);
        styleLoadoutItemsLiveData.setValue(items);
        saveStyleLoadoutForCurrentStyle();
    }

    public String getEveryHuntText() {
        return toMultiline(everyHuntItemsLiveData.getValue());
    }

    public void setEveryHuntFromText(String text) {
        Log.d(TAG, "setEveryHuntFromText: text=\n" + text);
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.EVERY_HUNT);
        Log.d(TAG, "setEveryHuntFromText: parsed count=" + items.size());
        everyHuntItemsLiveData.setValue(items);
        saveCurrentState();
    }

    public String getOptionalText() {
        return toMultiline(optionalItemsLiveData.getValue());
    }

    public void setOptionalFromText(String text) {
        List<PackingItem> items = fromMultiline(text, PackingItem.Source.OPTIONAL);
        optionalItemsLiveData.setValue(items);
        rebuildOptionalCatalog(items);
        saveCurrentState();
    }

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
            list.add(new PackingItem(
                    "custom_" + trimmed.hashCode(),
                    trimmed,
                    null,
                    source
            ));
        }
        return list;
    }

    private void addAllSafe(List<PackingItem> dest, List<PackingItem> src) {
        if (src == null) return;
        dest.addAll(src);
    }

    // ---- Saved loadouts (in-memory only snapshot) --------------------------

    public void saveCurrentLoadoutAs(String name) {
        if (name == null) name = "";
        name = name.trim();
        if (name.isEmpty()) {
            Log.w(TAG, "saveCurrentLoadoutAs: empty name, ignoring");
            return;
        }

        List<PackingItem> current = new ArrayList<>();
        addAllSafe(current, weaponLoadoutItemsLiveData.getValue());
        addAllSafe(current, styleLoadoutItemsLiveData.getValue());
        addAllSafe(current, everyHuntItemsLiveData.getValue());
        addAllSafe(current, optionalItemsLiveData.getValue());

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
            existing = new ArrayList<>(existing);
        }

        String id = "loadout_" + System.currentTimeMillis();
        SavedLoadout loadout = new SavedLoadout(id, name, copy);

        Log.d(TAG, "saveCurrentLoadoutAs: name=" + name + " items=" + copy.size());
        existing.add(loadout);
        savedLoadoutsLiveData.setValue(existing);

        loadoutItemsLiveData.setValue(copy);
    }

    // ---- Unpack All ---------------------------------------------------------

    public void unpackAll() {
        List<PackingItem> packed = packedItemsLiveData.getValue();
        if (packed == null || packed.isEmpty()) {
            Log.d(TAG, "unpackAll: nothing packed");
            return;
        }

        List<PackingItem> snapshot = new ArrayList<>(packed);
        for (PackingItem item : snapshot) {
            onPackedItemUnchecked(item);
        }

        Log.d(TAG, "unpackAll: moved " + snapshot.size()
                + " items back to their source lists");
        saveCurrentState();
    }

    // ---- Context from Home (weapon/style) -----------------------------------

    public void onWeaponTypeChanged(WeaponType weapon) {
        if (weapon == null) return;

        // No-op if it didn't actually change
        if (weapon == currentWeapon) {
            Log.d(TAG, "onWeaponTypeChanged: same weapon " + weapon + ", ignoring");
            return;
        }

        boolean hadPrevious = (currentWeapon != null);

        Log.d(TAG, "onWeaponTypeChanged: " + currentWeapon + " -> " + weapon);
        currentWeapon = weapon;

        // Clear per-session lists so they don't bleed into a new context
        if (hadPrevious) {
            clearSessionOnContextChange();
        }

        // Reload the per-weapon loadout from Firestore (per-weapon doc)
        loadWeaponLoadoutFor(weapon);
    }

    public void onHuntingStyleChanged(HuntingStyle style) {
        if (style == null) return;

        if (style == currentStyle) {
            Log.d(TAG, "onHuntingStyleChanged: same style " + style + ", ignoring");
            return;
        }

        boolean hadPrevious = (currentStyle != null);

        Log.d(TAG, "onHuntingStyleChanged: " + currentStyle + " -> " + style);
        currentStyle = style;

        // Clear per-session lists so they don't bleed into a new context
        if (hadPrevious) {
            clearSessionOnContextChange();
        }

        // Reload the per-style loadout from Firestore (per-style doc)
        loadStyleLoadoutFor(style);
    }


    private void saveWeaponLoadoutForCurrentWeapon() {
        if (currentWeapon == null) {
            Log.w(TAG, "saveWeaponLoadoutForCurrentWeapon: currentWeapon is null, skipping");
            return;
        }

        String docId = "weapon_" + currentWeapon.name();   // e.g. weapon_RIFLE
        DocumentReference docRef = userFs
                .loadoutsCollection()                      // <--- use loadoutsCollection
                .document(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("items", toStateList(
                weaponLoadoutItemsLiveData.getValue(),
                PackingItem.Source.WEAPON
        ));
        data.put("lastUpdated", FieldValue.serverTimestamp());

        Log.d(TAG, "saveWeaponLoadoutForCurrentWeapon: docId=" + docId);

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "saveWeaponLoadoutForCurrentWeapon: success"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveWeaponLoadoutForCurrentWeapon: failure", e));
    }


    private void saveStyleLoadoutForCurrentStyle() {
        if (currentStyle == null) {
            Log.w(TAG, "saveStyleLoadoutForCurrentStyle: currentStyle is null, skipping");
            return;
        }

        String docId = "style_" + currentStyle.name();     // e.g. style_TREESTAND
        DocumentReference docRef = userFs
                .loadoutsCollection()                      // <--- use loadoutsCollection
                .document(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("items", toStateList(
                styleLoadoutItemsLiveData.getValue(),
                PackingItem.Source.STYLE
        ));
        data.put("lastUpdated", FieldValue.serverTimestamp());

        Log.d(TAG, "saveStyleLoadoutForCurrentStyle: docId=" + docId);

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "saveStyleLoadoutForCurrentStyle: success"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveStyleLoadoutForCurrentStyle: failure", e));
    }

    // ---- Optional catalog helpers ------------------------------------------

    private void rebuildOptionalCatalog(List<PackingItem> optional) {
        optionalCatalog.clear();
        if (optional != null) {
            optionalCatalog.addAll(optional);
        }
    }

    public List<PackingItem> getAvailableOptionalCandidates() {
        // Items currently in use anywhere in the packing list
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

    public void addOptionalItems(List<PackingItem> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        List<PackingItem> current = optionalItemsLiveData.getValue();
        if (current == null) current = new ArrayList<>();
        else current = new ArrayList<>(current);

        current.addAll(newItems);
        optionalItemsLiveData.setValue(current);

        // Also track candidates in catalog
        optionalCatalog.addAll(newItems);

        saveCurrentState();
    }

    private void loadWeaponLoadoutFor(@NonNull WeaponType weapon) {
        String docId = "weapon_" + weapon.name();   // e.g. weapon_RIFLE
        Log.d(TAG, "loadWeaponLoadoutFor: docId=" + docId);

        // Clear current list immediately so we don't show stale values
        weaponLoadoutItemsLiveData.setValue(new ArrayList<>());

        userFs.loadoutsCollection()
                .document(docId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        Log.d(TAG, "loadWeaponLoadoutFor: no doc for " + weapon + " (fresh / default)");
                        // keep the list empty
                        return;
                    }

                    Object raw = snapshot.get("items");  // <-- make sure your save code uses this field name
                    List<PackingItem> items = fromStateList(raw, PackingItem.Source.WEAPON);
                    Log.d(TAG, "loadWeaponLoadoutFor: " + weapon + " items=" + items.size());
                    weaponLoadoutItemsLiveData.setValue(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadWeaponLoadoutFor: failed for " + weapon, e);
                    // On failure, we keep the list cleared
                    weaponLoadoutItemsLiveData.setValue(new ArrayList<>());
                });
    }

    private void loadStyleLoadoutFor(@NonNull HuntingStyle style) {
        String docId = "style_" + style.name();     // e.g. style_TREESTAND
        Log.d(TAG, "loadStyleLoadoutFor: docId=" + docId);

        // Clear current list immediately so we don't show stale values
        styleLoadoutItemsLiveData.setValue(new ArrayList<>());

        userFs.loadoutsCollection()
                .document(docId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        Log.d(TAG, "loadStyleLoadoutFor: no doc for " + style + " (fresh / default)");
                        // keep the list empty
                        return;
                    }

                    Object raw = snapshot.get("items");  // <-- must match your save field
                    List<PackingItem> items = fromStateList(raw, PackingItem.Source.STYLE);
                    Log.d(TAG, "loadStyleLoadoutFor: " + style + " items=" + items.size());
                    styleLoadoutItemsLiveData.setValue(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadStyleLoadoutFor: failed for " + style, e);
                    // On failure, we keep the list cleared
                    styleLoadoutItemsLiveData.setValue(new ArrayList<>());
                });
    }
    private void clearSessionOnContextChange() {
        Log.d(TAG, "clearSessionOnContextChange: clearing staged + packed");

        // Clear the per-session lists
        stagedItemsLiveData.setValue(new ArrayList<>());
        packedItemsLiveData.setValue(new ArrayList<>());
        saveCurrentState();

    }
}