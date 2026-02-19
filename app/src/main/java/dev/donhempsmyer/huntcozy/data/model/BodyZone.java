package dev.donhempsmyer.huntcozy.data.model;


/**
 * BodyZone indicates where on the body this gear is primarily used.
 * This lets the recommender ensure head-to-toe coverage.
 */
public enum BodyZone {
    HEAD,
    FACE,
    NECK,
    TORSO,
    LEGS,
    HANDS,
    FEET,
    PACK,
    WEAPON_ACCESSORY
}