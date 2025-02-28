package me.roboroads.robosort.state;

import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/FloorState.java">Source</a>
 */
public class WiredState {
    private static WiredState instance;
    private final Robosort ext;
    private Map<Integer, WiredFurni> currentWired;
    private Map<Integer, WiredFurni> previousWired;

    private WiredState(Robosort ext) {
        reset();
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "Objects", this::handleObjects);

        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::handleObjectAdd);
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::handleObjectRemove);
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::handleObjectUpdate);

        ext.intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::handleSlideObjectBundle);
        ext.intercept(HMessage.Direction.TOCLIENT, "WiredFurniMove", this::handleWiredFurniMove);
        ext.intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::handleWiredMovements);

        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
    }

    private void reset() {
        currentWired = new HashMap<>();
        previousWired = new HashMap<>();
    }

    private void handleObjects(HMessage hMessage) {
        HFloorItem[] floorItems = HFloorItem.parse(hMessage.getPacket());

        Arrays.stream(floorItems).forEach(this::maybeAdd);
    }

    private void handleObjectAdd(HMessage hMessage) {
        maybeAdd(new HFloorItem(hMessage.getPacket()));
    }

    private void handleObjectRemove(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        int furniId = Integer.parseInt(packet.readString());
        WiredFurni removedWired = currentWired.remove(furniId);
        if (removedWired != null) {
            this.previousWired.put(furniId, removedWired);
        }
    }

    private void handleObjectUpdate(HMessage hMessage) {
        maybeAdd(new HFloorItem(hMessage.getPacket()));
    }

    private void handleSlideObjectBundle(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.readInteger(); // oldX
        packet.readInteger(); // oldY
        int newX = packet.readInteger();
        int newY = packet.readInteger();

        int count = packet.readInteger();
        for (int i = 0; i < count; i++) {
            int furniId = packet.readInteger();
            packet.readString(); // oldZ
            String newZ = packet.readString();

            processMove(furniId, newX, newY, newZ);
        }
    }

    private void handleWiredFurniMove(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.readInteger(); // oldX
        packet.readInteger(); // oldY
        int newX = packet.readInteger();
        int newY = packet.readInteger();
        packet.readString(); // oldZ
        String newZ = packet.readString();

        int furniId = packet.readInteger();

        processMove(furniId, newX, newY, newZ);
    }

    private void handleWiredMovements(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();

        int count = packet.readInteger();
        for (int i = 0; i < count; i++) {
            int type = packet.readInteger();
            switch (type) {
                case 0: // user
                    packet.skip("iiiissiiiii");
                    break;
                case 1: // furni
                    packet.skip("ii");
                    int newX = packet.readInteger();
                    int newY = packet.readInteger();

                    packet.skip("s");
                    String newZ = packet.readString();
                    int furniId = packet.readInteger();
                    packet.skip("ii");

                    processMove(furniId, newX, newY, newZ);
                    break;
                case 2: // wall item
                    packet.skip("iBiiiiiiiii");
                    break;
                case 3: // user direction
                    packet.skip("iii");
                    break;
            }
        }
    }

    private void maybeAdd(HFloorItem floorItem) {
        WiredFurni.WiredBoxType wiredBoxType = WiredFurni.WiredBoxType.fromFurniName(ext.furniDataTools.getFloorItemName(floorItem.getTypeId()));

        if (wiredBoxType != null) {
            WiredFurni currentWiredFurni = currentWired.get(floorItem.getId());
            if (currentWiredFurni != null) {
                previousWired.put(floorItem.getId(), currentWiredFurni);
            }

            currentWired.put(floorItem.getId(), new WiredFurni(floorItem, wiredBoxType));
        }
    }

    private void processMove(int furniId, int newX, int newY, String newZ) {
        WiredFurni wiredFurni = currentWired.get(furniId);
        if (wiredFurni != null) {
            wiredFurni.floorItem.setTile(new HPoint(newX, newY, Double.parseDouble(newZ)));
        }
    }

    public static WiredState getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return instance;
    }

    public static WiredState getInstance(Robosort ext) {
        if (instance == null) {
            instance = new WiredState(ext);
        }

        return instance;
    }

    public List<WiredFurni> wiredOnTile(int x, int y) {
        return currentWired.values()
          .stream()
          .filter(wiredFurni -> wiredFurni.floorItem.getTile().getX() == x && wiredFurni.floorItem.getTile().getY() == y)
          .sorted(Comparator.comparingDouble(wiredFurni -> wiredFurni.floorItem.getTile().getZ()))
          .collect(Collectors.toList());
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
}
