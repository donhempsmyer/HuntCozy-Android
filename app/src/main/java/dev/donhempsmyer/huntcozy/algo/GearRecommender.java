package dev.donhempsmyer.huntcozy.algo;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.donhempsmyer.huntcozy.data.model.BodyZone;
import dev.donhempsmyer.huntcozy.data.model.GearAttributes;
import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.LayerType;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.weather.CurrentWeather;

/**
 * GearRecommender is responsible for turning:
 *  - weather
 *  - hunting style
 *  - weapon type
 *  - closet contents
 * into a recommended outfit.
 *
 * v1: outfit-level reasoning by BodyZone x LayerType.
 *  - We decide which body zones + layer slots we want given the conditions.
 *  - For each slot, we pick the best item from the closet.
 *  - Output is a deduplicated outfit list.
 *
 * Later:
 *  - Add hunt window (Morning/Mid, Mid/Evening, All-Day) and use hourly data.
 *  - Refine scoring weights for slots vs attributes.
 *  - Add user feedback & personalization.
 */
public class GearRecommender {

    private static final String TAG = "GearRecommender";

    // Temperature bands in °F (effective/feels-like)
    private enum TempBand {
        HOT,        // > 65°F
        MILD,       // 50–65°F
        COOL,       // 35–50°F
        COLD,       // 20–35°F
        VERY_COLD   // < 20°F
    }

    // Precipitation bands in inches over the considered period
    private enum PrecipBand {
        DRY,        // < 0.1"
        LIGHT,      // 0.1–0.5"
        WET         // > 0.5"
    }

    /**
     * OutfitSlot: a desired "slot" in the outfit:
     *  - body zone (TORSO / LEGS / HEAD / HANDS / FEET)
     *  - layer type (BASE / MID / INSULATION / SHELL / RAIN / etc.)
     *  - whether it's required or just a nice-to-have.
     */
    private static class OutfitSlot {
        final BodyZone zone;
        final LayerType layerType;
        final boolean required;

        OutfitSlot(BodyZone zone, LayerType layerType, boolean required) {
            this.zone = zone;
            this.layerType = layerType;
            this.required = required;
        }

        @Override
        public String toString() {
            return zone + " / " + layerType + " (required=" + required + ")";
        }
    }

    /**
     * Main entry point:
     *  - Builds an outfit by BodyZone x LayerType, rather than just ranking items.
     *  - Uses apparent temperature ("feels-like") as the primary temp signal.
     */
    public List<GearItem> buildOutfitFromCloset(
            List<GearItem> closet,
            CurrentWeather current,
            WeaponType weaponType,
            HuntingStyle huntingStyle
    ) {
        if (closet == null || closet.isEmpty() || current == null) {
            Log.w(TAG, "buildOutfitFromCloset: no closet or weather");
            return new ArrayList<>();
        }

        // NEW: keep both raw and feels-like around explicitly
        double rawTempF = current.temperature2m;        // air temp (already in °F in your app)
        double apparentTempF = current.apparentTemperature; // feels-like (also °F)

        double windMph = current.windSpeed10m;
        double precipTotalIn = current.precipitation + current.snowfall;

        // IMPORTANT CHANGE:
        // Use RAW temp for band classification (which controls which layers exist),
        // so we don't bounce between bands just because wind adds a small chill.
        TempBand tempBand = classifyTempBand(apparentTempF);
        PrecipBand precipBand = classifyPrecipBand(precipTotalIn);

        Log.d(TAG, "buildOutfitFromCloset: apparentTemp=" + apparentTempF
                + "F band=" + tempBand
                + " wind=" + windMph + " mph"
                + " precipTotal=" + precipTotalIn + " in band=" + precipBand
                + " style=" + huntingStyle + " weapon=" + weaponType);

        // For style-based tweaks, also use RAW temp (same reason: more stable bands).
        List<OutfitSlot> desiredSlots =
                determineDesiredSlots(tempBand, precipBand, windMph, huntingStyle, rawTempF);

        for (OutfitSlot slot : desiredSlots) {
            Log.d(TAG, "desired slot: " + slot);
        }

        // For scoring, we still use FEELS-LIKE (apparentTempF) to judge comfort.
        Set<GearItem> outfitSet = new LinkedHashSet<>();

        for (OutfitSlot slot : desiredSlots) {
            GearItem best = selectBestItemForSlot(
                    closet,
                    slot,
                    apparentTempF,      // <- still feels-like here
                    windMph,
                    precipTotalIn,
                    weaponType,
                    huntingStyle
            );

            if (best != null) {
                Log.d(TAG, "selectBestItemForSlot: slot=" + slot + " -> " + best.getName());
                outfitSet.add(best);
            } else if (slot.required) {
                Log.w(TAG, "selectBestItemForSlot: REQUIRED slot has no item. "
                        + "Consider recommending purchase: zone=" + slot.zone
                        + " layer=" + slot.layerType);
            } else {
                Log.d(TAG, "selectBestItemForSlot: optional slot " + slot + " has no item");
            }
        }

        List<GearItem> rawOutfit = new ArrayList<>(outfitSet);
        return simplifyOutfit(rawOutfit);
    }

