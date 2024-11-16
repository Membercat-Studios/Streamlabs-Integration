package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record ActionPlaceholder(
        @NotNull String name,
        Function<JsonObject, String> valueFunction
) {
    public static String replacePlaceholders(String originalString, StreamlabsEvent event, JsonObject baseObject) {
        for (ActionPlaceholder placeholder : event.getPlaceholders()) {
            originalString = originalString.replace(String.format("{%s}", placeholder.name()),
                    placeholder.valueFunction().apply(baseObject));
        }

        return originalString;
    }
}
