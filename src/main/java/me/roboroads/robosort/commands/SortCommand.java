package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.Tile;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;

import java.util.List;
import java.util.regex.Matcher;

public class SortCommand extends Command {
    public SortCommand(Robosort ctx) {
        super(ctx);
    }

    @Override
    public String getTriggerPattern() {
        // Matches ":sort" or ":sort all" (case-insensitive)
        return ":sort(?:\\s+(all))?";
    }

    @Override
    protected boolean onCommand(Matcher matcher) {
        boolean sortAll = matcher.group(1) != null;
        if (sortAll) {
            // Sort all stacks in the room (non-interactive)
            List<Tile> tiles = ext.wiredState.getAllWiredTiles();
            for (Tile tile : tiles) {
                HabboUtil.I().sort(tile.x, tile.y);
            }
            HabboUtil.I().sendChat("Started sorting for " + tiles.size() + " wired stack(s).");
            return false; // non-interactive
        } else {
            // Single-stack sort; wait for a click
            HabboUtil.I().sendChat("Click on a box in the wired stack you want to sort");
            return true; // interactive
        }
    }

    @Override
    public void onClick(WiredFurni wiredFurni) {
        HabboUtil.I().sort(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY());
    }
}
