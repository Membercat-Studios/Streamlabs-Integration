package me.Domplanto.streamLabs.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.exception.UnexpectedJsonFormatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class StreamlabsEvent {
    @NotNull
    private final String apiName;
    @NotNull
    private final String id;
    private final StreamlabsPlatform platform;

    public StreamlabsEvent(@NotNull String id, @NotNull String apiName, StreamlabsPlatform platform) {
        this.id = id;
        this.apiName = apiName;
        this.platform = platform;
    }

    @NotNull
    public JsonObject getBaseObject(JsonObject rootObject) throws UnexpectedJsonFormatException {
        JsonArray messages = rootObject.get("message").getAsJsonArray();
        if (messages.isEmpty())
            throw new UnexpectedJsonFormatException();

        return messages.get(0).getAsJsonObject();
    }

    @Nullable
    public abstract String getMessage(JsonObject object);

    public @NotNull String getRelatedUser(JsonObject object) {
        return object.get("name").getAsString();
    }

    public @NotNull String getApiName() {
        return apiName;
    }

    public @NotNull String getId() {
        return id;
    }

    public StreamlabsPlatform getPlatform() {
        return platform;
    }

    public boolean checkThreshold(JsonObject object, double threshold) {
        return true;
    }

    public static Set<? extends StreamlabsEvent> findEventClasses() {
        String packageName = StreamlabsEvent.class.getPackageName();
        return new Reflections(packageName)
                .getSubTypesOf(StreamlabsEvent.class)
                .stream()
                .map(cls -> {
                    try {
                        return cls.getConstructor().newInstance();
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }
}
