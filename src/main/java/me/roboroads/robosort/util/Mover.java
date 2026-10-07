package me.roboroads.robosort.util;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.CloseConnection;
import me.roboroads.gearth.gpackets.incoming.RoomReady;
import me.roboroads.gearth.gpackets.model.enums.WiredVariableAction;
import me.roboroads.gearth.gpackets.model.enums.WiredVariableTarget;
import me.roboroads.gearth.gpackets.outgoing.Quit;
import me.roboroads.gearth.gpackets.outgoing.WiredSetObjectVariableValue;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Movement;
import me.roboroads.robosort.data.WiredFurni;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mover {
    private static final String ALTITUDE_VARIABLE_ID = "-123";
    private static final String ROTATION_VARIABLE_ID = "-122";
    private static Mover INSTANCE;
    private final Robosort ext;

    private final LinkedList<Movement> queue = new LinkedList<>();
    private final Object lock = new Object();

    // Set on the tick that sends a rotation, used up by the next tick
    private Restore restore;

    private Mover(Robosort ext) {
        this.ext = ext;

        processQueue();
    }

    @Intercept({CloseConnection.class, RoomReady.class, Quit.class})
    private void onLeaveRoom(HMessage hMessage) {
        synchronized (lock) {
            queue.clear();
            restore = null;
        }
    }

    private void processQueue() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                Movement currentMovement;
                synchronized (lock) {
                    currentMovement = nextMovement();
                }

                if (currentMovement != null) {
                    ext.sendToServer(new WiredSetObjectVariableValue(WiredVariableTarget.FURNI, currentMovement.furniId, currentMovement.variableId, currentMovement.value, WiredVariableAction.SET_VALUE).toPacket());
                }
            } catch (RuntimeException e) {
                // An exception would cancel every later tick
                e.printStackTrace();
            }
        }, 5000, 350, TimeUnit.MILLISECONDS);
    }

    // Picks the one movement to send this tick, or null. Must hold the lock.
    private Movement nextMovement() {
        if (restore != null) {
            Restore rotated = restore;
            restore = null;

            // Habbo puts a rotated box on top of its stack, so its altitude goes right after the rotation
            Movement queuedAltitude = take(rotated.furniId, ALTITUDE_VARIABLE_ID);
            if (queuedAltitude != null) {
                return queuedAltitude;
            }
            if (rotated.altitude != null) {
                return new Movement(rotated.furniId, ALTITUDE_VARIABLE_ID, rotated.altitude);
            }
        }

        Movement movement;
        while ((movement = queue.poll()) != null) {
            if (movement.variableId.equals(ALTITUDE_VARIABLE_ID)) {
                Movement rotation = take(movement.furniId, ROTATION_VARIABLE_ID);
                if (rotation != null && !isNoOp(rotation)) {
                    // The rotation goes first; the next tick takes this altitude back out
                    queue.offer(movement);
                    return rotate(rotation);
                }
            }

            if (isNoOp(movement)) {
                continue;
            }

            return movement.variableId.equals(ROTATION_VARIABLE_ID) ? rotate(movement) : movement;
        }

        return null;
    }

    private Movement rotate(Movement rotation) {
        Integer altitude = null;
        WiredFurni wiredFurni = ext.wiredState.getCurrent(rotation.furniId);
        if (wiredFurni != null) {
            List<WiredFurni> stack = ext.wiredState.wiredOnTile(wiredFurni.getX(), wiredFurni.getY());
            boolean onTop = stack.get(stack.size() - 1) == wiredFurni;
            if (!onTop) {
                altitude = (int) (wiredFurni.getZ() * 100);
            }
        }

        restore = new Restore(rotation.furniId, altitude);
        return rotation;
    }

    private Movement take(int furniId, String variableId) {
        Iterator<Movement> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Movement movement = iterator.next();
            if (movement.furniId == furniId && movement.variableId.equals(variableId)) {
                iterator.remove();
                return movement;
            }
        }
        return null;
    }

    private boolean isNoOp(Movement movement) {
        WiredFurni wiredFurni = ext.wiredState.getCurrent(movement.furniId);
        if (wiredFurni == null) {
            return false;
        }

        if (movement.variableId.equals(ROTATION_VARIABLE_ID)) {
            return wiredFurni.getRotation() == movement.value;
        }
        return Math.abs((int) (wiredFurni.getZ() * 100) - movement.value) <= 1; // tolerate 1 unit precision
    }

    public static synchronized Mover I() {
        if (INSTANCE == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return INSTANCE;
    }

    public static synchronized Mover I(Robosort ext) {
        if (INSTANCE == null) {
            INSTANCE = new Mover(ext);
        }

        return INSTANCE;
    }

    public void queueAltitude(int furniId, int altitude) {
        queueMovement(new Movement(furniId, ALTITUDE_VARIABLE_ID, altitude));
    }

    public void queueRotation(int furniId, int value) {
        queueMovement(new Movement(furniId, ROTATION_VARIABLE_ID, value));
    }

    public void queueMovement(Movement movement) {
        synchronized (lock) {
            tryDequeue(movement.furniId, movement.variableId);
            queue.offer(movement);
        }
    }

    private void tryDequeue(int furniId, String variableId) {
        synchronized (lock) {
            queue.removeIf(movement -> movement.furniId == furniId && movement.variableId.equals(variableId));
        }
    }

    private static class Restore {
        final int furniId;
        // Altitude from before the rotation, null when the box was already on top
        final Integer altitude;

        Restore(int furniId, Integer altitude) {
            this.furniId = furniId;
            this.altitude = altitude;
        }
    }
}