    // ------------------------------------------------------------------------
    // Classification helpers
    // ------------------------------------------------------------------------

    private TempBand classifyTempBand(double tempF) {
        if (tempF > 65.0) return TempBand.HOT;
        if (tempF > 50.0) return TempBand.MILD;
        if (tempF > 35.0) return TempBand.COOL;
        if (tempF > 20.0) return TempBand.COLD;
        return TempBand.VERY_COLD;
    }

    private PrecipBand classifyPrecipBand(double precipIn) {
        if (precipIn < 0.1) return PrecipBand.DRY;
        if (precipIn <= 0.5) return PrecipBand.LIGHT;
        return PrecipBand.WET;
    }

    /**
     * Returns a 0–14 "offset within band" based on apparent temp.
     * Each band is treated as ~15°F wide.
     */
    private int tempOffsetWithinBand(TempBand band, double apparentTempF) {
        double min;
        switch (band) {
            case VERY_COLD:
                min = 5.0;
                break;
            case COLD:
                min = 20.0;
                break;
            case COOL:
                min = 35.0;
                break;
            case MILD:
                min = 50.0;
                break;
            case HOT:
            default:
                min = 65.0;
                break;
        }

        double clamped = Math.max(min, Math.min(apparentTempF, min + 14.99));
        int offset = (int) Math.round(clamped - min); // 0..14
        if (offset < 0) offset = 0;
        if (offset > 14) offset = 14;
        return offset;
    }

    /**
     * Adjusts temp band based on style using the within-band index (0–14),
     * so we don't always jump a full band just because you're in a treestand.
     */
    private TempBand adjustTempBandForStyle(
            TempBand base,
            double apparentTempF,
            HuntingStyle style
    ) {
        int offset = tempOffsetWithinBand(base, apparentTempF);

        // negative = "colder", positive = "warmer"
        int delta;
        switch (style) {
            case TREESTAND:
                // Feels several degrees colder but not always a full band.
                delta = -4;
                break;
            case STILL_HUNTING:
                // Slightly colder than neutral.
                delta = -2;
                break;
            case SPOT_AND_STALK:
                // Moving more, feels warmer.
                delta = +4;
                break;
            case GROUND_BLIND:
                // Mildly colder, but sheltered from wind vs open treestand.
                delta = -1;
                break;
            default:
                delta = 0;
                break;
        }

        int newOffset = offset + delta;
        TempBand result = base;

        if (newOffset < 0) {
            // Only spill into colder band if we pushed past the bottom
            result = shiftColder(base);
        } else if (newOffset > 14) {
            // Only spill into warmer band if we pushed past the top
            result = shiftWarmer(base);
        }

        return result;
    }

