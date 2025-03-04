package me.roboroads.robosort.data;

import gearth.extensions.parsers.HFloorItem;

public class WiredFurni {
    public final HFloorItem floorItem;
    public final WiredBoxType wiredBoxType;

    public WiredFurni(HFloorItem floorItem, WiredBoxType wiredBoxType) {
        this.floorItem = floorItem;
        this.wiredBoxType = wiredBoxType;
    }

}
