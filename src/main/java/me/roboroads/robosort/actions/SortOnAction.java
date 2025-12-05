package me.roboroads.robosort.actions;

import gearth.extensions.parsers.HFloorItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
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

        // Register interceptions here to keep Robosort minimal
        ext.intercept(HMessage.Direction.TOSERVER, "PlaceObject", m -> onPlaceObject());
        ext.intercept(HMessage.Direction.TOSERVER, "BuildersClubPlaceRoomItem", m -> onPlaceObject());
        ext.intercept(HMessage.Direction.TOSERVER, "MoveObject", m -> onMoveObject());
        ext.intercept(HMessage.Direction.TOSERVER, "PickupObject", m -> onPickupObject());
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::onObjectRemove);
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::onObjectUpdate);
    }

    public void onPlaceObject() {
        furniAdded = LocalDateTime.now();
    }

    public void onMoveObject() {
        furniMoved = LocalDateTime.now();
    }

    public void onPickupObject() {
        furniRemoved = LocalDateTime.now();
    }

    public void onObjectAdd(HMessage hMessage) {
        handleAddOrUpdate(hMessage, furniAdded);
    }

    public void onObjectUpdate(HMessage hMessage) {
        handleAddOrUpdate(hMessage, furniMoved);
    }

    public void onObjectRemove(HMessage hMessage) {
        long msDiff = Duration.between(furniRemoved, LocalDateTime.now()).toMillis();
        if (ext.sortOnActionEnabled() && HabboUtil.I().checkCanMove(false) && msDiff < 500) {
            HPacket packet = hMessage.getPacket();
            int furniId = Integer.parseInt(packet.readString());
            WiredFurni wiredFurni = ext.wiredState.get(furniId);
            if (wiredFurni != null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        HabboUtil.I().sort(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY());
                    }
                }, 10);
            }
        }
    }

    public void onObjectAddOrUpdateWithRef(HMessage hMessage, LocalDateTime lastAction) {
        handleAddOrUpdate(hMessage, lastAction);
    }

    private void handleAddOrUpdate(HMessage hMessage, LocalDateTime lastAction) {
        long msDiff = Duration.between(lastAction, LocalDateTime.now()).toMillis();
        if (ext.sortOnActionEnabled() && HabboUtil.I().checkCanMove(false) && msDiff < 500) {
            HFloorItem floorItem = new HFloorItem(hMessage.getPacket());
            String furniClassName = ext.furniDataTools.getFloorItemClassName(floorItem.getTypeId());

            if (WiredFurni.isWiredFurni(furniClassName)) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        HabboUtil.I().sort(floorItem.getTile().getX(), floorItem.getTile().getY());

                        WiredFurni previousPosition = ext.wiredState.getPrevious(floorItem.getId());
                        if (previousPosition != null && (floorItem.getTile().getX() != previousPosition.floorItem.getTile().getX() || floorItem.getTile().getY() != previousPosition.floorItem.getTile().getY())) {
                            HabboUtil.I().sort(previousPosition.floorItem.getTile().getX(), previousPosition.floorItem.getTile().getY());
                        }
                    }
                }, 10);
            }
        }
    }
}
