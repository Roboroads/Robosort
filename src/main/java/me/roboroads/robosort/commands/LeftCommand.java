package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Tile;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;
import me.roboroads.robosort.util.RotateStack;

import java.util.List;
import java.util.regex.Matcher;

public class LeftCommand extends Command {
    public LeftCommand(Robosort ctx) {
        super(ctx);
    }

    @Override
    public String getTriggerPattern() {
        return ":left(?:\\s+(all))?";
    }

    @Override
    protected boolean onCommand(Matcher matcher) {
        boolean rotateAll = matcher.group(1) != null;
        if (rotateAll) {
            List<Tile> tiles = ext.wiredState.getAllWiredTiles();
            int total = 0;
            for (Tile tile : tiles) {
                total += RotateStack.apply(ext, tile.x, tile.y, true);
            }
            HabboUtil.I().sendChat("Rotating " + total + " box(es) across " + tiles.size() + " stack(s) to the left.");
            return false;
        }
        HabboUtil.I().sendChat("Click on a box in the stack you want to rotate to the left.");
        return true;
    }

    @Override
    public void onClick(WiredFurni wiredFurni) {
        int rotated = RotateStack.apply(ext, wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY(), true);
        HabboUtil.I().sendChat("Rotating " + rotated + " box(es) to the left.");
    }
}