    private TempBand shiftColder(TempBand band) {
        switch (band) {
            case HOT:
                return TempBand.MILD;
            case MILD:
                return TempBand.COOL;
            case COOL:
                return TempBand.COLD;
            case COLD:
            case VERY_COLD:
            default:
                return TempBand.VERY_COLD;
        }
    }

    private TempBand shiftWarmer(TempBand band) {
        switch (band) {
            case VERY_COLD:
                return TempBand.COLD;
            case COLD:
                return TempBand.COOL;
            case COOL:
                return TempBand.MILD;
            case MILD:
            case HOT:
            default:
                return TempBand.HOT;
        }
    }

    // ------------------------------------------------------------------------
    // Determine desired slots by temp / precip / style
    // ------------------------------------------------------------------------

    /**
     * Determine which BodyZone x LayerType slots we want for this hunt.
     *
     * Notes:
     *  - Shell vs Rain: "either, but not both" per zone in most cases.
     *  - Style (e.g. TREESTAND vs SPOT_AND_STALK) adjusts the effective temp band.
     */
    private List<OutfitSlot> determineDesiredSlots(
            TempBand tempBand,
            PrecipBand precipBand,
            double windMph,
            HuntingStyle style,
            double apparentTempF
    ) {
        List<OutfitSlot> slots = new ArrayList<>();

        // Softer, index-based adjustment instead of hard band jumps
        TempBand adjustedBand = adjustTempBandForStyle(tempBand, apparentTempF, style);

        // TORSO logic
        slots.addAll(desiredTorsoSlots(adjustedBand, precipBand, windMph));

        // LEGS logic
        slots.addAll(desiredLegSlots(adjustedBand, precipBand));

        // HEAD logic
        slots.addAll(desiredHeadSlots(adjustedBand, precipBand));

        // HANDS & FEET
        slots.addAll(desiredHandSlots(adjustedBand, precipBand));
        slots.addAll(desiredFeetSlots(adjustedBand, precipBand));

        return slots;
    }

    // -------- TORSO ----------------------------------------------------------------

    private List<OutfitSlot> desiredTorsoSlots(
            TempBand tempBand,
            PrecipBand precipBand,
            double windMph
    ) {
        List<OutfitSlot> slots = new ArrayList<>();

        boolean wet = (precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT);
        boolean windy = windMph > 12.0; // align with your "high" bucket

        switch (tempBand) {
            case HOT:
                // No base layer in HOT band.
                // Only add an outer layer if wind/wet justify it.
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else if (windy) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.MID, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else if (windy) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.MID, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.INSULATION, false));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else if (windy) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.MID, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.INSULATION, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;

