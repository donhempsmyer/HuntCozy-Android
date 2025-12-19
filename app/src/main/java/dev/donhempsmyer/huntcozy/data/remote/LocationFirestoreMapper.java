package dev.donhempsmyer.huntcozy.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

public class LocationFirestoreMapper {

    // Field names in Firestore
    private static final String FIELD_ID          = "id";
    private static final String FIELD_NAME        = "name";
    private static final String FIELD_LAT         = "latitude";
    private static final String FIELD_LON         = "longitude";
    private static final String FIELD_TZ          = "timeZoneId";
    private static final String FIELD_NOTES       = "notes";

    @NonNull
    public static Map<String, Object> toMap(@NonNull HuntLocation loc) {
        Map<String, Object> map = new HashMap<>();

        // We store both the document id and an "id" field to make migrations easier later.
        map.put(FIELD_ID, loc.getId());
        map.put(FIELD_NAME, loc.getName());
        map.put(FIELD_LAT, loc.getLatitude());
        map.put(FIELD_LON, loc.getLongitude());

        // If your HuntLocation does not expose a time zone / notes, you can safely
        // remove these two lines and the corresponding reads in fromSnapshot().
        map.put(FIELD_TZ,  loc.getTimeZoneId());
        map.put(FIELD_NOTES, loc.getNotes());

        return map;
    }

    @Nullable
    public static HuntLocation fromSnapshot(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        // Extra safety: ignore any "_meta" docs if they slip through the query
        if ("_meta".equals(doc.getId())) {
            return null;
        }

        String id   = doc.getString(FIELD_ID);
        String name = doc.getString(FIELD_NAME);
        Double lat  = doc.getDouble(FIELD_LAT);
        Double lon  = doc.getDouble(FIELD_LON);
        String tz   = doc.getString(FIELD_TZ);
        String notes = doc.getString(FIELD_NOTES);

        // Fallbacks
        if (id == null) {
            id = doc.getId();
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Location";
        }
        double safeLat = (lat != null) ? lat : 0.0;
        double safeLon = (lon != null) ? lon : 0.0;

        // ---- ADAPT HERE IF YOUR HuntLocation SIGNATURE IS DIFFERENT ----
        // This assumes:
        //   public HuntLocation(String id,
        //                       String name,
        //                       double latitude,
        //                       double longitude,
        //                       String timeZoneId,
        //                       String notes)
        //
        // If your constructor has fewer fields (e.g. no tz/notes), adjust accordingly.
        return new HuntLocation(id, name, safeLat, safeLon, tz, notes);
    }
}
