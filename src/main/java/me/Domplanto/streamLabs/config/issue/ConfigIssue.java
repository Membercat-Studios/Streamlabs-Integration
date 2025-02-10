package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.util.components.ColorScheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Set;

import static net.kyori.adventure.text.Component.translatable;

public class ConfigIssue {
    private final String id;
    private final Level level;
    private Component description;

    public ConfigIssue(String id, Level level) {
        this.id = id;
        this.level = level;
        this.description = translatable("streamlabs.issue.%s".formatted(id));
    }

    public ConfigIssue(String id, Level level, ComponentLike... args) {
        this(id, level);
        this.description = translatable()
                .key("streamlabs.issue.%s".formatted(id))
                .arguments(args).build();
    }

    public Level getLevel() {
        return level;
    }

    public String getId() {
        return id;
    }

    public Component getDescription() {
        return description;
    }

    public enum Level {
        ERROR(ColorScheme.ERROR, java.util.logging.Level.SEVERE),
        WARNING(ColorScheme.INVALID, java.util.logging.Level.WARNING),
        HINT(ColorScheme.COMMENT, java.util.logging.Level.INFO);
        private final TextColor color;
        private final java.util.logging.Level logLevel;

        Level(TextColor color, java.util.logging.Level logLevel) {
            this.color = color;
            this.logLevel = logLevel;
        }

        public Component translatable() {
            return Component.translatable()
                    .key("streamlabs.issue.level.%s".formatted(this.name().toLowerCase()))
                    .style(Style.style(this.color, this != HINT ? Set.of(TextDecoration.BOLD) : Set.of())).build();
        }

        public TextColor getColor() {
            return color;
        }

        public java.util.logging.Level getLogLevel() {
            return logLevel;
        }
    }
}
