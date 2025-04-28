package me.Domplanto.streamLabs.action.ratelimiter;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "rate_limiter")
public abstract class RateLimiter implements YamlPropertyObject {
    private static final Map<String, Class<? extends RateLimiter>> RATE_LIMITER_CLASSES = RateLimiter.findRateLimiterClasses();
    @YamlProperty("value")
    private String value = "";

    @Nullable
    @YamlPropertyCustomDeserializer
    public static RateLimiter deserialize(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        if (section == null) return null;

        String type = YamlPropertyObject.getString(section, "type");
        issueHelper.process("type");
        issueHelper.pushProperty("type");
        RateLimiter instance = null;
        try {
            instance = RATE_LIMITER_CLASSES.entrySet().stream()
                    .filter(limiter -> limiter.getKey().equals(type))
                    .findAny().map(Map.Entry::getValue)
                    .map(cls -> ReflectUtil.instantiate(cls, RateLimiter.class))
                    .orElse(null);
            if (instance == null)
                issueHelper.appendAtPath(WR0.apply(type));
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(EI0, e);
        }

        issueHelper.pop();
        return instance;
    }

    private static Map<String, Class<? extends RateLimiter>> findRateLimiterClasses() {
        return ReflectUtil.loadClassesWithIds(RateLimiter.class, true);
    }

    public abstract boolean check(ActionExecutionContext ctx);

    public abstract void reset();

    public String getValue(ActionExecutionContext ctx) {
        return ActionPlaceholder.replacePlaceholders(this.value, ctx);
    }

    @YamlPropertyIssueAssigner(propertyName = "value")
    public void assignToValue(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (this.value.isBlank() && !actuallySet)
            issueHelper.appendAtPath(HR0);
    }
}
