package dev.donhempsmyer.huntcozy.data.model;

import androidx.annotation.Nullable;

/**
 * PackingItem is a UI-level representation of something you pack for a hunt.
 * It may be directly backed by a GearItem (clothing) or just a label
 * (e.g. "Rangefinder", "Knife") for loadout items.
 */
public class PackingItem {

    private final String id;          // unique per item (can be gearId or synthetic)
    private final String label;     // display name
    @Nullable
    private final GearItem gearItem; // null for generic items

    public PackingItem(String id, String label, @Nullable GearItem gearItem) {
        this.id = id;
        this.label = label;
        this.gearItem = gearItem;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Nullable
    public GearItem getGearItem() {
        return gearItem;
    }

    @Override
    public String toString() {
        return "PackingItem{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", gearItem=" + (gearItem != null ? gearItem.getName() : "null") +
                '}';
    }

    // Alternate approach:
    // - Add flags like "required", "optional", or a quantity field.
}
