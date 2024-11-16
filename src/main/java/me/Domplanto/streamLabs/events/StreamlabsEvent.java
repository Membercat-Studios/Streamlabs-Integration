package me.Domplanto.streamLabs.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.exception.UnexpectedJsonFormatException;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public abstract class StreamlabsEvent {
    @NotNull
    private final String apiName;
    @NotNull
    private final String id;
    private final StreamlabsPlatform platform;
    private final Set<ActionPlaceholder> placeholders;

    public StreamlabsEvent(@NotNull String id, @NotNull String apiName, StreamlabsPlatform platform) {
        this.id = id;
        this.apiName = apiName;
        this.platform = platform;
        this.placeholders = new HashSet<>();
        this.addPlaceholder("user", this::getRelatedUser);
    }

    protected void addPlaceholder(String name, Function<JsonObject, String> valueFunction) {
        this.placeholders.removeIf(placeholder -> placeholder.name().equals(name));
        this.placeholders.add(new ActionPlaceholder(name, valueFunction));
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

    public Set<ActionPlaceholder> getPlaceholders() {
        return placeholders;
    }

    public boolean checkThreshold(JsonObject object, double threshold) {
        return true;
    }

    public static Set<? extends StreamlabsEvent> findEventClasses() {
        return ReflectUtil.findClasses(StreamlabsEvent.class);
    }
}
