package me.Domplanto.streamLabs.ratelimiter;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public abstract class RateLimiter implements YamlPropertyObject {
    @NotNull
    private final String id;
    private final String value;

    public RateLimiter(@NotNull String id, String value) {
        this.id = id;
        this.value = value;
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
    public static RateLimiter deserialize(ConfigurationSection section, Logger logger) {
        if (section == null) return null;
        String type = RewardsConfig.getString(section, "type");
        RateLimiter instance = RateLimiter.create(
                type,
                RewardsConfig.getString(section, "value")
        );
        if (instance == null) {
            logger.warning("A rate limiter of type \"%s\" could not be found".formatted(type));
            return null;
        }
        try {
            instance.acceptYamlProperties(section);
        } catch (ReflectiveOperationException ignore) {
        }

        return instance;
    }

    @Nullable
    public static RateLimiter create(String id, String value) {
        return ReflectUtil.findClasses(RateLimiter.class, value).stream()
                .filter(limiter -> limiter.getId().equals(id))
                .findAny().orElse(null);
    }
}
