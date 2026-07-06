package me.roboroads.robosort.util;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Movement;

import java.util.LinkedList;
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

    private Mover(Robosort ext) {
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> queue.clear());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> queue.clear());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> queue.clear());

        processQueue();
    }

    private void processQueue() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            Movement currentMovement;
            synchronized (lock) {
                currentMovement = queue.poll();
            }

            if (currentMovement != null) {
                ext.sendToServer(new HPacket("WiredSetObjectVariableValue", HMessage.Direction.TOSERVER, 0, currentMovement.furniId, currentMovement.variableId, currentMovement.value));
            }
        }, 5000, 350, TimeUnit.MILLISECONDS);
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

}
