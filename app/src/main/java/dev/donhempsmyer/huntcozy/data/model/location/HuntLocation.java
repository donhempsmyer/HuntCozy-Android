package dev.donhempsmyer.huntcozy.data.model.location;

import androidx.annotation.Nullable;

public class HuntLocation {

    private final String id;       // now String, aligns with Firestore doc id
    private final String name;
    private final double latitude;
    private final double longitude;
    @Nullable
    private final String timeZoneId;
    @Nullable
    private final String notes;

    // Primary constructor used by Firestore + app code
    public HuntLocation(String id,
                        String name,
                        double latitude,
                        double longitude,
                        @Nullable String timeZoneId,
                        @Nullable String notes) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZoneId = timeZoneId;
        this.notes = notes;
    }

    // Optional: legacy convenience constructor if you still have places using long
    // You can keep this for now to reduce compile errors, then migrate callers.
    public HuntLocation(long numericId,
                        String name,
                        double latitude,
                        double longitude,
                        @Nullable String timeZoneId,
                        @Nullable String notes) {
        this(String.valueOf(numericId), name, latitude, longitude, timeZoneId, notes);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Nullable
    public String getTimeZoneId() {
        return timeZoneId;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "HuntLocation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", timeZoneId='" + timeZoneId + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
