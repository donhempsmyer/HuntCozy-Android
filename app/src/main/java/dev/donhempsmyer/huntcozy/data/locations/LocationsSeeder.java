package dev.donhempsmyer.huntcozy.data.locations;

import java.util.ArrayList;
import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.location.HuntLocation;

/**
 * LocationsSeeder seeds a few sample locations.
 *
 * v1:
 *  - in-memory only.
 * v2:
 *  - replaced by Room/Firebase storage and user-created locations.
 */
public final class LocationsSeeder {

    private LocationsSeeder() {}

    public static List<HuntLocation> seedDefaultLocations() {
        List<HuntLocation> list = new ArrayList<>();

        // Example 1
        list.add(new HuntLocation(
                1L,
                "Back Ridge Stand",
                65.500000,   // fake sample coords
                -88.500000
        ));

        // Example 2
        list.add(new HuntLocation(
                2L,
                "Pine Bottom Ground Blind",
                44.510000,
                120.510000
        ));

        // Example 3
        list.add(new HuntLocation(
                3L,
                "Field Edge Ladder Stand",
                34.520000,
                -67.520000
        ));

        //example 4
        list.add(new HuntLocation(
                4L,
                "Navarino",
                44.38,
                88.290000
        ));

        list.add(new HuntLocation(
                5L,
                "Pensacola",
                30.41,
                -87.22
        ));


        return list;
    }

    // Alternate approach:
    // - Accept a Random or timestamp and generate ids that won't clash
    //   with user-created locations later.
}
