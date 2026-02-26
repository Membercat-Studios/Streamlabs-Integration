package com.membercat.streamlabs.config.versioning.version102;

import com.membercat.streamlabs.config.versioning.ConfigMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class AddDatabaseOptionsMigrator implements ConfigMigrator {
    @Override
    public long getVersion() {
        return 102;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        ConfigurationSection dbSection = root.getConfigurationSection("database");
        if (dbSection != null) return;
        dbSection = root.createSection("database");
        dbSection.set("provider", "sqlite");
        root.setComments("database", List.of("The database is used to store event history data"));
    }
}
