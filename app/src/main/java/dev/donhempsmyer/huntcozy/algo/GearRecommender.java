package dev.donhempsmyer.huntcozy.algo;




import android.util.Log;

import dev.donhempsmyer.huntcozy.data.model.GearItem;
import dev.donhempsmyer.huntcozy.data.model.HuntingStyle;
import dev.donhempsmyer.huntcozy.data.model.WeaponType;
import dev.donhempsmyer.huntcozy.data.model.weather.CurrentWeather;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GearRecommender encapsulates logic for suggesting gear based on
 * weather, hunting style, weapon type, and closet contents.
 *
 * v1: simple scoring system, with room to grow.
 */
public class GearRecommender {

    private static final String TAG = "GearRecommender";

    // Simple placeholder version (not used once we move to closet-based recs).
    public List<GearItem> recommend(CurrentWeather current,
                                    WeaponType weaponType,
                                    HuntingStyle huntingStyle) {
        Log.d(TAG, "recommend(simple): temp=" + (current != null ? current.temperature2m : null)
                + " weapon=" + weaponType + " style=" + huntingStyle);
        return new ArrayList<>();
    }

    // 🔹 THIS is the one we’ll call from HomeViewModel
    public List<GearItem> recommendFromCloset(
            List<GearItem> closet,
            CurrentWeather current,
            WeaponType weaponType,
            HuntingStyle huntingStyle) {

        Log.d(TAG, "recommendFromCloset: closetSize=" +
                (closet != null ? closet.size() : 0));

        if (closet == null || closet.isEmpty() || current == null) {
            Log.w(TAG, "recommendFromCloset: no closet or weather");
            return new ArrayList<>();
        }

        double temp = current.temperature2m;
        double wind = current.windSpeed10m;
        double precip = current.precipitation + current.snowfall;

        List<ScoredItem> scored = new ArrayList<>();
        for (GearItem item : closet) {
            double score = scoreItem(item, temp, wind, precip, weaponType, huntingStyle);
            scored.add(new ScoredItem(item, score));
        }

        scored.sort(new Comparator<ScoredItem>() {
            @Override
            public int compare(ScoredItem o1, ScoredItem o2) {
                return Double.compare(o2.score, o1.score); // high first
            }
        });

        int maxItems = 10;
        List<GearItem> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < maxItems; i++) {
            result.add(scored.get(i).item);
        }

        Log.d(TAG, "recommendFromCloset: returning " + result.size() + " items");
        return result;
    }

    private double scoreItem(GearItem item,
                             double temp,
                             double wind,
                             double precip,
                             WeaponType weaponType,
                             HuntingStyle huntingStyle) {

        if (item == null || item.getAttributes() == null) return 0.0;

        var attrs = item.getAttributes();

        double tempScore;
        if (temp < attrs.getComfortTempMinC()) {
            tempScore = attrs.getInsulationLevel() * 0.5;
        } else if (temp > attrs.getComfortTempMaxC()) {
            tempScore = attrs.getBreathabilityLevel() * 0.5;
        } else {
            tempScore = 8.0;
        }

        double windScore = (wind > 20.0)
                ? attrs.getWindProofLevel()
                : attrs.getWindProofLevel() * 0.3;

        double precipScore = (precip > 0.5)
                ? attrs.getWaterProofLevel()
                : attrs.getWaterProofLevel() * 0.2;

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

        double weaponBonus = 0.0;
        if (weaponType == WeaponType.BOW) {
            weaponBonus += (10 - attrs.getNoiseLevel()) * 0.4;
        }

        double score = tempScore + windScore + precipScore + styleBonus + weaponBonus;
        Log.d(TAG, "scoreItem: " + item.getName() + " score=" + score);
        return score;
    }

    private static class ScoredItem {
        final GearItem item;
        final double score;

        ScoredItem(GearItem item, double score) {
            this.item = item;
            this.score = score;
        }
    }

    // Alternate approach:
    // - Make different scoring strategies for early/mid/late season and let
    //   the user choose which profile they want to use.
}
