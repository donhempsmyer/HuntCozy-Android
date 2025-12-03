package dev.donhempsmyer.huntcozy.data.repository;


import dev.donhempsmyer.huntcozy.data.model.BodyZone;
import dev.donhempsmyer.huntcozy.data.model.GearAttributes;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.LayerType;
import dev.donhempsmyer.huntcozy.data.model.MaterialType;

import java.util.Arrays;
import java.util.List;

/**
 * ClosetSeeder provides a static list of sample gear items.
 * This is a temporary stand-in until we wire up real persistence.
 */
public class ClosetSeeder {

    public static List<GearItem> createSampleCloset() {
        return Arrays.asList(
                // HEAD
                new GearItem("Merino beanie",
                        BodyZone.HEAD,
                        LayerType.HEADGEAR,
                        MaterialType.MERINO_WOOL,
                        new GearAttributes(
                                3,  // insulation
                                1,  // wind
                                0,  // water
                                7,  // breathability
                                1,  // noise
                                9,  // mobility
                                -5, // comfort min
                                10, // comfort max
                                false, // scentControl
                                true,  // quietFaceFabric
                                1,     // weight
                                1      // bulk
                        )),

                // TORSO BASE
                new GearItem("Heavy merino base top",
                        BodyZone.TORSO,
                        LayerType.BASE,
                        MaterialType.MERINO_WOOL,
                        new GearAttributes(
                                4,  // insulation
                                0,  // wind
                                0,  // water
                                8,  // breathability
                                1,  // noise
                                9,  // mobility
                                -10,
                                5,
                                false,
                                true,
                                2,
                                2
                        )),

                // TORSO MID
                new GearItem("Grid fleece hoodie",
                        BodyZone.TORSO,
                        LayerType.MID,
                        MaterialType.SYNTHETIC_FLEECE,
                        new GearAttributes(
                                5,
                                1,
                                0,
                                7,
                                2,
                                8,
                                -5,
                                10,
                                false,
                                true,
                                3,
                                3
                        )),

                // TORSO INSULATION
                new GearItem("Lofted insulated jacket",
                        BodyZone.TORSO,
                        LayerType.INSULATION,
                        MaterialType.SYNTHETIC_INSULATION,
                        new GearAttributes(
                                8,
                                2,
                                1,
                                4,
                                4,
                                6,
                                -20,
                                -5,
                                false,
                                true,
                                5,
                                6
                        )),

                // TORSO SHELL
                new GearItem("Quiet softshell jacket",
                        BodyZone.TORSO,
                        LayerType.SHELL,
                        MaterialType.SOFTSHELL,
                        new GearAttributes(
                                3,
                                7,
                                4,
                                6,
                                2,
                                7,
                                -10,
                                10,
                                false,
                                true,
                                4,
                                4
                        )),

                // RAIN
                new GearItem("Hardshell rain jacket",
                        BodyZone.TORSO,
                        LayerType.RAIN,
                        MaterialType.HARDSHELL,
                        new GearAttributes(
                                2,
                                9,
                                10,
                                4,
                                4,
                                5,
                                -5,
                                15,
                                false,
                                false,
                                5,
                                5
                        )),

                // LEGS BASE
                new GearItem("Merino base bottoms",
                        BodyZone.LEGS,
                        LayerType.BASE,
                        MaterialType.MERINO_WOOL,
                        new GearAttributes(
                                4,
                                0,
                                0,
                                8,
                                1,
                                9,
                                -10,
                                5,
                                false,
                                true,
                                2,
                                2
                        )),

                // LEGS INSULATION
                new GearItem("Insulated bibs",
                        BodyZone.LEGS,
                        LayerType.INSULATION,
                        MaterialType.SYNTHETIC_INSULATION,
                        new GearAttributes(
                                8,
                                4,
                                3,
                                3,
                                4,
                                5,
                                -25,
                                -5,
                                false,
                                true,
                                7,
                                7
                        )),

                // FOOTWEAR
                new GearItem("400g insulated boots",
                        BodyZone.FEET,
                        LayerType.FOOTWEAR,
                        MaterialType.LEATHER,
                        new GearAttributes(
                                6,
                                5,
                                5,
                                3,
                                4,
                                6,
                                -10,
                                10,
                                false,
                                true,
                                7,
                                5
                        )),

                new GearItem("Merino boot socks",
                        BodyZone.FEET,
                        LayerType.SOCK,
                        MaterialType.MERINO_WOOL,
                        new GearAttributes(
                                3,
                                0,
                                0,
                                8,
                                1,
                                10,
                                -5,
                                15,
                                false,
                                true,
                                1,
                                1
                        )),

                // HANDS
                new GearItem("Insulated shooting glove",
                        BodyZone.HANDS,
                        LayerType.GLOVE,
                        MaterialType.SYNTHETIC_INSULATION,
                        new GearAttributes(
                                6,
                                3,
                                2,
                                4,
                                3,
                                7,
                                -15,
                                0,
                                false,
                                true,
                                2,
                                2
                        ))
        );
    }

    // Alternate approach:
    // - Move this into a JSON asset and parse it so non-developers can edit the default closet.
}