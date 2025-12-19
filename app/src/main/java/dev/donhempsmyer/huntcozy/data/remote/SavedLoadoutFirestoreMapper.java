package dev.donhempsmyer.huntcozy.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.PackingItem;
import dev.donhempsmyer.huntcozy.data.model.SavedLoadout;

public class SavedLoadoutFirestoreMapper {

    @NonNull
    public static Map<String, Object> toMap(@NonNull SavedLoadout loadout) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", loadout.getId());
        map.put("name", loadout.getName());
        map.put("items",
                PackingItemFirestoreMapper.toMapList(loadout.getItems()));
        map.put("updatedAt", FieldValue.serverTimestamp());
        return map;
    }

    @Nullable
    public static SavedLoadout fromSnapshot(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        String id = doc.getString("id");
        if (id == null) id = doc.getId();

        String name = doc.getString("name");
        if (name == null) name = "Unnamed Loadout";

        Object rawItems = doc.get("items");
        List<PackingItem> items = new ArrayList<>();
        if (rawItems instanceof List) {
            // Use EVERY_HUNT as a neutral default; each PackingItem still carries its own source.
            items = PackingItemFirestoreMapper.fromMapList(
                    (List<?>) rawItems,
                    PackingItem.Source.EVERY_HUNT
            );
        }

        return new SavedLoadout(id, name, items);
    }
}
