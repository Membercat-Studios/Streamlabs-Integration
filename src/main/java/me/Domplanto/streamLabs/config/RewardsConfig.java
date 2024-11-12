package me.Domplanto.streamLabs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RewardsConfig {
    private final Map<String, List<String>> eventCommands;
    private final Map<String, Double> eventThresholds;

    public RewardsConfig(FileConfiguration config) {
        eventCommands = new HashMap<>();
        eventThresholds = new HashMap<>();
        loadConfig(config);
    }

    private void loadConfig(FileConfiguration config) {
        ConfigurationSection events = config.getConfigurationSection("events");
        if (events != null) {
            for (String eventName : events.getKeys(false)) {
                ConfigurationSection eventSection = events.getConfigurationSection(eventName);
                if (eventSection != null) {
                    eventCommands.put(eventName, eventSection.getStringList("commands"));
                    eventThresholds.put(eventName, eventSection.getDouble("threshold", 0.0));
                }
            }
        }
    }

    public List<String> getCommands(String eventType) {
        return eventCommands.getOrDefault(eventType, List.of());
    }

    public double getThreshold(String eventType) {
        return eventThresholds.getOrDefault(eventType, 0.0);
    }
}
