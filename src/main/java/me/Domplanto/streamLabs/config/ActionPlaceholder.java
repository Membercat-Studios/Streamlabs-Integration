package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ActionPlaceholder {
    private final @NotNull String name;
    private final PlaceholderFunction function;

    public ActionPlaceholder(@NotNull String name, PlaceholderFunction function) {
        this.name = name;
        this.function = function;
    }

    public static String replacePlaceholders(String originalString, ActionExecutionContext ctx) {
        return replacePlaceholders(originalString, ctx, 0);
    }

    public static String replacePlaceholders(String originalString, ActionExecutionContext ctx, int plExecutionCount) {
        boolean containsPlaceholders = false;
        for (ActionPlaceholder placeholder : ctx.getPlaceholders()) {
            String ph = String.format("{%s}", placeholder.name());
            if (!originalString.contains(ph)) continue;

            containsPlaceholders = true;
            originalString = originalString.replace(ph, placeholder.function().execute(ctx.baseObject(), ctx));
        }

        plExecutionCount++;
        if (plExecutionCount > 1000)
            return "(Infinite placeholder loop detected)";
        if (containsPlaceholders)
            originalString = replacePlaceholders(originalString, ctx, plExecutionCount);

        return originalString;
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
            if (contextDependentFunction == null && valueFunction == null)
                throw new NullPointerException();

            return contextDependentFunction != null ? contextDependentFunction.apply(object, context)
                    : valueFunction.apply(object);
        }
    }
}
