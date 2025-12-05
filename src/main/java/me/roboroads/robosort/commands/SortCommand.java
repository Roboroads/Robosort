package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;

public class SortCommand extends Command {
    public SortCommand(Robosort ctx) {
        super(ctx);
    }

    @Override
    public String getTrigger() {
        return ":sort";
    }

    @Override
    public boolean onChat(String text) {
        HabboUtil.I().sendChat("Click on a box in the wired stack you want to sort");
        return true;
    }

    @Override
    public void onClick(WiredFurni wiredFurni) {
        HabboUtil.I().sort(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY());
    }
}
