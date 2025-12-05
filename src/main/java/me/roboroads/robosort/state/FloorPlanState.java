package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/FloorState.java">Source</a>
 */
public class FloorPlanState {
    private static FloorPlanState INSTANCE;
    private final Robosort ext;
    private char[][] floorPlan = null;
    private boolean isReady = false;

    private FloorPlanState(Robosort ext) {
        reset();
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::handleFloorHeightMap);

        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
    }

    public static FloorPlanState I() {
        if (INSTANCE == null) {
            throw new IllegalStateException("FloorPlanState has not been initialized");
        }

        return INSTANCE;
    }

    public static FloorPlanState I(Robosort ext) {
        if (INSTANCE == null) {
            INSTANCE = new FloorPlanState(ext);
        }

        return INSTANCE;
    }

    private void handleFloorHeightMap(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.skip("bi");
        String[] split = packet.readString().split("\r");
        floorPlan = new char[split[0].length()][split.length];
        for (int x = 0; x < split[0].length(); x++) {
            for (int y = 0; y < split.length; y++) {
                floorPlan[x][y] = split[y].charAt(x);
            }
        }
        isReady = true;
    }

    private void reset() {
        isReady = false;
        floorPlan = null;
    }

    public int getTileHeight(int x, int y) {
        try {
            char height = floorPlan[x][y];
            if (height == 'x') {
                return 0;
            }
            if (Character.isDigit(height)) {
                return Character.getNumericValue(height);
            }
            return height - 'a' + 10;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    public boolean isReady() {
        return isReady;
    }
}
