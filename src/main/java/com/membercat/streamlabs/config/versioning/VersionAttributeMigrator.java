package com.membercat.streamlabs.config.versioning;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class VersionAttributeMigrator implements ConfigMigrator {
    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public long getVersion() {
        return -1;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        root.set("version", targetVersion);
        root.setComments("version", List.of("DO NOT modify this value, it's used to keep track of the config version and not intended to be modified by the user!"));
    }
}
