package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;

public abstract class Command {
    protected final Robosort ext;

    protected Command(Robosort ext) {
        this.ext = ext;
    }

    public abstract String getTrigger();

    // Return true if the chat was handled and should be blocked
    public abstract boolean onChat(String text);

    public abstract void onClick(WiredFurni wiredFurni);

    public void onAbort() {
        // default no-op
    }
}
