package me.roboroads.robosort.data;

import gearth.extensions.parsers.HFloorItem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WiredFurni {
    private static final int SELECTOR_HEIGHT = 100;
    private static final int DEFAULT_BOX_HEIGHT = 65;
    private static final int ADDON_BOX_HEIGHT = 37;
    private static final int VARIABLE_BOX_HEIGHT = 120;
    private static final int VARIABLE_ADDON_BOX_HEIGHT = 75;

    private static final Map<String, WiredBoxType> furniClassPrefixTypeMap = new LinkedHashMap<String, WiredBoxType>() {{
        put("wf_trg_", WiredBoxType.TRIGGER);
        put("wf_slc_", WiredBoxType.SELECTOR);
        put("wf_xtra_filter_", WiredBoxType.FILTER);
        put("wf_cnd_", WiredBoxType.CONDITION);
        put("wf_xtra_var_", WiredBoxType.VARIABLE_EXTRA);
        put("wf_var_", WiredBoxType.VARIABLE);
        put("wf_xtra_", WiredBoxType.ADDON);
        put("wf_act_", WiredBoxType.EFFECT);
    }};

    private static final Map<String, ExceptionDetails> exceptions = new HashMap<String, ExceptionDetails>() {{
        put("wf_xtra_text_output_variable", new ExceptionDetails(WiredBoxType.EFFECT, ADDON_BOX_HEIGHT));
        put("wf_xtra_text_input_variable", new ExceptionDetails(WiredBoxType.EFFECT, ADDON_BOX_HEIGHT));
        put("wf_ltdproto_act_toggle_state", new ExceptionDetails(WiredBoxType.EFFECT, SELECTOR_HEIGHT));
        put("wf_proto_cnd_trggrer_on_frn", new ExceptionDetails(WiredBoxType.CONDITION, SELECTOR_HEIGHT));
        put("wf_proto_trg_at_given_time", new ExceptionDetails(WiredBoxType.TRIGGER, SELECTOR_HEIGHT));
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
                return SELECTOR_HEIGHT;
            case ADDON:
                return ADDON_BOX_HEIGHT;
            case VARIABLE:
                return VARIABLE_BOX_HEIGHT;
            case VARIABLE_EXTRA:
                return VARIABLE_ADDON_BOX_HEIGHT;
            default:
                return DEFAULT_BOX_HEIGHT;
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
