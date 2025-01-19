package me.Domplanto.streamLabs.config.issue;

import org.bukkit.ChatColor;

public class PathSpecificConfigIssue extends ConfigIssue {
    private final ConfigPathStack pathStack;

    public PathSpecificConfigIssue(Level level, ConfigPathStack pathStack, String description) {
        super(level, description);
        this.pathStack = pathStack;
    }

    @Override
    public String getMessage() {
        return getLevel().getColor() + "[%s %sat %s]: %s".formatted(getLevel(), ChatColor.DARK_GRAY, getPath().toFormattedString() + getLevel().getColor(), getDescription());
    }

    public ConfigPathStack getPath() {
        return pathStack;
    }
}
