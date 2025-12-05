package me.roboroads.robosort.util;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredBoxType;
import me.roboroads.robosort.data.WiredFurni;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class HabboUtil {
    private static HabboUtil INSTANCE;
    private final Robosort ext;

    private HabboUtil(Robosort ext) {
        this.ext = ext;
    }

    public static void init(Robosort ext) {
        INSTANCE = new HabboUtil(ext);
    }

    public static HabboUtil I() {
        return INSTANCE;
    }

    // Common utilities previously in Robosort
    public boolean checkCanMove(boolean callOut) {
        if (!ext.floorPlanState.isReady()) {
            if (callOut) {
                sendChat("Floorplan is not loaded - re-enter the room to load it.");
            }
            return false;
        }

        if (!ext.wiredState.isReady()) {
            if (callOut) {
                sendChat("There are no wired boxes in the room - re-enter the room if you think that's false.");
            }
            return false;
        }

        if (!ext.furniDataTools.isReady()) {
            if (callOut) {
                sendChat("Furnidata is not loaded, wait a minute or restart Habbo.");
            }
            return false;
        }

        if (!ext.roomPermissionState.canModifyWired()) {
            if (callOut) {
                sendChat("You are not allowed to modify wired here.");
            }
            return false;
        }

        return true;
    }

    public void sendChat(String text) {
        ext.sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, -1, "[ROBOSORT] " + text, 0, 1, 0, -1));
    }

    // Sorting helper
    public void sort(int x, int y) {
        // The ListView items hold the reversed order; reverse again to get the actual order
        List<WiredBoxType> unreversedSortOrder = Util.reverse(ext.sortOrderListView.getItems());
        List<WiredFurni> stackState = ext.wiredState.wiredOnTile(x, y).stream().sorted(Comparator.comparingInt(wiredFurni -> unreversedSortOrder.indexOf(wiredFurni.wiredBoxType))).collect(Collectors.toList());

        int currentAltitude = ext.floorPlanState.getTileHeight(x, y) * 100;
        for (WiredFurni wiredFurni : stackState) {
            int currentZ = (int) (wiredFurni.floorItem.getTile().getZ() * 100);
            if (Math.abs(currentZ - currentAltitude) > 1) { // tolerate 1 unit precision
                ext.mover.queue(wiredFurni.floorItem.getId(), currentAltitude);
            }
            currentAltitude += wiredFurni.height;
        }
    }
}
