package me.Domplanto.streamLabs.config.issue;

import org.bukkit.ChatColor;

public class ConfigIssue {
    private final Level level;
    private final String description;

    public ConfigIssue(Level level, String description) {
        this.level = level;
        this.description = description;
    }

    public String getMessage() {
        return getLevel().getColor() + "[%s]: %s".formatted(getLevel(), getDescription());
    }

    public Level getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public enum Level {
        ERROR(ChatColor.RED, java.util.logging.Level.SEVERE),
        WARNING(ChatColor.YELLOW, java.util.logging.Level.WARNING),
        HINT(ChatColor.GRAY, java.util.logging.Level.INFO);
        private final ChatColor color;
        private final java.util.logging.Level logLevel;

        Level(ChatColor color, java.util.logging.Level logLevel) {
            this.color = color;
            this.logLevel = logLevel;
        }

        public ChatColor getColor() {
            return color;
        }

        public java.util.logging.Level getLogLevel() {
            return logLevel;
        }
    }
}
