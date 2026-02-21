package com.membercat.streamlabs.action;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.config.placeholder.PlaceholderScopeStack;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
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
        boolean ignoreConditions,
        AtomicBoolean dirty,
        AtomicReference<Predicate<ActionExecutionContext>> keepExecutingCheck,
        JsonObject baseObject,
        AtomicBoolean shouldExecute
) {
    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, JsonObject jsonObject) {
        this(event, executor, config, action, false, false, jsonObject);
    }

    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, boolean bypassRateLimiters, boolean ignoreConditions, JsonObject jsonObject) {
        this(event, executor, config, action, new PlaceholderScopeStack(), bypassRateLimiters, ignoreConditions, new AtomicBoolean(), new AtomicReference<>(), jsonObject, new AtomicBoolean(true));
        if (event != null) event.getPlaceholders().forEach(scopeStack::addPlaceholder);
        if (config != null) config.getCustomPlaceholders().forEach(scopeStack::addPlaceholder);
        scopeStack.push("action");
    }

    boolean checkConditions() {
        return ignoreConditions || action().check(this);
    }

    public void markDirty() {
        this.dirty().set(true);
    }

    public ActionExecutionContext withAction(@NotNull PluginConfig.AbstractAction action) {
        return new ActionExecutionContext(event(), executor(), config(), action, baseObject());
    }

    public boolean shouldKeepExecuting() {
        return Optional.ofNullable(this.keepExecutingCheck.get())
                .map(check -> check.test(this))
                .orElse(false);
    }

    public ActionExecutionContext cloneScopeStack() {
        return new ActionExecutionContext(event, executor, config, action, (PlaceholderScopeStack) scopeStack.clone(), bypassRateLimiters, ignoreConditions, dirty, keepExecutingCheck, baseObject, shouldExecute);
    }

    public boolean shouldStopOnFailure() {
        return action() != null && action().stopOnFailure;
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
