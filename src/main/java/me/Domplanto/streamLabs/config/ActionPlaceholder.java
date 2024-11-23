package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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

    public static String replacePlaceholders(String originalString, StreamlabsEvent event, RewardsConfig config, JsonObject baseObject) {
        Collection<ActionPlaceholder> placeholders = event.getPlaceholders();
        placeholders.addAll(config.getCustomPlaceholders());
        for (ActionPlaceholder placeholder : placeholders) {
            originalString = originalString.replace(String.format("{%s}", placeholder.name()),
                    placeholder.function().execute(baseObject, event));
        }

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
        private final BiFunction<JsonObject, StreamlabsEvent, String> eventDependentFunction;

        private PlaceholderFunction(@Nullable Function<JsonObject, String> valueFunction, @Nullable BiFunction<JsonObject, StreamlabsEvent, String> eventDependentValueFunction) {
            if (valueFunction == null && eventDependentValueFunction == null)
                throw new NullPointerException();

            this.valueFunction = valueFunction;
            this.eventDependentFunction = eventDependentValueFunction;
        }

        public static PlaceholderFunction of(@NotNull String staticValue) {
            Objects.requireNonNull(staticValue);
            return new PlaceholderFunction(o -> staticValue, null);
        }

        public static PlaceholderFunction of(Function<JsonObject, String> valueFunction) {
            return new PlaceholderFunction(Objects.requireNonNull(valueFunction), null);
        }

        public static PlaceholderFunction of(BiFunction<JsonObject, StreamlabsEvent, String> valueFunction) {
            return new PlaceholderFunction(null, Objects.requireNonNull(valueFunction));
        }

        public String execute(JsonObject object, StreamlabsEvent event) {
            if (eventDependentFunction == null && valueFunction == null)
                throw new NullPointerException();

            return eventDependentFunction != null ? eventDependentFunction.apply(object, event)
                    : valueFunction.apply(object);
        }
    }
}
