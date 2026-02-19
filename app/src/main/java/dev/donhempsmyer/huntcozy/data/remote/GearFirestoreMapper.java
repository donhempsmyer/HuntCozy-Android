package dev.donhempsmyer.huntcozy.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.BodyZone;
import dev.donhempsmyer.huntcozy.data.model.GearAttributes;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.LayerType;
import dev.donhempsmyer.huntcozy.data.model.MaterialType;

public class GearFirestoreMapper {

    private static final String TAG = "GearFirestoreMapper";

    // ------------------------------------------------------------------------
    //  GearItem -> Firestore map
    // ------------------------------------------------------------------------
    @NonNull
    public static Map<String, Object> toMap(@NonNull GearItem item) {
        Map<String, Object> map = new HashMap<>();

        // You can keep id as a field if you like, but the canonical id is doc.getId().
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("bodyZone", item.getBodyZone() != null ? item.getBodyZone().name() : null);
        map.put("layerType", item.getLayerType() != null ? item.getLayerType().name() : null);
        map.put("materialType", item.getMaterialType() != null ? item.getMaterialType().name() : null);

        map.put("brand", item.getBrand());
        map.put("notes", item.getNotes());
        map.put("userOwned", item.isUserOwned());

        GearAttributes attrs = item.getAttributes();
        if (attrs != null) {
            Map<String, Object> attrsMap = new HashMap<>();
            attrsMap.put("comfortTempMinF", attrs.getComfortTempMinF());
            attrsMap.put("comfortTempMaxF", attrs.getComfortTempMaxF());
            attrsMap.put("insulationLevel", attrs.getInsulationLevel());
            attrsMap.put("breathabilityLevel", attrs.getBreathabilityLevel());
            attrsMap.put("windProofLevel", attrs.getWindProofLevel());
            attrsMap.put("waterProofLevel", attrs.getWaterProofLevel());
            attrsMap.put("noiseLevel", attrs.getNoiseLevel());
            attrsMap.put("bulkLevel", attrs.getBulkLevel());
            attrsMap.put("mobilityLevel", attrs.getMobilityLevel());
            attrsMap.put("weightLevel", attrs.getWeightLevel());
            attrsMap.put("scentControl", attrs.hasScentControl());
            attrsMap.put("quietFaceFabric", attrs.hasQuietFaceFabric());

            map.put("attributes", attrsMap);
        } else {
            // Don’t overwrite an existing attributes map with null
            // by omitting the field entirely.
            // map.put("attributes", null);
        }

        return map;
    }

    // ------------------------------------------------------------------------
    //  Firestore doc -> GearItem
    // ------------------------------------------------------------------------
    @Nullable
    public static GearItem fromSnapshot(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        // Prefer field "id" if present, but fall back to doc id
        String id = doc.getString("id");
        if (id == null || id.trim().isEmpty()) {
            id = doc.getId();
        }

        String name = doc.getString("name");
        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Gear";
        }

        BodyZone bodyZone = parseEnum(BodyZone.class, doc.getString("bodyZone"));
        LayerType layerType = parseEnum(LayerType.class, doc.getString("layerType"));
        MaterialType materialType = parseEnum(MaterialType.class, doc.getString("materialType"));

        String brand = doc.getString("brand");
        String notes = doc.getString("notes");
        Boolean owned = doc.getBoolean("userOwned");
        boolean userOwned = (owned == null) || owned;

        // ----- Attributes subdocument -----
        @SuppressWarnings("unchecked")
        Map<String, Object> attrsMap = (Map<String, Object>) doc.get("attributes");
        GearAttributes attrs = null;

        if (attrsMap != null) {
            attrs = new GearAttributes(
                    // IMPORTANT: order must match GearAttributes constructor
                    getInt(attrsMap, "insulationLevel", 0),
                    getInt(attrsMap, "breathabilityLevel", 0),
                    getInt(attrsMap, "windProofLevel", 0),
                    getInt(attrsMap, "waterProofLevel", 0),
                    getInt(attrsMap, "noiseLevel", 0),
                    getInt(attrsMap, "mobilityLevel", 0),
                    getDouble(attrsMap, "comfortTempMinF", 0.0),
                    getDouble(attrsMap, "comfortTempMaxF", 0.0),
                    getBoolean(attrsMap, "scentControl", false),
                    getBoolean(attrsMap, "quietFaceFabric", false),
                    getInt(attrsMap, "weightLevel", 0),
                    getInt(attrsMap, "bulkLevel", 0)
            );
        }

        return new GearItem(
                id,
                name,
                bodyZone,
                layerType,
                materialType,
                attrs,
                brand,
                notes,
                userOwned
        );
    }

    // ------------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------------

    private static double getDouble(Map<String, Object> map, String key, double def) {
        Object o = map.get(key);
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        return def;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object o = map.get(key);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return def;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object o = map.get(key);
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return def;
    }

    private static <T extends Enum<T>> @Nullable T parseEnum(@NonNull Class<T> enumType,
                                                             @Nullable String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "parseEnum: unknown value " + value + " for " + enumType.getSimpleName());
            return null;
        }
    }
}
