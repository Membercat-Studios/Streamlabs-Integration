package com.membercat.streamlabs.config.issue;

import com.membercat.streamlabs.util.components.ColorScheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

public class ConfigPathStack extends Stack<ConfigPathStack.Entry> {
    public String toFormattedString() {
        return PlainTextComponentSerializer.plainText().serialize(toComponent());
    }

    public Component toComponent() {
        TextComponent.Builder builder = text();
        for (ConfigPathStack.Entry segment : this) {
            String clsName = segment.cls() != null ? getClassDisplayName(segment.cls()) : "unknown";
            builder.append(text()
                    .content(segment.ownName() != null ? segment.ownName() : clsName)
                    .color(this.lastElement().equals(segment) ? ColorScheme.DONE : null)
                    .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.config_type", ColorScheme.STREAMLABS, text(clsName, ColorScheme.SUCCESS)))));
            if (!this.lastElement().equals(segment)) builder.append(text("/"));
        }

        return builder.build();
    }

    public static @NotNull String getObjectTypeName(@Nullable Object o) {
        return Optional.ofNullable(o).map(Object::getClass)
                .map(ConfigPathStack::getClassDisplayName)
                .orElse("null");
    }

    public static @NotNull String getClassDisplayName(@NotNull Class<?> cls) {
        ConfigPathSegment segment = cls.getDeclaredAnnotation(ConfigPathSegment.class);
        return Optional.ofNullable(segment).map(ConfigPathSegment::id)
                .orElseGet(cls::getSimpleName);
    }

    @Override
    public synchronized ConfigPathStack clone() {
        return (ConfigPathStack) super.clone();
    }

    public record Entry(
            @Nullable Class<?> cls,
            @Nullable String ownName,
            Set<String> suppressedIssues,
            Set<String> processedProperties
    ) {
        public boolean isProperty() {
            return cls() == Property.class;
        }

        public boolean isSection() {
            return cls() == Section.class;
        }

        public boolean isRoot() {
            return cls() == Root.class;
        }

        public void suppress(Collection<String> issueIds) {
            this.suppressedIssues.addAll(issueIds);
        }

        public void process(String... properties) {
            this.processedProperties.addAll(Arrays.asList(properties));
        }
    }

    @ConfigPathSegment(id = "root")
    public record Root() {
    }

    @ConfigPathSegment(id = "property")
    public record Property() {
    }

    @ConfigPathSegment(id = "section")
    public record Section() {
    }
}
