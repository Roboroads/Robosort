package me.roboroads.robosort.data;

import gearth.extensions.parsers.HFloorItem;

import java.util.*;
import java.util.Map.Entry;

public class WiredFurni {
    private static final int BIG_BOX_HEIGHT = 100;
    private static final int NORMAL_BOX_HEIGHT = 65;
    private static final int SMALL_BOX_HEIGHT = 37;

    private static final Map<String, WiredBoxType> furniClassPrefixTypeMap = new LinkedHashMap<String, WiredBoxType>() {{
        put("wf_trg_", WiredBoxType.TRIGGER);
        put("wf_slc_", WiredBoxType.SELECTOR);
        put("wf_xtra_filter_", WiredBoxType.FILTER);
        put("wf_cnd_", WiredBoxType.CONDITION);
        put("wf_xtra_", WiredBoxType.ADDON);
        put("wf_act_", WiredBoxType.EFFECT);
    }};

    private static final Map<String, ExceptionDetails> exceptions = new HashMap<String, ExceptionDetails>() {{
        put("wf_xtra_text_output_variable", new ExceptionDetails(WiredBoxType.EFFECT, SMALL_BOX_HEIGHT));
        put("wf_xtra_text_input_variable", new ExceptionDetails(WiredBoxType.EFFECT, SMALL_BOX_HEIGHT));
    }};

    public final HFloorItem floorItem;
    public final WiredBoxType wiredBoxType;
    public final String furniClassName;
    public final int height;

    public WiredFurni(HFloorItem floorItem, String furniClassName) {
        if (!isWiredFurni(furniClassName)) {
            throw new IllegalArgumentException("Not a wired furni: " + furniClassName);
        }

        this.floorItem = floorItem;
        this.furniClassName = furniClassName;
        this.wiredBoxType = getType(furniClassName);
        this.height = getHeight();
    }

    private int getHeight() {
        if (exceptions.containsKey(this.furniClassName)) {
            return exceptions.get(this.furniClassName).height;
        }

        switch (this.wiredBoxType) {
            case SELECTOR:
            case FILTER:
                return BIG_BOX_HEIGHT;
            case ADDON:
                return SMALL_BOX_HEIGHT;
            default:
                return NORMAL_BOX_HEIGHT;
        }
    }

    public static boolean isWiredFurni(String furniClassName) {
        return getType(furniClassName) != null;
    }

    private static WiredBoxType getType(String furniName) {
        if (exceptions.containsKey(furniName)) {
            return exceptions.get(furniName).type;
        }

        for (Entry<String, WiredBoxType> entry : furniClassPrefixTypeMap.entrySet()) {
            if (furniName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static class ExceptionDetails {
        public final WiredBoxType type;
        public final int height;

        ExceptionDetails(WiredBoxType type, int height) {
            this.type = type;
            this.height = height;
        }
    }
}
