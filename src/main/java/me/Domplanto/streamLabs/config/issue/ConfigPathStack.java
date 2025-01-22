package me.Domplanto.streamLabs.config.issue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.Stack;

public class ConfigPathStack extends Stack<ConfigPathStack.Entry> {
    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        for (ConfigPathStack.Entry segment : this) {
            builder.append("/");
            String staticName = segment.annotation() != null ? segment.annotation().id() : segment.cls().getTypeName();
            builder.append(segment.ownName() != null ? segment.ownName() : staticName);
        }

        return builder.toString();
    }

    @Override
    public synchronized ConfigPathStack clone() {
        return (ConfigPathStack) super.clone();
    }

    public record Entry(
            @NotNull Class<?> cls,
            @Nullable ConfigPathSegment annotation,
            @Nullable String ownName,
            Set<String> suppressedIssues
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
    }

    @ConfigPathSegment(id = "property")
    public record Property() {
    }

    @ConfigPathSegment(id = "section")
    public record Section() {
    }
}
