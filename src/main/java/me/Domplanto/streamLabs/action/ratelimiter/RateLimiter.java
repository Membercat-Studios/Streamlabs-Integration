package me.Domplanto.streamLabs.action.ratelimiter;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
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

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "rate_limiter")
public abstract class RateLimiter implements YamlPropertyObject, Cloneable {
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

        String type = YamlPropertyObject.getString(section, "type");
        issueHelper.pushProperty("type");
        RateLimiter instance = null;
        try {
            instance = RATE_LIMITERS.stream()
                    .filter(limiter -> limiter.getId().equals(type))
                    .findAny().map(RateLimiter::createInstance).orElse(null);
            if (instance == null)
                issueHelper.appendAtPath(WR0.apply(type));
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(EI0, e);
        }

        issueHelper.pop();
        return instance;
    }

    public RateLimiter createInstance() {
        try {
            return (RateLimiter) this.clone();
        } catch (CloneNotSupportedException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    @YamlPropertyIssueAssigner(propertyName = "value")
    public void assignToValue(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (this.value.isBlank() && !actuallySet)
            issueHelper.appendAtPath(HR0);
    }

    private static Set<? extends RateLimiter> findRateLimiterClasses() {
        return ReflectUtil.initializeClasses(RateLimiter.class);
    }
}
