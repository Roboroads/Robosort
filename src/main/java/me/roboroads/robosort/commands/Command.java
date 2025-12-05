package me.roboroads.robosort.commands;

import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Command {
    protected final Robosort ext;

    protected Command(Robosort ext) {
        this.ext = ext;
    }

    // Regex pattern string that fully matches the command, including optional args
    public abstract String getTriggerPattern();

    // HandleResult wraps whether the command claimed the chat and if it is interactive (expects a click)
    public static class HandleResult {
        public final boolean claimed;
        public final boolean interactive;

        public HandleResult(boolean claimed, boolean interactive) {
            this.claimed = claimed;
            this.interactive = interactive;
        }
    }

    // Centralized argument parsing + dispatch based on regex
    public final HandleResult handle(String text) {
        String trimmed = text.trim();
        Pattern pattern = Pattern.compile(getTriggerPattern(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(trimmed);
        if (!matcher.matches()) {
            return new HandleResult(false, false);
        }
        boolean interactive = onCommand(matcher);
        return new HandleResult(true, interactive);
    }

    // Implement command behavior. Return true if the command expects a follow-up click.
    protected abstract boolean onCommand(Matcher matcher);

    public abstract void onClick(WiredFurni wiredFurni);

    public void onAbort() {
        // default no-op
    }
}