            case VERY_COLD:
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.MID, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.INSULATION, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;
        }

        return slots;
    }

    // -------- LEGS -----------------------------------------------------------------

    private List<OutfitSlot> desiredLegSlots(
            TempBand tempBand,
            PrecipBand precipBand
    ) {
        List<OutfitSlot> slots = new ArrayList<>();
        boolean wet = (precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT);

        switch (tempBand) {
            case HOT:
                // No leg base layer in HOT — assume main pant handles it.
                break;

            case MILD:
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.RAIN, true));
                }
                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.INSULATION, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.RAIN, true));
                }
                break;

            case VERY_COLD:
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.MID, true));
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.INSULATION, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.RAIN, true));
                }
                break;
        }

        return slots;
    }

    // -------- HEAD -----------------------------------------------------------------

    private List<OutfitSlot> desiredHeadSlots(
            TempBand tempBand,
            PrecipBand precipBand
    ) {
        List<OutfitSlot> slots = new ArrayList<>();
        boolean wet = (precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT);

        switch (tempBand) {
            case HOT:
                // Cap / light headgear optional
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.HEADGEAR, false));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.HEADGEAR, false));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.HEADGEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.INSULATED_HEADGEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case VERY_COLD:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.INSULATED_HEADGEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;
        }

        return slots;
    }

    // -------- HANDS ----------------------------------------------------------------

    private List<OutfitSlot> desiredHandSlots(
            TempBand tempBand,
            PrecipBand precipBand
    ) {
        List<OutfitSlot> slots = new ArrayList<>();
        boolean wet = (precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT);

        switch (tempBand) {
            case HOT:
                break;
            case MILD:

                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.GLOVE, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.RAIN, true));
                }
                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.INSULATED_GLOVE, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.RAIN, true));
                }
                break;

            case VERY_COLD:
                slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.INSULATED_GLOVE, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.RAIN, true));
                }
                break;
        }

        return slots;
    }

    // -------- FEET -----------------------------------------------------------------

    private List<OutfitSlot> desiredFeetSlots(
            TempBand tempBand,
            PrecipBand precipBand
    ) {
        List<OutfitSlot> slots = new ArrayList<>();
        boolean wet = (precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT);

        switch (tempBand) {
            case HOT:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));

                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));

                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));

                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.INSULATED_SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, true));
                }
                break;

            case VERY_COLD:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.INSULATED_SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.INSULATED_FOOTWEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, true));
                }
                break;
        }

        return slots;
    }

    // ------------------------------------------------------------------------
    // Slot-level item selection
    // ------------------------------------------------------------------------

    private GearItem selectBestItemForSlot(
            List<GearItem> closet,
            OutfitSlot slot,
            double apparentTempF,
            double windMph,
            double precipIn,
            WeaponType weaponType,
            HuntingStyle style
    ) {
        GearItem best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (GearItem item : closet) {
            if (item == null) continue;
            if (item.getBodyZone() != slot.zone) continue;
            if (item.getLayerType() != slot.layerType) continue;

            double score = scoreItemForSlot(
                    item,
                    apparentTempF,
                    windMph,
                    precipIn,
                    weaponType,
                    style,
                    slot
            );

            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    /**
     * Slot-aware scoring; for now we just reuse the existing scoreItem logic.
     * Later we can adjust by slot (e.g., insulation slots weight insulation more heavily).
     */
    private double scoreItemForSlot(
            GearItem item,
            double apparentTempF,
            double windMph,
            double precipIn,
            WeaponType weaponType,
            HuntingStyle style,
            OutfitSlot slot
    ) {
        double baseScore = scoreItem(item, apparentTempF, windMph, precipIn, weaponType, style);
        // TODO: slot-specific adjustments if needed.
        return baseScore;
    }

    /**
     * Per-item scoring logic using apparent temp, wind, precip, style, and weapon.
     */
    private double scoreItem(
            GearItem item,
            double apparentTempF,
            double windMph,
            double precipIn,
            WeaponType weaponType,
            HuntingStyle huntingStyle
    ) {
        GearAttributes attrs = item.getAttributes();
        if (attrs == null) return 0.0;

        TempBand band = classifyTempBand(apparentTempF);

        // Wind buckets (shared with wind-score logic)
        boolean noWind = windMph < 4.0;
        boolean lowWind = windMph >= 4.0 && windMph < 7.0;
        boolean midWind = windMph >= 7.0 && windMph <= 12.0;
        boolean highWind = windMph > 12.0;

        // Temperature component (comfort vs insulation/breathability)
        double tempScore;
        if (apparentTempF < attrs.getComfortTempMinF()) {
            // colder than item is built for -> weight insulation
            tempScore = attrs.getInsulationLevel() * 0.5;
        } else if (apparentTempF > attrs.getComfortTempMaxF()) {
            // warmer than item is built for -> weight breathability
            tempScore = attrs.getBreathabilityLevel() * 0.5;
        } else {
            // In the comfort window, reward strongly
            tempScore = 8.0;
        }

        // Wind component
        double windScore = computeWindScoreForBand(attrs, band, windMph);

        // Precip component (rain + snow combined)
        double precipScore = (precipIn > 0.5)
                ? attrs.getWaterProofLevel()
                : attrs.getWaterProofLevel() * 0.2;

        // Style bonus – depends on hunting style, temp band, and wind
        double styleBonus = 0.0;

        switch (huntingStyle) {
            case TREESTAND: {
                // Sitting still; insulation and windproofness matter a lot.
                double insWeight;
                if (band == TempBand.COLD || band == TempBand.VERY_COLD) {
                    insWeight = 0.9;
                } else if (band == TempBand.COOL) {
                    insWeight = 0.6;
                } else {
                    insWeight = 0.3;
                }

                double windWeight;
                if (highWind) {
                    windWeight = 0.8;
                } else if (midWind) {
                    windWeight = 0.6;
                } else if (lowWind) {
                    windWeight = 0.4;
                } else { // noWind
                    windWeight = 0.2;
                }

                double noiseWeight = 0.15; // treestand always somewhat noise-sensitive

                styleBonus += attrs.getInsulationLevel() * insWeight;
                styleBonus += attrs.getWindProofLevel() * windWeight;
                styleBonus += (10 - attrs.getNoiseLevel()) * noiseWeight;
                break;
            }

            case GROUND_BLIND: {
                // Inside a blind: still (need insulation), but more shielded from wind.
                double insWeight;
                if (band == TempBand.COLD || band == TempBand.VERY_COLD) {
                    insWeight = 0.8;
                } else if (band == TempBand.COOL) {
                    insWeight = 0.5;
                } else {
                    insWeight = 0.3;
                }

                double windWeight;
                if (highWind || midWind) {
                    windWeight = 0.3;
                } else {
                    windWeight = 0.15;
                }

                double noiseWeight = 0.1;

                styleBonus += attrs.getInsulationLevel() * insWeight;
                styleBonus += attrs.getWindProofLevel() * windWeight;
                styleBonus += (10 - attrs.getNoiseLevel()) * noiseWeight;
                break;
            }

            case SPOT_AND_STALK: {
                // Moving a lot: mobility, low bulk, and breathability are key.
                // Wind can mask some noise at higher speeds.
                double noiseWeight = (midWind || highWind) ? 0.25 : 0.6;
                double mobilityWeight = 0.7;
                double bulkWeight = 0.6;
                double breathWeight = 0.4;

                styleBonus += attrs.getMobilityLevel() * mobilityWeight;
                styleBonus += (10 - attrs.getBulkLevel()) * bulkWeight; // lower bulk is better
                styleBonus += (10 - attrs.getNoiseLevel()) * noiseWeight;
                styleBonus += attrs.getBreathabilityLevel() * breathWeight;
                break;
            }

            case STILL_HUNTING: {
                // Slow, deliberate movement: need to be quiet & reasonably warm,
                // but still not fight bulk.
                double noiseWeight = (midWind || highWind) ? 0.3 : 0.55;
                double mobilityWeight = 0.6;
                double bulkWeight = 0.5;

                double insWeight;
                if (band == TempBand.HOT || band == TempBand.MILD) {
                    insWeight = 0.15;
                } else if (band == TempBand.COOL) {
                    insWeight = 0.4;
                } else { // COLD or VERY_COLD
                    insWeight = 0.8;
                }

                styleBonus += attrs.getMobilityLevel() * mobilityWeight;
                styleBonus += (10 - attrs.getBulkLevel()) * bulkWeight;
                styleBonus += (10 - attrs.getNoiseLevel()) * noiseWeight;
                styleBonus += attrs.getInsulationLevel() * insWeight;
                break;
            }

            default:
                // No extra style bias
                break;
        }

        // Weapon bonus (bow -> extra quiet)
        double weaponBonus = 0.0;
        if (weaponType == WeaponType.BOW) {
            weaponBonus += (10 - attrs.getNoiseLevel()) * 0.4;
        }

        double score = tempScore + windScore + precipScore + styleBonus + weaponBonus;

        Log.d(TAG, "scoreItem: " + item.getName() + " score=" + score
                + " (apparentTempF=" + apparentTempF
                + ", wind=" + windMph
                + ", precipIn=" + precipIn + ")");
        return score;
    }

    /**
     * Wind score that respects both the temp band (how dangerous wind is)
     * and your wind thresholds: <4, 4–7, 7–12, >12 mph.
     */
    private double computeWindScoreForBand(GearAttributes attrs, TempBand band, double windMph) {
        int windLevel = attrs.getWindProofLevel();

        boolean noWind = windMph < 4.0;
        boolean lowWind = windMph >= 4.0 && windMph < 7.0;
        boolean midWind = windMph >= 7.0 && windMph <= 12.0;
        boolean highWind = windMph > 12.0;

        double multiplier;

        switch (band) {
            case HOT:
                if (noWind || lowWind) {
                    multiplier = 0.0;
                } else if (midWind) {
                    multiplier = 0.1;
                } else { // highWind
                    multiplier = 0.2;
                }
                break;

            case MILD:
                if (noWind) {
                    multiplier = 0.0;
                } else if (lowWind) {
                    multiplier = 0.1;
                } else if (midWind) {
                    multiplier = 0.3;
                } else { // highWind
                    multiplier = 0.5;
                }
                break;

            case COOL:
                if (noWind) {
                    multiplier = 0.1;
                } else if (lowWind) {
                    multiplier = 0.3;
                } else if (midWind) {
                    multiplier = 0.6;
                } else { // highWind
                    multiplier = 0.9;
                }
                break;

            case COLD:
                if (noWind) {
                    multiplier = 0.2;
                } else if (lowWind) {
                    multiplier = 0.5;
                } else if (midWind) {
                    multiplier = 0.9;
                } else { // highWind
                    multiplier = 1.1;
                }
                break;

            case VERY_COLD:
            default:
                if (noWind) {
                    multiplier = 0.3;
                } else if (lowWind) {
                    multiplier = 0.7;
                } else if (midWind) {
                    multiplier = 1.1;
                } else { // highWind
                    multiplier = 1.4;
                }
                break;
        }

        return windLevel * multiplier;
    }

    // ------------------------------------------------------------------------
    // Outfit post-processing: merge multi-role outer pieces
    // ------------------------------------------------------------------------

    /**
     * Simplifies the outfit by letting a single "monster" outer piece
     * cover multiple requirements in the same body zone (insulation + rain + shell).
     *
     * Heuristic:
     *  - For each zone, if we find an item with high insulation + wind + waterproof,
     *    we keep it and remove weaker outer layers for that zone.
     */
    private List<GearItem> simplifyOutfit(List<GearItem> outfit) {
        if (outfit == null || outfit.isEmpty()) return outfit;

        List<GearItem> result = new ArrayList<>(outfit);

        for (BodyZone zone : BodyZone.values()) {
            GearItem multiProtector = null;

            // 1) find a "do-it-all" piece for this zone
            for (GearItem item : result) {
                if (item.getBodyZone() != zone) continue;
                GearAttributes a = item.getAttributes();
                if (a == null) continue;

                boolean strongIns = a.getInsulationLevel() >= 7;
                boolean strongWind = a.getWindProofLevel() >= 7;
                boolean strongWater = a.getWaterProofLevel() >= 7;

                if (strongIns && strongWind && strongWater) {
                    multiProtector = item;
                    break;
                }
            }

            if (multiProtector == null) continue;

            // 2) remove weaker outer layers for that same zone
            List<GearItem> toRemove = new ArrayList<>();
            for (GearItem item : result) {
                if (item == multiProtector) continue;
                if (item.getBodyZone() != zone) continue;

                LayerType lt = item.getLayerType();
                if (lt == LayerType.SHELL
                        || lt == LayerType.RAIN
                        || lt == LayerType.INSULATION) {
                    toRemove.add(item);
                }
            }

            result.removeAll(toRemove);
        }

        return result;
    }

    // Alternate approach:
    //  - Introduce a RecommendationResult object that also carries "notes" back to the UI
    //    (e.g., "You don't own any insulation for LEGS", "Consider leaving bibs off on hike in").
}
