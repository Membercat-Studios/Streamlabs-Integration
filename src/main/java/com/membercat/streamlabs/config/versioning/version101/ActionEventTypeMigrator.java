package com.membercat.streamlabs.config.versioning.version101;

import com.membercat.streamlabs.config.versioning.ConfigMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import static com.membercat.streamlabs.config.versioning.version100.StepsMigrator.getSubSections;

@SuppressWarnings("unused")
public class ActionEventTypeMigrator implements ConfigMigrator {
    @Override
    public long getVersion() {
        return 101;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        ConfigurationSection actions = root.getConfigurationSection("actions");
        if (actions == null) return;

        for (ConfigurationSection action : getSubSections(actions)) {
            String eventType = action.getString("action");
            if (eventType == null) continue;
            action.set("action", null);
            action.set("events", eventType);
        }
    }
}
