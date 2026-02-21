package com.membercat.streamlabs.config.placeholder;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.action.ActionExecutionContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;

public abstract class AbstractPlaceholder {
    private final @NotNull String name;

    protected AbstractPlaceholder(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
    }

    public static String replacePlaceholders(String originalString, ActionExecutionContext ctx) {
        return replacePlaceholders(originalString, ctx, 0);
    }

    private static String replacePlaceholders(String originalString, ActionExecutionContext ctx, int plExecutionCount) {
        boolean containsPlaceholders = false;
        for (AbstractPlaceholder placeholder : ctx.scopeStack().getPlaceholders()) {
            if (!placeholder.isPresentIn(originalString)) continue;
            containsPlaceholders = true;
            try {
                originalString = placeholder.replaceAll(originalString, ctx);
            } catch (Exception e) {
                ctx.markDirty();
                StreamLabs.LOGGER.log(Level.WARNING, "Failed to resolve placeholder %s:".formatted(placeholder), e);
                return originalString;
            }
        }

        if (StreamLabs.isPapiInstalled()) originalString = PlaceholderAPI.setPlaceholders(null, originalString);
        plExecutionCount++;
        if (plExecutionCount > 1000) return "(Infinite placeholder loop detected)";
        if (containsPlaceholders) originalString = replacePlaceholders(originalString, ctx, plExecutionCount);
        return originalString;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name() + "]";
    }

    public @NotNull String name() {
        return this.name;
    }

    public abstract boolean isPresentIn(@NotNull String input);

    public abstract @NotNull String replaceAll(@NotNull String input, ActionExecutionContext ctx);

    @Override
    public abstract boolean equals(Object obj);
}
