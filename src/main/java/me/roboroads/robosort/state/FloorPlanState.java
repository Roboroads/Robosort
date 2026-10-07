package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.CloseConnection;
import me.roboroads.gearth.gpackets.incoming.FloorHeightMap;
import me.roboroads.gearth.gpackets.incoming.RoomReady;
import me.roboroads.gearth.gpackets.outgoing.Quit;
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

    @Intercept
    private void handleFloorHeightMap(FloorHeightMap floorHeightMap) {
        String[] split = floorHeightMap.floorPlan().split("\r");
        floorPlan = new char[split[0].length()][split.length];
        for (int x = 0; x < split[0].length(); x++) {
            for (int y = 0; y < split.length; y++) {
                floorPlan[x][y] = split[y].charAt(x);
            }
        }
        isReady = true;
    }

    @Intercept({CloseConnection.class, RoomReady.class, Quit.class})
    private void onLeaveRoom(HMessage hMessage) {
        reset();
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
