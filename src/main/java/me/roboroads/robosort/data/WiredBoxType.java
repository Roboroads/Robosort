package me.roboroads.robosort.data;

import java.util.Arrays;
import java.util.List;

public enum WiredBoxType {
    TRIGGER, SELECTOR, FILTER, CONDITION, ADDON, VARIABLE_EXTRA, VARIABLE, EFFECT;

    public String toString() {
        // Pretty-print enum name: split on underscores, capitalize each word, and pluralize
        String[] parts = name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) {
                sb.append(" ");
            }
        }
        sb.append("s");
        return sb.toString();
    }

    public static List<WiredBoxType> defaultValues() {
        // Default sorting order
        return Arrays.asList(TRIGGER, SELECTOR, FILTER, CONDITION, ADDON, EFFECT, VARIABLE, VARIABLE_EXTRA);
    }
}
