package me.Domplanto.streamLabs.action;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public record ActionExecutionContext(
        StreamlabsEvent event,
        ActionExecutor executor,
        PluginConfig config,
        PluginConfig.AbstractAction action,
        Set<ActionPlaceholder> actionSpecificPlaceholders,
        AtomicReference<Predicate<ActionExecutionContext>> keepExecutingCheck,
        JsonObject baseObject,
        AtomicBoolean shouldExecute
) {
    public ActionExecutionContext(StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, JsonObject jsonObject) {
        this(event, executor, config, action, new HashSet<>(), new AtomicReference<>(), jsonObject, new AtomicBoolean(true));
    }

    public Collection<ActionPlaceholder> getPlaceholders() {
        Collection<ActionPlaceholder> placeholders = new HashSet<>(config().getCustomPlaceholders());
        if (event() != null) placeholders.addAll(event().getPlaceholders());
        placeholders.addAll(this.actionSpecificPlaceholders);
        return placeholders;
    }

    public void addSpecificPlaceholder(@NotNull ActionPlaceholder placeholder) {
        this.actionSpecificPlaceholders.removeIf(pl -> pl.name().equals(placeholder.name()));
        this.actionSpecificPlaceholders.add(placeholder);
    }

    public boolean shouldKeepExecuting() {
        return Optional.ofNullable(this.keepExecutingCheck.get())
                .map(check -> check.test(this))
                .orElse(false);
    }

    void setKeepExecutingCheck(@Nullable Predicate<ActionExecutionContext> keepExecutingCheck) {
        this.keepExecutingCheck.set(keepExecutingCheck);
    }

    public void runSteps(StepExecutor executor, StreamLabs plugin) {
        executor.runSteps(this, plugin);
    }

    public boolean isDonation() {
        return event() instanceof BasicDonationEvent;
    }

    public void stop() {
        shouldExecute.set(false);
    }
}
