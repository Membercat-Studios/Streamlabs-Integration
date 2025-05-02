package me.Domplanto.streamLabs.config.versioning;

import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ConfigMigrator {
    Set<? extends ConfigMigrator> MIGRATORS = ReflectUtil.initializeClasses(ConfigMigrator.class);

    long getVersion();

    int getPriority();

    void apply(@NotNull ConfigurationSection root, long targetVersion);

    default boolean shouldUse(long targetVersion) {
        return getVersion() == -1 || getVersion() == targetVersion;
    }
}
