package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.util.components.ColorScheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
            builder.append(text("/"));
            String clsName = segment.cls() != null ? segment.cls().getTypeName() : "unknown";
            String staticName = segment.annotation() != null ? segment.annotation().id() : clsName;
            builder.append(text()
                    .content(segment.ownName() != null ? segment.ownName() : staticName)
                    .color(this.lastElement().equals(segment) ? ColorScheme.DONE : null)
                    .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.config_type", ColorScheme.STREAMLABS, text(staticName, ColorScheme.SUCCESS)))));
        }

        return builder.build();
    }

    @Override
    public synchronized ConfigPathStack clone() {
        return (ConfigPathStack) super.clone();
    }

    public record Entry(
            @Nullable Class<?> cls,
            @Nullable ConfigPathSegment annotation,
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

        public void suppress(Collection<String> issueIds) {
            this.suppressedIssues.addAll(issueIds);
        }

        public void process(String... properties) {
            this.processedProperties.addAll(Arrays.asList(properties));
        }
    }

    @ConfigPathSegment(id = "property")
    public record Property() {
    }

    @ConfigPathSegment(id = "section")
    public record Section() {
    }
}
