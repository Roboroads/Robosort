package me.roboroads.robosort.util;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Movement;

import java.util.LinkedList;
import java.util.concurrent.*;

public class Mover {
    private static final String ALTITUDE_VARIABLE_ID = "-123";
    private static Mover instance;
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
        executorService.scheduleAtFixedRate(
          () -> {
              Movement currentMovement;
              synchronized (lock) {
                  currentMovement = queue.poll();
              }

              if (currentMovement != null) {
                  ext.sendToServer(new HPacket(
                    "WiredSetObjectVariableValue",
                    HMessage.Direction.TOSERVER,
                    0,
                    currentMovement.furniId,
                    ALTITUDE_VARIABLE_ID,
                    currentMovement.altitude
                  ));
              }
          }, 5000, 350, TimeUnit.MILLISECONDS
        );
    }

    public static synchronized Mover getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RoomPermissionState has not been initialized");
        }

        return instance;
    }

    public static synchronized Mover initialize(Robosort ext) {
        if (instance == null) {
            instance = new Mover(ext);
        }

        return instance;
    }

    public void queue(int furniId, int altitude) {
        queue(new Movement(furniId, altitude));
    }

    public void queue(Movement movement) {
        synchronized (lock) {
            tryDequeue(movement.furniId);
            queue.offer(movement);
        }
    }

    private void tryDequeue(int furniId) {
        synchronized (lock) {
            queue.removeIf(movement -> movement.furniId == furniId);
        }
    }

}
