package me.Domplanto.streamLabs.action;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.PlaceholderScopeStack;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public record ActionExecutionContext(
        StreamlabsEvent event,
        ActionExecutor executor,
        PluginConfig config,
        PluginConfig.AbstractAction action,
        PlaceholderScopeStack scopeStack,
        boolean bypassRateLimiters,
        AtomicReference<Predicate<ActionExecutionContext>> keepExecutingCheck,
        JsonObject baseObject,
        AtomicBoolean shouldExecute
) {
    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, JsonObject jsonObject) {
        this(event, executor, config, action, false, jsonObject);
    }

    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, boolean bypassRateLimiters, JsonObject jsonObject) {
        this(event, executor, config, action, new PlaceholderScopeStack(), bypassRateLimiters, new AtomicReference<>(), jsonObject, new AtomicBoolean(true));
        if (event != null) event.getPlaceholders().forEach(scopeStack::addPlaceholder);
        config.getCustomPlaceholders().forEach(scopeStack::addPlaceholder);
        scopeStack.push("action");
    }

    boolean checkConditions() {
        return action().check(this);
    }

    public ActionExecutionContext withAction(@NotNull PluginConfig.AbstractAction action) {
        return new ActionExecutionContext(event(), executor(), config(), action, baseObject());
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
