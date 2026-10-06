package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.CloseConnection;
import me.roboroads.gearth.gpackets.incoming.ObjectAdd;
import me.roboroads.gearth.gpackets.incoming.ObjectRemove;
import me.roboroads.gearth.gpackets.incoming.ObjectUpdate;
import me.roboroads.gearth.gpackets.incoming.Objects;
import me.roboroads.gearth.gpackets.incoming.RoomReady;
import me.roboroads.gearth.gpackets.incoming.SlideObjectBundle;
import me.roboroads.gearth.gpackets.incoming.WiredMovements;
import me.roboroads.gearth.gpackets.incoming.sub.furni.FloorItem;
import me.roboroads.gearth.gpackets.incoming.sub.furni.SlideObject;
import me.roboroads.gearth.gpackets.incoming.sub.wired.FurniMove;
import me.roboroads.gearth.gpackets.incoming.sub.wired.WiredMovement;
import me.roboroads.gearth.gpackets.outgoing.Quit;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Tile;
import me.roboroads.robosort.data.WiredFurni;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/FloorState.java">Source</a>
 */
public class WiredState {
    private static WiredState INSTANCE;
    private final Robosort ext;
    private Map<Integer, WiredFurni> currentWired;
    private Map<Integer, WiredFurni> previousWired;

    private WiredState(Robosort ext) {
        reset();
        this.ext = ext;
    }

    public static WiredState I() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WiredState has not been initialized");
        }

        return INSTANCE;
    }

    public static WiredState I(Robosort ext) {
        if (INSTANCE == null) {
            INSTANCE = new WiredState(ext);
        }

        return INSTANCE;
    }

    private void reset() {
        currentWired = new HashMap<>();
        previousWired = new HashMap<>();
    }

    @Intercept({CloseConnection.class, RoomReady.class, Quit.class})
    private void onLeaveRoom(HMessage hMessage) {
        reset();
    }

    @Intercept
    private void handleObjects(Objects objects) {
        objects.objects().forEach(this::maybeAdd);
    }

    @Intercept
    private void handleObjectAdd(ObjectAdd objectAdd) {
        maybeAdd(objectAdd.object());
    }

    @Intercept
    private void handleObjectRemove(ObjectRemove objectRemove) {
        int furniId = Integer.parseInt(objectRemove.furniId());
        WiredFurni removedWired = currentWired.remove(furniId);
        if (removedWired != null) {
            this.previousWired.put(furniId, removedWired);
        }
    }

    @Intercept
    private void handleObjectUpdate(ObjectUpdate objectUpdate) {
        maybeAdd(objectUpdate.object());
    }

    @Intercept
    private void handleSlideObjectBundle(SlideObjectBundle slideObjectBundle) {
        for (SlideObject slideObject : slideObjectBundle.objects()) {
            processMove(slideObject.furniId(), slideObjectBundle.newX(), slideObjectBundle.newY(), slideObject.newZ());
        }
    }

    @Intercept
    private void handleWiredMovements(WiredMovements wiredMovements) {
        for (WiredMovement movement : wiredMovements.movements()) {
            if (!(movement instanceof FurniMove)) {
                continue;
            }

            FurniMove furniMove = (FurniMove) movement;

            processMove(furniMove.furniId(), furniMove.targetX(), furniMove.targetY(), furniMove.targetZ());
        }
    }

    private void maybeAdd(FloorItem floorItem) {
        String furniName = ext.furniDataTools.getFloorItemClassName(floorItem.furniClassId());

        if (WiredFurni.isWiredFurni(furniName)) {
            WiredFurni currentWiredFurni = currentWired.get(floorItem.furniId());
            if (currentWiredFurni != null) {
                previousWired.put(floorItem.furniId(), currentWiredFurni);
            }

            currentWired.put(floorItem.furniId(), new WiredFurni(floorItem, furniName));
        }
    }

    private void processMove(int furniId, int newX, int newY, String newZ) {
        WiredFurni wiredFurni = currentWired.get(furniId);
        if (wiredFurni != null) {
            wiredFurni.moveTo(newX, newY, Double.parseDouble(newZ));
        }
    }

    public List<WiredFurni> wiredOnTile(int x, int y) {
        return currentWired.values().stream().filter(wiredFurni -> wiredFurni.getX() == x && wiredFurni.getY() == y).sorted(Comparator.comparingDouble(WiredFurni::getZ)).collect(Collectors.toList());
    }

    public WiredFurni get(int id) {
        WiredFurni wiredFurni = currentWired.get(id);
        if (wiredFurni == null) {
            wiredFurni = previousWired.get(id);
        }
        return wiredFurni;
    }

    public WiredFurni getCurrent(int id) {
        return currentWired.get(id);
    }

    public WiredFurni getPrevious(int id) {
        return previousWired.get(id);
    }

    public boolean isReady() {
        return !currentWired.isEmpty();
    }

    // Returns unique (x,y) tile coordinates that contain at least one wired furni
    public List<Tile> getAllWiredTiles() {
        Set<String> seen = new HashSet<>();
        List<Tile> tiles = new ArrayList<>();
        for (WiredFurni wf : currentWired.values()) {
            int x = wf.getX();
            int y = wf.getY();
            String key = x + "," + y;
            if (seen.add(key)) {
                tiles.add(new Tile(x, y));
            }
        }
        return tiles;
    }
}
