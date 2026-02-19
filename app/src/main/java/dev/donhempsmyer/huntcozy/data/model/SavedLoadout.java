package dev.donhempsmyer.huntcozy.data.model;

import java.util.List;

/**
 * Represents a saved non-clothing loadout, e.g. "Archery Evening Sit".
 * Holds a snapshot of PackingItems at the time of save.
 */
public class SavedLoadout {

    private final String id;
    private final String name;
    private final List<PackingItem> items;

    public SavedLoadout(String id, String name, List<PackingItem> items) {
        this.id = id;
        this.name = name;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<PackingItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "SavedLoadout{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}
