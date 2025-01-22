package me.Domplanto.streamLabs.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.socket.serializer.SocketSerializerException;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;

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
    private boolean bypassRateLimiters = false;

    public StreamlabsEvent(@NotNull String id, @NotNull String apiName, StreamlabsPlatform platform) {
        this.id = id;
        this.apiName = apiName;
        this.platform = platform;
        this.placeholders = new HashSet<>();
        this.addInternalPlaceholders();
        this.addPlaceholder("user", this::getRelatedUser);
    }

    private void addInternalPlaceholders() {
        this.addPlaceholder("_type", obj -> this.getId());
        this.addPlaceholder("_api_type", obj -> this.getApiName());
        this.addContextPlaceholder("_action", ctx -> ctx.action().id);
        this.addPlaceholder("_platform", obj -> this.getPlatform().name().toLowerCase());
        this.addPlaceholder("_api_platform", obj -> this.getPlatform().getId());
        this.addContextPlaceholder("_pl_count", ctx -> String.valueOf(ctx.getPlaceholders().size()));
        this.addPlaceholder("_pl_count_event", obj -> String.valueOf(this.getPlaceholders().size()));
    }

    public void addPlaceholder(String name, Function<JsonObject, String> valueFunction) {
        this.placeholders.removeIf(placeholder -> placeholder.name().equals(name));
        this.placeholders.add(new ActionPlaceholder(name, ActionPlaceholder.PlaceholderFunction.of(valueFunction)));
    }

    public void addContextPlaceholder(String name, Function<ActionExecutionContext, String> valueFunction) {
        this.placeholders.removeIf(placeholder -> placeholder.name().equals(name));
        this.placeholders.add(new ActionPlaceholder(name, ActionPlaceholder.PlaceholderFunction.of((obj, ctx) -> valueFunction.apply(ctx))));
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

    public void bypassRateLimiters() {
        this.bypassRateLimiters = true;
    }

    public boolean checkConditions(ActionExecutionContext ctx) {
        RateLimiter limiter = ctx.action().rateLimiter;
        if (!this.bypassRateLimiters && (limiter != null && !limiter.check(ctx))) return false;

        return ctx.action().check(ctx);
    }

    public static Set<? extends StreamlabsEvent> findEventClasses() {
        return ReflectUtil.findClasses(StreamlabsEvent.class);
    }
}
