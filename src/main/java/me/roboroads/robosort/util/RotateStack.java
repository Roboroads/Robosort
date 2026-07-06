package me.roboroads.robosort.util;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.features.ForcedDirection;

import java.util.List;

public final class RotateStack {
    private RotateStack() {
    }

    public static int apply(Robosort ext, int x, int y, boolean targetLeft) {
        List<WiredFurni> stack = ext.wiredState.wiredOnTile(x, y);
        int rotated = 0;
        for (WiredFurni wf : stack) {
            boolean isException = ForcedDirection.EXCEPTIONS.contains(wf.furniClassName);
            boolean facesLeft = (wf.floorItem.getFacing().ordinal() == 0) != isException;
            if (facesLeft != targetLeft) {
                int preserveAltitude = (int) (wf.floorItem.getTile().getZ() * 100);
                ext.mover.queueRotation(wf.floorItem.getId());
                ext.mover.queueAltitude(wf.floorItem.getId(), preserveAltitude);
                rotated++;
            }
        }
        return rotated;
    }
}
