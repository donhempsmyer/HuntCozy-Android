package dev.donhempsmyer.huntcozy.data.model;


/**
 * LayerType describes the role of the gear in a layering system.
 * Not all zones use all types (e.g. socks won't have BASE/MID).
 */
public enum LayerType {
    BASE,
    MID,
    INSULATION,
    SHELL,        // windproof / weather shell
    RAIN,         // true rain gear (can be layered over shell)
    FOOTWEAR,
    INSULATED_FOOTWEAR,
    SOCK,
    INSULATED_SOCK,
    GLOVE,
    INSULATED_GLOVE,
    HEADGEAR,
    INSULATED_HEADGEAR,
    ACCESSORY,
}