package dev.donhempsmyer.huntcozy.data.model;




import android.util.Log;

import java.util.UUID;

/**
 * GearItem represents a specific piece of gear in the user's closet.
 * Example: "Sitka Fanatic Jacket size L", not just "generic insulated jacket".
 */
public class GearItem {

    private static final String TAG = "GearItem";

    private final String id;              // stable ID for persistence
    private final String name;            // user-friendly label

    private final BodyZone bodyZone;
    private final LayerType layerType;
    private final MaterialType materialType;

    private final GearAttributes attributes;

    // Optional metadata for later features
    private final String brand;
    private final String notes;
    private final boolean userOwned;

    public GearItem(String id,
                    String name,
                    BodyZone bodyZone,
                    LayerType layerType,
                    MaterialType materialType,
                    GearAttributes attributes,
                    String brand,
                    String notes,
                    boolean userOwned) {
        this.id = (id != null) ? id : UUID.randomUUID().toString();
        this.name = name;
        this.bodyZone = bodyZone;
        this.layerType = layerType;
        this.materialType = materialType;
        this.attributes = attributes;
        this.brand = brand;
        this.notes = notes;
        this.userOwned = userOwned;

        Log.d(TAG, "GearItem created: " + this.name + " (" + bodyZone + ", " + layerType + ")");
    }

    /**
     * Convenience constructor if you don't care about brand/notes/userOwned yet.
     */
    public GearItem(String name,
                    BodyZone bodyZone,
                    LayerType layerType,
                    MaterialType materialType,
                    GearAttributes attributes) {
        this(null, name, bodyZone, layerType, materialType, attributes,
                null, null, true);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BodyZone getBodyZone() { return bodyZone; }
    public LayerType getLayerType() { return layerType; }
    public MaterialType getMaterialType() { return materialType; }
    public GearAttributes getAttributes() { return attributes; }
    public String getBrand() { return brand; }
    public String getNotes() { return notes; }
    public boolean isUserOwned() { return userOwned; }

    /**
     * Backwards-compat "category" string, derived from body zone + layer type.
     * This replaces the old explicit category field.
     */
    public String getCategory() {
        // Example: "TORSO · INSULATION"
        String zone = (bodyZone != null) ? bodyZone.name() : "UNKNOWN_ZONE";
        String layer = (layerType != null) ? layerType.name() : "UNKNOWN_LAYER";
        return zone + " · " + layer;
    }



    @Override
    public String toString() {
        return name;
    }

    // Alternate approach:
    // - Keep a real `category` field and let the user customize it,
    //   while still exposing bodyZone/layerType separately for the algorithm.
}