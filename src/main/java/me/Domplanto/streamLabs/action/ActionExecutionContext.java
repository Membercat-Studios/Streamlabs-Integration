package me.Domplanto.streamLabs.action;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;

import java.util.Collection;

public record ActionExecutionContext(
        StreamlabsEvent event,
        RewardsConfig config,
        RewardsConfig.Action action,
        JsonObject baseObject
) {
    public Collection<ActionPlaceholder> getPlaceholders() {
        Collection<ActionPlaceholder> placeholders = event().getPlaceholders();
        placeholders.addAll(config().getCustomPlaceholders());
        return placeholders;
    }
}
