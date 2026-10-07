package me.roboroads.robosort.features;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.ObjectAdd;
import me.roboroads.gearth.gpackets.incoming.ObjectRemove;
import me.roboroads.gearth.gpackets.incoming.ObjectUpdate;
import me.roboroads.gearth.gpackets.incoming.sub.furni.FloorItem;
import me.roboroads.gearth.gpackets.outgoing.BuildersClubPlaceRoomItem;
import me.roboroads.gearth.gpackets.outgoing.MoveObject;
import me.roboroads.gearth.gpackets.outgoing.PickupObject;
import me.roboroads.gearth.gpackets.outgoing.PlaceObject;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

public class SortOnAction {
    private final Robosort ext;

    private LocalDateTime furniRemoved;
    private LocalDateTime furniAdded;
    private LocalDateTime furniMoved;

    public SortOnAction(Robosort ctx) {
        this.ext = ctx;
        LocalDateTime init = LocalDateTime.now().minusSeconds(1);
        this.furniRemoved = init;
        this.furniAdded = init;
        this.furniMoved = init;
    }

    @Intercept({PlaceObject.class, BuildersClubPlaceRoomItem.class})
    public void onPlaceObject(HMessage hMessage) {
        furniAdded = LocalDateTime.now();
    }

    @Intercept(MoveObject.class)
    public void onMoveObject(HMessage hMessage) {
        furniMoved = LocalDateTime.now();
    }

    @Intercept(PickupObject.class)
    public void onPickupObject(HMessage hMessage) {
        furniRemoved = LocalDateTime.now();
    }

    @Intercept
    public void onObjectAdd(ObjectAdd objectAdd) {
        handleAddOrUpdate(objectAdd.object(), furniAdded);
    }

    @Intercept
    public void onObjectUpdate(ObjectUpdate objectUpdate) {
        handleAddOrUpdate(objectUpdate.object(), furniMoved);
    }

    @Intercept
    public void onObjectRemove(ObjectRemove objectRemove) {
        long msDiff = Duration.between(furniRemoved, LocalDateTime.now()).toMillis();
        if (ext.sortOnActionEnabled() && HabboUtil.I().checkCanMove(false) && msDiff < 500) {
            int furniId = Integer.parseInt(objectRemove.furniId());
            WiredFurni wiredFurni = ext.wiredState.get(furniId);
            if (wiredFurni != null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        HabboUtil.I().sort(wiredFurni.getX(), wiredFurni.getY());
                    }
                }, 10);
            }
        }
    }

    private void handleAddOrUpdate(FloorItem floorItem, LocalDateTime lastAction) {
        long msDiff = Duration.between(lastAction, LocalDateTime.now()).toMillis();
        if (ext.sortOnActionEnabled() && HabboUtil.I().checkCanMove(false) && msDiff < 500) {
            String furniClassName = ext.furniDataTools.getFloorItemClassName(floorItem.furniClassId());

            if (WiredFurni.isWiredFurni(furniClassName)) {
                int furniId = floorItem.furniId();
                int x = floorItem.x();
                int y = floorItem.y();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        HabboUtil.I().sort(x, y);

                        WiredFurni previousPosition = ext.wiredState.getPrevious(furniId);
                        if (previousPosition != null && (x != previousPosition.getX() || y != previousPosition.getY())) {
                            HabboUtil.I().sort(previousPosition.getX(), previousPosition.getY());
                        }
                    }
                }, 10);
            }
        }
    }
}
