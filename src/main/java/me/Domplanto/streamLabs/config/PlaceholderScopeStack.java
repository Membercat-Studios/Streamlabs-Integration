package me.Domplanto.streamLabs.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class PlaceholderScopeStack extends Stack<PlaceholderScopeStack.Entry> {
    public PlaceholderScopeStack() {
        this.push((String) null);
    }

    public void push(@Nullable String scopeName) {
        Set<ActionPlaceholder> placeholders = !isEmpty() ? peek().placeholders() : Set.of();
        this.push(new Entry(scopeName, new HashSet<>(placeholders)));
    }

    public void addPlaceholder(@NotNull String name, ActionPlaceholder.PlaceholderFunction function) {
        this.addPlaceholder(new ActionPlaceholder(name, function));
    }

    public void addPlaceholder(@NotNull ActionPlaceholder placeholder) {
        if (this.isEmpty()) return;
        Set<ActionPlaceholder> placeholders = this.peek().placeholders();
        placeholders.removeIf(placeholder::equals);
        placeholders.add(placeholder);
    }

    public @NotNull Set<ActionPlaceholder> getPlaceholders() {
        return !empty() ? peek().placeholders() : Set.of();
    }

    public record Entry(
            @Nullable String scopeName,
            Set<ActionPlaceholder> placeholders
    ) {
    }
}
