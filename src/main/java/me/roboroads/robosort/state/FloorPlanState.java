package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/FloorState.java">Source</a>
 */
public class FloorPlanState {
    private static FloorPlanState instance;
    private final Robosort ext;
    private char[][] floorplan = null;
    private boolean isReady = false;

    private FloorPlanState(Robosort ext) {
        reset();
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::handleFloorHeightMap);

        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
    }

    private void handleFloorHeightMap(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.skip("bi");
        String[] split = packet.readString().split("\r");
        floorplan = new char[split[0].length()][split.length];
        for (int x = 0; x < split[0].length(); x++) {
            for (int y = 0; y < split.length; y++) {
                floorplan[x][y] = split[y].charAt(x);
            }
        }
        isReady = true;
    }

    private void reset() {
        isReady = false;
        floorplan = null;
    }

    public int getTileHeight(int x, int y) {
        try {
            char height = floorplan[x][y];
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

    public static FloorPlanState getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return instance;
    }

    public static FloorPlanState getInstance(Robosort ext) {
        if (instance == null) {
            instance = new FloorPlanState(ext);
        }

        return instance;
    }
}
