package me.Domplanto.streamLabs.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.socket.serializer.SocketSerializerException;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    public void addPlaceholder(String name, Function<JsonObject, String> valueFunction) {
        this.placeholders.removeIf(placeholder -> placeholder.name().equals(name));
        this.placeholders.add(new ActionPlaceholder(name, ActionPlaceholder.PlaceholderFunction.of(valueFunction)));
    }

    @NotNull
    public JsonObject getBaseObject(JsonObject rootObject) throws SocketSerializerException {
        JsonArray messages = rootObject.get("message").getAsJsonArray();
        if (messages.isEmpty())
            throw new SocketSerializerException();

        return messages.get(0).getAsJsonObject();
    }

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

    public boolean checkConditions(ActionExecutionContext ctx) {
        RateLimiter limiter = ctx.action().rateLimiter;
        if (limiter != null && !limiter.check(ctx)) return false;

        ArrayList<Condition> conditionList = new ArrayList<>(ctx.action().conditions);
        if (this instanceof BasicDonationEvent)
            conditionList.addAll(ctx.action().donationConditions);

        for (Condition condition : conditionList)
            if (!condition.check(ctx)) return false;

        return true;
    }

    public static Set<? extends StreamlabsEvent> findEventClasses() {
        return ReflectUtil.findClasses(StreamlabsEvent.class);
    }
}
