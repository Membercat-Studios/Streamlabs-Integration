package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ActionPlaceholder {
    private final @NotNull String name;
    private final PlaceholderFunction function;

    public ActionPlaceholder(@NotNull String name, PlaceholderFunction function) {
        this.name = Objects.requireNonNull(name);
        this.function = function;
    }

    public static String replacePlaceholders(String originalString, ActionExecutionContext ctx) {
        return replacePlaceholders(originalString, ctx, 0);
    }

    public static String replacePlaceholders(String originalString, ActionExecutionContext ctx, int plExecutionCount) {
        boolean containsPlaceholders = false;
        for (ActionPlaceholder placeholder : ctx.scopeStack().getPlaceholders()) {
            String format = placeholder.getFormat();
            if (!originalString.contains(format)) continue;

            containsPlaceholders = true;
            originalString = originalString.replace(format, placeholder.function().execute(ctx.baseObject(), ctx));
        }

        if (StreamLabs.isPapiInstalled())
            originalString = PlaceholderAPI.setPlaceholders(null, originalString);
        plExecutionCount++;
        if (plExecutionCount > 1000)
            return "(Infinite placeholder loop detected)";
        if (containsPlaceholders)
            originalString = replacePlaceholders(originalString, ctx, plExecutionCount);

        return originalString;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActionPlaceholder placeholder)) return false;
        return this.name().equals(placeholder.name())
                && this.getFormat().equals(placeholder.getFormat());
    }

    public @NotNull String getFormat() {
        return "{%s}".formatted(name());
    }

    public @NotNull String name() {
        return name;
    }

    public PlaceholderFunction function() {
        return function;
    }

    public static class PlaceholderFunction {
        @Nullable
        private final Function<JsonObject, String> valueFunction;
        @Nullable
        private final BiFunction<JsonObject, ActionExecutionContext, String> contextDependentFunction;

        private PlaceholderFunction(@Nullable Function<JsonObject, String> valueFunction, @Nullable BiFunction<JsonObject, ActionExecutionContext, String> contextDependentValueFunction) {
            if (valueFunction == null && contextDependentValueFunction == null)
                throw new NullPointerException();

            this.valueFunction = valueFunction;
            this.contextDependentFunction = contextDependentValueFunction;
        }

        public static PlaceholderFunction of(@NotNull String staticValue) {
            Objects.requireNonNull(staticValue);
            return new PlaceholderFunction(o -> staticValue, null);
        }

        public static PlaceholderFunction of(Function<JsonObject, String> valueFunction) {
            return new PlaceholderFunction(Objects.requireNonNull(valueFunction), null);
        }

        public static PlaceholderFunction of(BiFunction<JsonObject, ActionExecutionContext, String> valueFunction) {
            return new PlaceholderFunction(null, Objects.requireNonNull(valueFunction));
        }

        public String execute(@NotNull JsonObject object, @Nullable ActionExecutionContext context) {
            try {
                if (contextDependentFunction == null && valueFunction == null)
                    throw new NullPointerException();

                String result = contextDependentFunction != null ? contextDependentFunction.apply(object, context)
                        : valueFunction.apply(object);
                return result != null ? result : "(Error while resolving placeholder)";
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                StreamLabs.LOGGER.warning("Failed to resolve placeholder:%s\n%s".formatted(e.toString(), writer.toString()));
                return "(Unexpected error while resolving placeholder, check the logs for more info)";
            }
        }
    }
}
