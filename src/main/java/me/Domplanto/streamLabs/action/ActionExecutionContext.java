package me.Domplanto.streamLabs.action;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

import java.util.Collection;

public record ActionExecutionContext(
        StreamlabsEvent event,
        PluginConfig config,
        PluginConfig.Action action,
        JsonObject baseObject
) {
    public Collection<ActionPlaceholder> getPlaceholders() {
        Collection<ActionPlaceholder> placeholders = event().getPlaceholders();
        placeholders.addAll(config().getCustomPlaceholders());
        return placeholders;
    }

    public boolean isDonation() {
        return event() instanceof BasicDonationEvent;
    }
}
