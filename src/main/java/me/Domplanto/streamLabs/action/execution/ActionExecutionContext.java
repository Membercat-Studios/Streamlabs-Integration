package me.Domplanto.streamLabs.action.execution;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

import java.util.Collection;
import java.util.HashSet;

public record ActionExecutionContext(
        StreamlabsEvent event,
        ActionExecutor executor,
        PluginConfig config,
        PluginConfig.AbstractAction action,
        JsonObject baseObject
) {
    public Collection<ActionPlaceholder> getPlaceholders() {
        Collection<ActionPlaceholder> placeholders = new HashSet<>(config().getCustomPlaceholders());
        if (event() != null)
            placeholders.addAll(event().getPlaceholders());
        return placeholders;
    }

    public boolean isDonation() {
        return event() instanceof BasicDonationEvent;
    }
}
