package com.membercat.streamlabs.action.ratelimiter;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.placeholder.ActionPlaceholder;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathSegment;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import com.membercat.streamlabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import static com.membercat.streamlabs.config.issue.Issues.WRE0;

@ConfigPathSegment(id = "rate_limiter_event")
public class RateLimiterEvent extends PluginConfig.AbstractAction {
    @YamlProperty("event_mode")
    private EventMode mode = EventMode.ALWAYS;
    private final Set<String> currentLimited = new HashSet<>();
    private final Set<String> pastLimited = new HashSet<>();

    protected void onCheck(@NotNull String value, boolean result, ActionExecutionContext ctx) {
        if (result) {
            this.currentLimited.remove(value);
            return;
        }

        boolean alreadyLimited = currentLimited.contains(value);
        boolean wasLimitedBefore = pastLimited.contains(value);
        if (mode.shouldRun(alreadyLimited, wasLimitedBefore) && this.check(ctx)) {
            ActionExecutionContext newCtx = ctx.withAction(this);
            newCtx.scopeStack().addPlaceholder("value", ActionPlaceholder.PlaceholderFunction.of(value));
            newCtx.scopeStack().addPlaceholder("already_limited", ActionPlaceholder.PlaceholderFunction.of(alreadyLimited));
            newCtx.scopeStack().addPlaceholder("limited_before", ActionPlaceholder.PlaceholderFunction.of(wasLimitedBefore));
            ctx.executor().executeAction(newCtx);
        }
        this.currentLimited.add(value);
        this.pastLimited.add(value);
    }

    protected void reset() {
        this.currentLimited.clear();
        this.pastLimited.clear();
    }

    @YamlPropertyCustomDeserializer(propertyName = "event_mode")
    public @NotNull EventMode deserializeMode(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        try {
            return EventMode.valueOf(input);
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WRE0.apply(input));
            return EventMode.NONE;
        }
    }

    public enum EventMode {
        NONE((already, before) -> false),
        ALWAYS((already, before) -> true),
        ONCE((already, before) -> !before),
        ON_FIRST_LIMIT((already, before) -> !already);
        private final BiFunction<Boolean, Boolean, Boolean> checkFunc;

        EventMode(BiFunction<Boolean, Boolean, Boolean> checkFunc) {
            this.checkFunc = checkFunc;
        }

        public boolean shouldRun(boolean alreadyLimited, boolean wasLimitedBefore) {
            return this.checkFunc.apply(alreadyLimited, wasLimitedBefore);
        }
    }
}
