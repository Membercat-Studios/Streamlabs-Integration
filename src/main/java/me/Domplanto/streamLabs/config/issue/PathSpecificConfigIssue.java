package me.Domplanto.streamLabs.config.issue;

import org.bukkit.ChatColor;

public class PathSpecificConfigIssue extends ConfigIssue {
    private final ConfigPathStack pathStack;

    public PathSpecificConfigIssue(ConfigIssue issue, ConfigPathStack pathStack) {
        super(issue.getId(), issue.getLevel(), issue.getDescription());
        this.pathStack = pathStack;
    }

    @Override
    public String getMessage() {
        return getLevel().getColor() + "[%s/%s %sat %s]: %s".formatted(getLevel(), getId(), ChatColor.DARK_GRAY, getPath().toFormattedString() + getLevel().getColor(), getDescription());
    }

    public ConfigPathStack getPath() {
        return pathStack;
    }
}
