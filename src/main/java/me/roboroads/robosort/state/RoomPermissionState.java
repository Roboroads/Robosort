package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.CloseConnection;
import me.roboroads.gearth.gpackets.incoming.RoomReady;
import me.roboroads.gearth.gpackets.incoming.WiredPermissions;
import me.roboroads.gearth.gpackets.incoming.YouAreController;
import me.roboroads.gearth.gpackets.incoming.YouAreNotController;
import me.roboroads.gearth.gpackets.outgoing.Quit;
import me.roboroads.robosort.Robosort;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/RoomPermissions.java">Source</a>
 */
public class RoomPermissionState {
    private static RoomPermissionState INSTANCE;
    private final Robosort ext;

    private boolean canModifyWired;
    private boolean canMoveFurni;

    private RoomPermissionState(Robosort ext) {
        this.ext = ext;
    }

    public static RoomPermissionState I() {
        if (INSTANCE == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return INSTANCE;
    }

    public static RoomPermissionState I(Robosort ext) {
        if (INSTANCE == null) {
            INSTANCE = new RoomPermissionState(ext);
        }

        return INSTANCE;
    }

    @Intercept
    private void onWiredPermissions(WiredPermissions wiredPermissions) {
        canModifyWired = wiredPermissions.canModify();
    }

    @Intercept(YouAreController.class)
    private void onYouAreController(HMessage msg) {
        canMoveFurni = true;
    }

    @Intercept(YouAreNotController.class)
    private void onYouAreNotController(HMessage msg) {
        canMoveFurni = false;
    }

    @Intercept({CloseConnection.class, RoomReady.class, Quit.class})
    private void onLeaveRoom(HMessage msg) {
        clear();
    }

    public void clear() {
        canModifyWired = false;
        canMoveFurni = false;
    }

    public boolean canModifyWired() {
        return canModifyWired;
    }

    public boolean canMoveFurni() {
        return canMoveFurni;
    }
}
