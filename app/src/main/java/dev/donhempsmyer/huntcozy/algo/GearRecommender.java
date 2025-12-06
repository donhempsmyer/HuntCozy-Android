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
import dev.donhempsmyer.huntcozy.data.model.MaterialType;
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
     *  - layer type (BASE / MID / INSULATION / SHELL / RAIN)
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
     * Old-style method that returned top items globally.
     * Kept for reference / debugging.
     *
     * v1.5+: Prefer buildOutfitFromCloset().
     */
    @Deprecated
    public List<GearItem> recommendFromCloset(
            List<GearItem> closet,
            CurrentWeather current,
            WeaponType weaponType,
            HuntingStyle huntingStyle
    ) {
        Log.w(TAG, "recommendFromCloset: deprecated; prefer buildOutfitFromCloset()");
        return buildOutfitFromCloset(closet, current, weaponType, huntingStyle);
    }

    /**
     * New main entry point:
     *  - Builds an outfit by BodyZone x LayerType, rather than just ranking items.
     *  - Uses raw temperature for now; we'll refine to apparent temperature later.
     *
     * Note: current "weather" is still CurrentWeather; later we may switch
     * to a HuntWindowConditions object (min/max temp, wind, precip for the hunt period).
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

        // v1: use raw temperature as "effective"; later we can integrate
        // apparentTemperature and wind into a better feels-like metric.
        double effectiveTempF = current.temperature2m;
        double windMph = current.windSpeed10m;
        double precipTotalIn = current.precipitation + current.snowfall;

        TempBand tempBand = classifyTempBand(effectiveTempF);
        PrecipBand precipBand = classifyPrecipBand(precipTotalIn);

        Log.d(TAG, "buildOutfitFromCloset: temp=" + effectiveTempF
                + "F band=" + tempBand
                + " wind=" + windMph + " mph"
                + " precipTotal=" + precipTotalIn + " in band=" + precipBand
                + " style=" + huntingStyle + " weapon=" + weaponType);

        List<OutfitSlot> desiredSlots =
                determineDesiredSlots(tempBand, precipBand, windMph, huntingStyle);

        for (OutfitSlot slot : desiredSlots) {
            Log.d(TAG, "desired slot: " + slot);
        }

        // For each slot, find the best matching item from the closet.
        Set<GearItem> outfitSet = new LinkedHashSet<>();

        for (OutfitSlot slot : desiredSlots) {
            GearItem best = selectBestItemForSlot(
                    closet,
                    slot,
                    effectiveTempF,
                    windMph,
                    precipTotalIn,
                    weaponType,
                    huntingStyle
            );

            if (best != null) {
                Log.d(TAG, "selectBestItemForSlot: slot=" + slot + " -> " + best.getName());
                outfitSet.add(best);
            } else if (slot.required) {
                // TODO: surface this as a user-facing note later ("You don't own any X for Y").
                Log.w(TAG, "selectBestItemForSlot: REQUIRED slot has no item. "
                        + "Consider recommending purchase: zone=" + slot.zone
                        + " layer=" + slot.layerType);
            } else {
                Log.d(TAG, "selectBestItemForSlot: optional slot " + slot + " has no item");
            }
        }

        // Deduplicated, insertion-order preserved outfit.
        return new ArrayList<>(outfitSet);
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

    // ------------------------------------------------------------------------
    // Determine desired slots by temp / precip / style
    // ------------------------------------------------------------------------

    /**
     * Determine which BodyZone x LayerType slots we want for this hunt.
     *
     * Notes:
     *  - Shell vs Rain: "either, but not both" per zone in most cases.
     *  - Style (e.g. TREESTAND vs SPOT_AND_STALK) may shift required/optional.
     */
    private List<OutfitSlot> determineDesiredSlots(
            TempBand tempBand,
            PrecipBand precipBand,
            double windMph,
            HuntingStyle style
    ) {
        List<OutfitSlot> slots = new ArrayList<>();

        // We'll treat treestand as effectively colder (we sit a lot),
        // and spot & stalk as effectively warmer (we move more).
        TempBand adjustedBand = adjustTempBandForStyle(tempBand, style);

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

    private TempBand adjustTempBandForStyle(TempBand base, HuntingStyle style) {
        // Simple: treestand -> "colder" one band, spot_and_stalk -> "warmer" one band.
        switch (style) {
            case TREESTAND:
                return shiftColder(base);
            case SPOT_AND_STALK:
                return shiftWarmer(base);
            default:
                return base;
        }
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

    // -------- TORSO ----------------------------------------------------------------

    private List<OutfitSlot> desiredTorsoSlots(
            TempBand tempBand,
            PrecipBand precipBand,
            double windMph
    ) {
        List<OutfitSlot> slots = new ArrayList<>();

        boolean windy = windMph > 15.0;
        boolean wet = precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT;

        switch (tempBand) {
            case HOT:
                // base optional, shell or rain only if wind/wet
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.BASE, false));
                if (wet) {
                    // Shell vs Rain: prefer RAIN instead of SHELL in wet.
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.RAIN, true));
                } else if (windy) {
                    slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.SHELL, true));
                }
                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.TORSO, LayerType.MID, false));
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
                // Shell vs Rain: prefer RAIN in wet, otherwise SHELL for wind
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
        boolean wet = precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT;

        switch (tempBand) {
            case HOT:
                // base mostly optional
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, false));
                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, false));
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.BASE, true));
                slots.add(new OutfitSlot(BodyZone.LEGS, LayerType.INSULATION, false));
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
        boolean wet = precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT;

        // For HEAD we use HEADGEAR as the primary layer type.
        // Thickness / warmth is expressed by the GearAttributes (insulation, etc.),
        // so we don't need separate BASE vs INSULATION layer types here.

        switch (tempBand) {
            case HOT:
                // Cap / light headgear optional
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.HEADGEAR, false));
                if (wet) {
                    // Dedicated rain hood / hat
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case MILD:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.BASE, false));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.RAIN, true));
                }
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.HEAD, LayerType.BASE, true));
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
                // In colder bands we still just pick one HEADGEAR item,
                // but the recommender should prefer higher-insulation pieces.
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
        boolean wet = precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT;

        // For HANDS we use GLOVE as the main type.
        // If you later add liners, we can treat them as a separate LayerType,
        // but for now one GLOVE slot per outfit is enough.

        switch (tempBand) {
            case HOT:
            case MILD:
                // Light gloves optional; more about abrasion / minimal warmth.
                slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.GLOVE, false));
                if (wet) {
                    // If you model rain over-mitts as RAIN, we can request them here.
                    slots.add(new OutfitSlot(BodyZone.HANDS, LayerType.RAIN, false));
                }
                break;

            case COOL:
                // Gloves recommended
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
                // In cold/very cold, gloves are effectively required
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
        boolean wet = precipBand == PrecipBand.WET || precipBand == PrecipBand.LIGHT;

        // For FEET we distinguish between SOCK and FOOTWEAR.
        //  - SOCK: base layer on the foot
        //  - FOOTWEAR: boots / shoes
        // Rain can represent rubber boots or overshoes if you model them.

        switch (tempBand) {
            case HOT:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, false));
                }
                break;

            case MILD:
                // In warm temps, a sock + standard footwear is usually enough.
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, false));
                }
                break;

            case COOL:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, true));
                }
                break;

            case COLD:
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.INSULATED_SOCK, true));
                slots.add(new OutfitSlot(BodyZone.FEET, LayerType.FOOTWEAR, true));
                if (wet) {
                    slots.add(new OutfitSlot(BodyZone.FEET, LayerType.RAIN, true));
                }
                break;

            case VERY_COLD:
                // Cold: socks + insulated footwear basically required.
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
            double tempF,
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
                    tempF,
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
     * Later we can adjust by slot (e.g., insulation slots weight insulation higher).
     */
    private double scoreItemForSlot(
            GearItem item,
            double tempF,
            double windMph,
            double precipIn,
            WeaponType weaponType,
            HuntingStyle style,
            OutfitSlot slot
    ) {
        double baseScore = scoreItem(item, tempF, windMph, precipIn, weaponType, style);

        // TODO: refine by slot, for example:
        //  - if slot.layerType == INSULATION, weight insulation more heavily.
        //  - if slot.layerType == RAIN/SHELL, weight waterproof/wind more heavily.
        return baseScore;
    }

    /**
     * Original per-item scoring logic (slightly adjusted to expect tempF / windMph / precipIn).
     */
    private double scoreItem(
            GearItem item,
            double tempF,
            double windMph,
            double precipIn,
            WeaponType weaponType,
            HuntingStyle huntingStyle
    ) {
        GearAttributes attrs = item.getAttributes();
        if (attrs == null) return 0.0;

        // Temperature component
        double tempScore;
        if (tempF < attrs.getComfortTempMinC()) {
            tempScore = attrs.getInsulationLevel() * 0.5;
        } else if (tempF > attrs.getComfortTempMaxC()) {
            tempScore = attrs.getBreathabilityLevel() * 0.5;
        } else {
            tempScore = 8.0;
        }

        // Wind component
        double windScore = (windMph > 20.0)
                ? attrs.getWindProofLevel()
                : attrs.getWindProofLevel() * 0.3;

        // Precip component (rain + snow combined)
        double precipScore = (precipIn > 0.5)
                ? attrs.getWaterProofLevel()
                : attrs.getWaterProofLevel() * 0.2;

        // Style bonus
        double styleBonus = 0.0;
        switch (huntingStyle) {
            case TREESTAND:
                styleBonus += (10 - attrs.getNoiseLevel()) * 0.5;
                styleBonus += attrs.getInsulationLevel() * 0.3;
                break;
            case SPOT_AND_STALK:
                styleBonus += attrs.getMobilityLevel() * 0.6;
                styleBonus += attrs.getBreathabilityLevel() * 0.3;
                break;
            case GROUND_BLIND:
            case STILL_HUNTING:
                styleBonus += (10 - attrs.getNoiseLevel()) * 0.3;
                styleBonus += attrs.getInsulationLevel() * 0.3;
                break;
            default:
                break;
        }

        // Weapon bonus (bow -> extra quiet)
        double weaponBonus = 0.0;
        if (weaponType == WeaponType.BOW) {
            weaponBonus += (10 - attrs.getNoiseLevel()) * 0.4;
        }

        double score = tempScore + windScore + precipScore + styleBonus + weaponBonus;

        Log.d(TAG, "scoreItem: " + item.getName() + " score=" + score
                + " (tempF=" + tempF
                + ", wind=" + windMph
                + ", precipIn=" + precipIn + ")");
        return score;
    }

    // Alternate approach:
    //  - Introduce a RecommendationResult object that also carries "notes" back to the UI
    //    (e.g., "You don't own any insulation for LEGS", "Consider leaving bibs off on hike in").
}
