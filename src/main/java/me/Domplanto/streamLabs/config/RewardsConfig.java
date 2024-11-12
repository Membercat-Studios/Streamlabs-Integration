package me.Domplanto.streamLabs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class RewardsConfig {
    private final Map<String, List<Action>> actionsByEvent;

    public RewardsConfig(FileConfiguration config) {
        this.actionsByEvent = new HashMap<>();
        loadConfig(config);
    }

    private void loadConfig(FileConfiguration config) {
        ConfigurationSection actions = config.getConfigurationSection("actions");
        if (actions == null) return;

        for (String actionKey : actions.getKeys(false)) {
            ConfigurationSection actionSection = actions.getConfigurationSection(actionKey);
            if (actionSection == null) continue;

            Action action = new Action(
                    actionKey,
                    actionSection.getString("action"),
                    actionSection.getBoolean("enabled", true),
                    actionSection.getDouble("threshold", 0.0),
                    actionSection.getStringList("commands")
            );

            // Store the action by its event type for easy lookup
            actionsByEvent.computeIfAbsent(action.getEventType(), k -> new ArrayList<>())
                    .add(action);
        }
    }

    public List<Action> getActionsForEvent(String eventType) {
        return actionsByEvent.getOrDefault(eventType, List.of());
    }

    public static class Action {
        private final String name;
        private final String eventType;
        private final boolean enabled;
        private final double threshold;
        private final List<String> commands;

        public Action(String name, String eventType, boolean enabled, double threshold, List<String> commands) {
            this.name = name;
            this.eventType = eventType;
            this.enabled = enabled;
            this.threshold = threshold;
            this.commands = commands;
        }

        public String getName() {
            return name;
        }

        public String getEventType() {
            return eventType;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public double getThreshold() {
            return threshold;
        }

        public List<String> getCommands() {
            return commands;
        }
    }
}