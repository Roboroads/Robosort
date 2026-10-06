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
            int targetValue = (targetLeft ^ isException) ? 0 : 1;
            int currentValue = wf.getRotation();
            if (currentValue != targetValue) {
                // Mover puts the box back at its altitude after the rotation
                ext.mover.queueRotation(wf.id, targetValue);
                rotated++;
            }
        }
        return rotated;
    }
}
