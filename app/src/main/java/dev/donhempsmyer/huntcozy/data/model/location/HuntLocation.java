package dev.donhempsmyer.huntcozy.data.model.location;

import androidx.annotation.NonNull;

/**
 * HuntLocation represents a saved hunting location with lat/long.
 *
 * v1:
 *  - simple in-memory objects, seeded with a few examples.
 * v2:
 *  - persist with Room/Firebase and add fields like wind access, notes, etc.
 */
public class HuntLocation {

    private final long id;          // simple unique id (v1: from seeder)
    @NonNull
    private final String name;      // "North Ridge Stand"
    private final double latitude;  // decimal degrees
    private final double longitude; // decimal degrees

    public HuntLocation(long id,
                        @NonNull String name,
                        double latitude,
                        double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @NonNull
    @Override
    public String toString() {
        return "HuntLocation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                '}';
    }

    // Alternate approach:
    // - Add fields like "isDefault", "accessDirection", "notes"
    //   and let the recommendation system reason about them later
    //   (e.g., wind direction vs stand access).
}
