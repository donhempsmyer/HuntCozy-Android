package dev.donhempsmyer.huntcozy.data.model;

/**
 * HuntWindow describes the time window of the hunt.
 * v1: used to choose how we interpret forecast data for recommendations.
 * v2: will drive hourly-range min/max temp, wind, and precip.
 */
public enum HuntWindow {
    MORNING_MID,   // early morning through late morning / midday
    MID_EVENING,   // midday through sunset / evening
    ALL_DAY        // full day window
}
