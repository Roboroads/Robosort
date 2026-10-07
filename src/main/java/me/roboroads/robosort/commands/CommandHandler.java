package me.roboroads.robosort.commands;

import gearth.protocol.HMessage;
import me.roboroads.gearth.gpackets.Intercept;
import me.roboroads.gearth.gpackets.incoming.CloseConnection;
import me.roboroads.gearth.gpackets.incoming.RoomReady;
import me.roboroads.gearth.gpackets.outgoing.Chat;
import me.roboroads.gearth.gpackets.outgoing.ClickFurni;
import me.roboroads.gearth.gpackets.outgoing.Quit;
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
    }

    @Intercept
    private void onChat(Chat chat, HMessage hMessage) {
        String text = chat.text();
        if (!ext.commandsEnabled() || !text.startsWith(":")) {
            return;
        }
        boolean handled = handleChat(text);
        if (handled) {
            hMessage.setBlocked(true);
        }
    }

    @Intercept
    private void onClickFurni(ClickFurni clickFurni, HMessage hMessage) {
        if (!ext.commandsEnabled() || !HabboUtil.I().checkCanMove(false)) {
            return;
        }
        boolean handled = handleClickFurni(clickFurni.furniId());
        if (handled) {
            hMessage.setBlocked(true);
        }
    }

    // Abort active command on common lifecycle events
    @Intercept({CloseConnection.class, RoomReady.class, Quit.class})
    private void onLeaveRoom(HMessage hMessage) {
        abortActive();
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

        Command matched = null;
        for (Command command : commands) {
            if (command.match(text) != null) {
                matched = command;
                break;
            }
        }
        if (matched == null) {
            return false;
        }

        if (!HabboUtil.I().checkCanMove(true)) {
            return true;
        }

        Command.HandleResult res = matched.handle(text);
        if (res.claimed) {
            active = res.interactive ? matched : null;
            return true;
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
