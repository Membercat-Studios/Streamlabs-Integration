package com.membercat.streamlabs.config.versioning.version100;

import com.membercat.streamlabs.config.versioning.ConfigMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class StepsMigrator implements ConfigMigrator {
    public static List<ConfigurationSection> getSubSections(ConfigurationSection section) {
        return section.getKeys(false).stream()
                .map(section::getConfigurationSection)
                .filter(Objects::nonNull).toList();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public long getVersion() {
        return 100;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        List<ConfigurationSection> sections = new ArrayList<>();
        ConfigurationSection actions = root.getConfigurationSection("actions");
        ConfigurationSection goalTypes = root.getConfigurationSection("goal_types");
        if (actions != null) sections.addAll(getSubSections(actions));
        if (goalTypes != null) sections.addAll(getSubSections(goalTypes));

        sections.forEach(action -> {
            List<Map<String, ?>> steps = Stream.concat(
                    getSteps("messages", "message", action),
                    getSteps("commands", "command", action)
            ).toList();
            action.set("steps", steps);
        });
    }

    private Stream<? extends Map<String, ?>> getSteps(String id, String newName, ConfigurationSection section) {
        List<?> list = section.getList(id, null);
        if (list == null) return Stream.of();
        section.set(id, null);
        if (list.size() <= 2) {
            return list.stream()
                    .map(line -> Map.of(newName, line));
        }

        return Stream.of(Map.of(newName, list));
    }
}
