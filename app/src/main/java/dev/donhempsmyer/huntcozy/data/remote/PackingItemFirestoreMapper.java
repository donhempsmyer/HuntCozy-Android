package dev.donhempsmyer.huntcozy.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.PackingItem;

public class PackingItemFirestoreMapper {

    @NonNull
    public static Map<String, Object> toMap(@NonNull PackingItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("label", item.getLabel());
        map.put("source", item.getSource() != null ? item.getSource().name() : null);

        // We don't persist full GearItem here; just its id if present.
        if (item.getGearItem() != null && item.getGearItem().getId() != null) {
            map.put("gearId", item.getGearItem().getId());
        } else {
            map.put("gearId", null);
        }
        return map;
    }

    @NonNull
    public static List<Map<String, Object>> toMapList(@Nullable List<PackingItem> items) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (items == null) return out;
        for (PackingItem item : items) {
            if (item == null) continue;
            out.add(toMap(item));
        }
        return out;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static List<PackingItem> fromMapList(@Nullable List<?> rawList,
                                                @NonNull PackingItem.Source defaultSource) {
        List<PackingItem> result = new ArrayList<>();
        if (rawList == null) return result;

        for (Object o : rawList) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) o;
            PackingItem item = fromMap(map, defaultSource);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    @Nullable
    public static PackingItem fromMap(@NonNull Map<String, Object> map,
                                      @NonNull PackingItem.Source defaultSource) {
        Object idObj = map.get("id");
        Object labelObj = map.get("label");
        Object sourceObj = map.get("source");

        String id = idObj instanceof String ? (String) idObj : null;
        String label = labelObj instanceof String ? (String) labelObj : null;
        String sourceStr = sourceObj instanceof String ? (String) sourceObj : null;

        if (id == null) {
            id = "pi_" + (label != null ? label.hashCode() : System.currentTimeMillis());
        }
        if (label == null) {
            label = "Item";
        }

        PackingItem.Source src = defaultSource;
        if (sourceStr != null) {
            try {
                src = PackingItem.Source.valueOf(sourceStr);
            } catch (IllegalArgumentException ignored) {
                // fall back to defaultSource
            }
        }

        // We *could* re-link gearId to a GearItem from the closet later.
        // For now, we just keep gearItem = null (non-clothing or generic reference).
        return new PackingItem(
                id,
                label,
                null,   // gearItem
                src
        );
    }
}
