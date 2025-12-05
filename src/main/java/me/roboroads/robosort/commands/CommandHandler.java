package me.roboroads.robosort.commands;

import gearth.protocol.HMessage;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.util.HabboUtil;

import java.util.List;

public class CommandHandler {
    private final Robosort ext;
    private final List<Command> commands;
    private Command active;

    public CommandHandler(Robosort ctx, List<Command> commands) {
        this.ext = ctx;
        this.commands = commands;
        // Register interceptions here to keep Robosort minimal
        ext.intercept(HMessage.Direction.TOSERVER, "Chat", hMessage -> {
            String text = hMessage.getPacket().readString();
            if (!ext.commandsEnabled() || !text.startsWith(":")) {
                return;
            }
            boolean handled = handleChat(text);
            if (handled) {
                hMessage.setBlocked(true);
            }
        });

        ext.intercept(HMessage.Direction.TOSERVER, "ClickFurni", hMessage -> {
            if (!ext.commandsEnabled() || !HabboUtil.I().checkCanMove(false)) {
                return;
            }
            int furniId = hMessage.getPacket().readInteger();
            boolean handled = handleClickFurni(furniId);
            if (handled) {
                hMessage.setBlocked(true);
            }
        });

        // Abort active command on common lifecycle events
        ext.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> abortActive());
        ext.intercept(HMessage.Direction.TOSERVER, "Quit", m -> abortActive());
        ext.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> abortActive());
    }

    public boolean handleChat(String text) {
        if (!text.startsWith(":")) {
            return false;
        }

        if (active != null) {
            if (":abort".equals(text)) {
                active.onAbort();
                active = null;
                HabboUtil.I().sendChat("Aborted");
                return true;
            }
            // another command while active: ignore
            return false;
        }

        if (!HabboUtil.I().checkCanMove(true)) {
            return false;
        }

        for (Command command : commands) {
            if (text.startsWith(command.getTrigger())) {
                boolean claimed = command.onChat(text);
                if (claimed) {
                    active = command;
                }
                return claimed;
            }
        }

        return false;
    }

    public boolean handleClickFurni(int furniId) {
        if (active == null) {
            return false;
        }
        WiredFurni wiredFurni = ext.wiredState.getCurrent(furniId);
        if (wiredFurni == null) {
            return false;
        }
        active.onClick(wiredFurni);
        active = null;
        return true;
    }

    public void abortActive() {
        if (active != null) {
            active.onAbort();
            active = null;
        }
    }
}
