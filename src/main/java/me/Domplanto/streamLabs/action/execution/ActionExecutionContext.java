package me.Domplanto.streamLabs.action.execution;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.AbstractStep;
import me.Domplanto.streamLabs.action.StepBase;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public record ActionExecutionContext(
        StreamlabsEvent event,
        ActionExecutor executor,
        PluginConfig config,
        PluginConfig.AbstractAction action,
        JsonObject baseObject,
        AtomicBoolean shouldExecute
) {

    public ActionExecutionContext(StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, JsonObject jsonObject) {
        this(event, executor, config, action, jsonObject, new AtomicBoolean(true));
    }

    public Collection<ActionPlaceholder> getPlaceholders() {
        Collection<ActionPlaceholder> placeholders = new HashSet<>(config().getCustomPlaceholders());
        if (event() != null)
            placeholders.addAll(event().getPlaceholders());
        return placeholders;
    }

    public void runSteps(Collection<? extends StepBase<?>> steps, StreamLabs plugin) throws AbstractStep.ActionFailureException {
        for (StepBase<?> step : steps) {
            if (!shouldExecute().get()) return;
            step.execute(this, plugin);
        }
    }

    public boolean isDonation() {
        return event() instanceof BasicDonationEvent;
    }

    public void stop() {
        shouldExecute.set(false);
    }
}
