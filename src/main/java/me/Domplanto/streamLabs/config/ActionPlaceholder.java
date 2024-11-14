package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record ActionPlaceholder(
        @NotNull String name,
        Function<JsonObject, String> valueFunction
) {
}
