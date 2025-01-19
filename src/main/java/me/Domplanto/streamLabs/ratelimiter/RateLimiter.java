package me.Domplanto.streamLabs.ratelimiter;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@ConfigPathSegment(id = "rate_limiter")
public abstract class RateLimiter implements YamlPropertyObject {
    private static final Set<? extends RateLimiter> RATE_LIMITERS = RateLimiter.findRateLimiterClasses();
    @NotNull
    private final String id;
    @YamlProperty("value")
    private String value = "";

    public RateLimiter(@NotNull String id) {
        this.id = id;
    }

    public abstract boolean check(ActionExecutionContext ctx);

    public abstract void reset();

    public @NotNull String getId() {
        return id;
    }

    public String getValue(ActionExecutionContext ctx) {
        return ActionPlaceholder.replacePlaceholders(this.value, ctx);
    }

    @Nullable
    @YamlPropertyCustomDeserializer
    public static RateLimiter deserialize(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        if (section == null) return null;

        String type = PluginConfig.getString(section, "type");
        issueHelper.pushProperty("type");
        RateLimiter instance = null;
        try {
            instance = RATE_LIMITERS.stream()
                    .filter(limiter -> limiter.getId().equals(type))
                    .findAny().orElse(null);
            if (instance == null)
                issueHelper.appendAtPath(ConfigIssue.Level.WARNING, "No rate limiter of type \"%s\" could be found, possible typo?".formatted(type));
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(ConfigIssue.Level.ERROR, "Internal error during deserialization", e);
        }

        issueHelper.pop();
        return instance;
    }

    @YamlPropertyIssueAssigner(propertyName = "value")
    public void assignToValue(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (this.value.isBlank() && !actuallySet)
            issueHelper.appendAtPath(ConfigIssue.Level.HINT, "The value of the rate limiter was implicitly set to empty, since it is not directly specified in the config. Make sure to explicitly set value to empty in the config to dismiss this hint and avoid accidentally configuring your rate limiter wrong!");
    }

    private static Set<? extends RateLimiter> findRateLimiterClasses() {
        return ReflectUtil.findClasses(RateLimiter.class);
    }
}
