package com.membercat.streamlabs.config.issue;

import com.membercat.streamlabs.util.components.ColorScheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigIssue that)) return false;
        return Objects.equals(getId(), that.getId()) && getLevel() == that.getLevel() && Component.EQUALS.test(getDescription(), that.getDescription());
    }

    public enum Level {
        ERROR(ColorScheme.ERROR, true),
        WARNING(ColorScheme.INVALID, true),
        HINT(ColorScheme.COMMENT, false),
        TODO(ColorScheme.TODO,false);
        private final TextColor color;
        private final boolean important;

        Level(@NotNull TextColor color, boolean important) {
            this.color = color;
            this.important = important;
        }

        public Component translatable() {
            return Component.translatable()
                    .key("streamlabs.issue.level.%s".formatted(this.name().toLowerCase()))
                    .style(Style.style(this.color, Set.of(TextDecoration.BOLD))).build();
        }

        public boolean isImportant() {
            return important;
        }

        public TextColor getColor() {
            return color;
        }
    }
}
