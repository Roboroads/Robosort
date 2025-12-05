package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;

import java.util.List;
import java.util.stream.Collectors;

public class UpCommand extends Command {
    private int amount = 1;

    public UpCommand(Robosort ctx) {
        super(ctx);
    }

    @Override
    public String getTrigger() {
        return ":up";
    }

    @Override
    public boolean onChat(String text) {
        // parse optional number
        try {
            String[] parts = text.split(" ");
            if (parts.length == 2) {
                amount = Integer.parseInt(parts[1]);
            } else {
                amount = 1;
            }
        } catch (Exception ignored) {
            amount = 1;
        }
        HabboUtil.I().sendChat("Click on the box that you want to move up " + amount + " position(s).");
        return true;
    }

    @Override
    public void onClick(WiredFurni wiredFurni) {
        moveUp(wiredFurni, amount);
    }

    private void moveUp(WiredFurni wiredFurni, int amount) {
        List<WiredFurni> stackState = ext.wiredState.wiredOnTile(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY()).stream().filter(i -> i.wiredBoxType == wiredFurni.wiredBoxType).collect(Collectors.toList());

        int index = stackState.indexOf(wiredFurni);
        List<WiredFurni> movingBoxes = stackState.subList(index, Math.min(index + amount + 1, stackState.size()));

        if (movingBoxes.size() < 2) {
            HabboUtil.I().sendChat("There's no boxes of the same type above this one. Aborting.");
            return;
        }

        for (int i = 0; i < movingBoxes.size(); i++) {
            WiredFurni movingBox = movingBoxes.get(i);
            double newZ = movingBoxes.get((i == 0 ? (movingBoxes.size() - 1) : (i - 1))).floorItem.getTile().getZ();
            ext.mover.queue(movingBox.floorItem.getId(), (int) (newZ * 100));
        }
    }
}
