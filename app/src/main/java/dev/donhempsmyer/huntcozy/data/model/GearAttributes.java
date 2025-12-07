package dev.donhempsmyer.huntcozy.data.model;


import android.util.Log;

/**
 * GearAttributes captures performance characteristics of a piece of gear.
 * Most fields are rated 0–10 to keep things easy to tweak and learn from.
 */
public class GearAttributes {

    private static final String TAG = "GearAttributes";

    // 0–10 scale: 0 = none, 10 = extreme
    private final int insulationLevel;   // overall warmth
    private final int windProofLevel;
    private final int waterProofLevel;
    private final int breathabilityLevel;
    private final int noiseLevel;        // 0 = silent, 10 = very noisy
    private final int mobilityLevel;     // 0 = stiff, 10 = very mobile

    // Comfort temp range in °C for "typical" stationary use.
    // Recommender can adjust based on activity level and user feedback.
    private final double comfortTempMinF;
    private final double comfortTempMaxF;

    // Flags for special behavior
    private final boolean scentControl;
    private final boolean quietFaceFabric;

    // OPTIONAL: weight & bulk, 0–10 simple scales
    private final int weightLevel;
    private final int bulkLevel;

    public GearAttributes(
            int insulationLevel,
            int windProofLevel,
            int waterProofLevel,
            int breathabilityLevel,
            int noiseLevel,
            int mobilityLevel,
            double comfortTempMinF,
            double comfortTempMaxF,
            boolean scentControl,
            boolean quietFaceFabric,
            int weightLevel,
            int bulkLevel) {

        this.insulationLevel = clamp(insulationLevel);
        this.windProofLevel = clamp(windProofLevel);
        this.waterProofLevel = clamp(waterProofLevel);
        this.breathabilityLevel = clamp(breathabilityLevel);
        this.noiseLevel = clamp(noiseLevel);
        this.mobilityLevel = clamp(mobilityLevel);
        this.comfortTempMinF = comfortTempMinF;
        this.comfortTempMaxF = comfortTempMaxF;
        this.scentControl = scentControl;
        this.quietFaceFabric = quietFaceFabric;
        this.weightLevel = clamp(weightLevel);
        this.bulkLevel = clamp(bulkLevel);

        Log.d(TAG, "GearAttributes created: insulation=" + this.insulationLevel
                + " wind=" + this.windProofLevel
                + " water=" + this.waterProofLevel
                + " tempRange=" + this.comfortTempMinF + " to " + this.comfortTempMaxF);
    }

    private int clamp(int value) {
        if (value < 0) return 0;
        if (value > 10) return 10;
        return value;
    }

    public int getInsulationLevel() { return insulationLevel; }
    public int getWindProofLevel() { return windProofLevel; }
    public int getWaterProofLevel() { return waterProofLevel; }
    public int getBreathabilityLevel() { return breathabilityLevel; }
    public int getNoiseLevel() { return noiseLevel; }
    public int getMobilityLevel() { return mobilityLevel; }
    public double getComfortTempMinF() { return comfortTempMinF; }
    public double getComfortTempMaxF() { return comfortTempMaxF; }
    public boolean hasScentControl() { return scentControl; }
    public boolean hasQuietFaceFabric() { return quietFaceFabric; }
    public int getWeightLevel() { return weightLevel; }
    public int getBulkLevel() { return bulkLevel; }

    // Alternate approach:
    // - Represent attributes as a Map<GearAttributeType, Double> for fully dynamic attributes.
    //   That makes the DB schema more generic but loses some type safety and IDE help.
}
