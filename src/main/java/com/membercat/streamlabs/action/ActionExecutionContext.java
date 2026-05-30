package com.membercat.streamlabs.action;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.placeholder.ActionPlaceholder;
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
        AtomicBoolean shouldExecute,
        @Nullable PluginConfig.StreamlabsAccount sourceAccount
) {
    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, JsonObject jsonObject, @Nullable PluginConfig.StreamlabsAccount sourceAccount) {
        this(event, executor, config, action, false, false, jsonObject, sourceAccount);
    }

    public ActionExecutionContext(@Nullable StreamlabsEvent event, ActionExecutor executor, PluginConfig config, PluginConfig.AbstractAction action, boolean bypassRateLimiters, boolean ignoreConditions, JsonObject jsonObject, @Nullable PluginConfig.StreamlabsAccount sourceAccount) {
        this(event, executor, config, action, new PlaceholderScopeStack(), bypassRateLimiters, ignoreConditions, new AtomicBoolean(), new AtomicReference<>(), jsonObject, new AtomicBoolean(true), sourceAccount);
        if (event != null) event.getPlaceholders().forEach(scopeStack::addPlaceholder);
        if (config != null) config.getCustomPlaceholders().forEach(scopeStack::addPlaceholder);
        if (sourceAccount != null)
            scopeStack.addPlaceholder(new ActionPlaceholder("account", ActionPlaceholder.PlaceholderFunction.of(sourceAccount.id)));
        scopeStack.push("action");
    }

    boolean checkConditions() {
        return ignoreConditions || action().check(this);
    }

    public void markDirty() {
        this.dirty().set(true);
    }

    public ActionExecutionContext withAction(@NotNull PluginConfig.AbstractAction action) {
        return new ActionExecutionContext(event(), executor(), config(), action, baseObject(), sourceAccount());
    }

    public boolean shouldKeepExecuting() {
        return Optional.ofNullable(this.keepExecutingCheck.get())
                .map(check -> check.test(this))
                .orElse(false);
    }

    public ActionExecutionContext cloneScopeStack() {
        return new ActionExecutionContext(event, executor, config, action, (PlaceholderScopeStack) scopeStack.clone(), bypassRateLimiters, ignoreConditions, dirty, keepExecutingCheck, baseObject, shouldExecute, sourceAccount);
    }

    public boolean shouldStopOnFailure() {
        return action() != null && action().stopOnFailure;
    }

    void setKeepExecutingCheck(@Nullable Predicate<ActionExecutionContext> keepExecutingCheck) {
        this.keepExecutingCheck.set(keepExecutingCheck);
    }

    public void runSteps(StepExecutor executor, StreamlabsIntegration plugin) {
        executor.runSteps(this, plugin);
    }

    public boolean isDonation() {
        return event() instanceof BasicDonationEvent;
    }

    public void stop() {
        shouldExecute.set(false);
    }
}
