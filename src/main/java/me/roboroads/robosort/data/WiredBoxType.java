package me.roboroads.robosort.data;

import java.util.Arrays;
import java.util.List;

public enum WiredBoxType {
    TRIGGER,
    SELECTOR,
    FILTER,
    CONDITION,
    ADDON,
    EFFECT;

    public String toString() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase() + "s";
    }

    public static List<WiredBoxType> defaultValues() {
        return Arrays.asList(TRIGGER, SELECTOR, FILTER, CONDITION, ADDON, EFFECT);
    }
}
