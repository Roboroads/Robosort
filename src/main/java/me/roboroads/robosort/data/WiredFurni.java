package me.roboroads.robosort.data;

import gearth.extensions.parsers.HFloorItem;

public class WiredFurni {
    public final HFloorItem floorItem;
    public final WiredBoxType wiredBoxType;

    public WiredFurni(HFloorItem floorItem, WiredBoxType wiredBoxType) {
        this.floorItem = floorItem;
        this.wiredBoxType = wiredBoxType;
    }

    public enum WiredBoxType {
        TRIGGER("wf_trg_", 1, 65),
        SELECTOR("wf_slc_", 2, 100),
        FILTER("wf_xtra_filter_", 3, 100),
        CONDITION("wf_cnd_", 4, 65),
        ADDON("wf_xtra_", 5, 37),
        EFFECT("wf_act_", 6, 65);

        public final int sortNumber;
        public final int height;
        private final String startsWith;

        WiredBoxType(String startsWith, int sortNumber, int height) {
            this.startsWith = startsWith;
            this.sortNumber = sortNumber;
            this.height = height;
        }

        public static WiredBoxType fromFurniName(String furniName) {
            for (WiredBoxType type : WiredBoxType.values()) {
                if (furniName.startsWith(type.startsWith)) {
                    return type;
                }
            }
            return null;
        }
    }
}
