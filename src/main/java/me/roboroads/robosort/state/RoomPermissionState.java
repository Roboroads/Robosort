package me.roboroads.robosort.state;

import gearth.protocol.HMessage;
import me.roboroads.robosort.Robosort;

/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/game/RoomPermissions.java">Source</a>
 */
public class RoomPermissionState {
    private static RoomPermissionState instance;
    private final Robosort ext;

    private boolean canModifyWired;
    private boolean canMoveFurni;

    private RoomPermissionState(Robosort ext) {
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "WiredPermissions", this::onWiredPermissions);
        ext.intercept(HMessage.Direction.TOCLIENT, "YouAreController", this::onYouAreController);
        ext.intercept(HMessage.Direction.TOCLIENT, "YouAreNotController", this::onYouAreNotController);

        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> clear());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> clear());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> clear());
    }

    private void onWiredPermissions(HMessage msg) {
        canModifyWired = msg.getPacket().readBoolean();
    }

    private void onYouAreController(HMessage msg) {
        canMoveFurni = true;
    }

    private void onYouAreNotController(HMessage msg) {
        canMoveFurni = false;
    }

    public void clear() {
        canModifyWired = false;
        canMoveFurni = false;
    }

    public static RoomPermissionState getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return instance;
    }

    public static RoomPermissionState getInstance(Robosort ext) {
        if (instance == null) {
            instance = new RoomPermissionState(ext);
        }

        return instance;
    }

    public boolean canModifyWired() {
        return canModifyWired;
    }

    public boolean canMoveFurni() {
        return canMoveFurni;
    }
}
