package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.message.Message;
import me.Domplanto.streamLabs.ratelimiter.RateLimiter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PluginConfig {
    private Map<String, List<Action>> actionsByEvent;
    private Map<String, CustomPlaceholder> customPlaceholders;
    private final Logger logger;

    public RewardsConfig(FileConfiguration config, Logger logger) {
        this.logger = logger;
        this.load(config);
    }

    public void load(FileConfiguration config) {
        this.actionsByEvent = new HashMap<>();
        this.customPlaceholders = new HashMap<>();
        ConfigurationSection actions = config.getConfigurationSection("actions");
        if (actions == null) return;

        for (String actionKey : actions.getKeys(false)) {
            ConfigurationSection actionSection = actions.getConfigurationSection(actionKey);
            if (actionSection == null) continue;

            // Store the action by its event type for easy lookup
            Action action = Action.deserialize(actionSection, this.logger);
            actionsByEvent.computeIfAbsent(action.getEventType(), k -> new ArrayList<>())
                    .add(action);
        }

        ConfigurationSection customPlaceholders = config.getConfigurationSection("custom_placeholders");
        if (customPlaceholders == null) return;

        for (String placeholderId : customPlaceholders.getKeys(false)) {
            ConfigurationSection placeholderSection = customPlaceholders.getConfigurationSection(placeholderId);
            if (placeholderSection == null) continue;

            List<CustomPlaceholder.StateBasedValue> values = placeholderSection.getKeys(false)
                    .stream()
                    .map(placeholderSection::getConfigurationSection)
                    .filter(Objects::nonNull)
                    .filter(section -> !section.getName().equals("default_value"))
                    .map(section -> new CustomPlaceholder.StateBasedValue(
                            section.getName(),
                            getString(section, "value"),
                            getStringList(section, "conditions"),
                            getStringList(section, "donation_conditions")
                    )).toList();

            this.customPlaceholders.put(placeholderId, new CustomPlaceholder(placeholderId,
                    getString(placeholderSection, "default_value"), values));
        }
    }

    @Nullable
    private static List<String> getStringList(ConfigurationSection section, String key) {
        return section.getKeys(true).contains(key) ? section.getStringList(key) : null;
    }

    @Nullable
    public static String getString(ConfigurationSection section, String key) {
        return section.getKeys(true).contains(key) ? section.getString(key) : null;
    }

    public List<Action> getActionsForEvent(String eventType) {
        return actionsByEvent.getOrDefault(eventType, List.of());
    }

    public Set<RateLimiter> fetchRateLimiters() {
        return actionsByEvent.values().stream()
                .flatMap(Collection::stream)
                .map(Action::getRateLimiter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Collection<CustomPlaceholder> getCustomPlaceholders() {
        return customPlaceholders.values();
    }

    public static class Action {
        private final String name;
        private final String eventType;
        private final List<Message> messages;
        private final boolean enabled;
        @Nullable
        private final List<String> conditionStrings;
        @Nullable
        private final List<String> donationConditionStrings;
        private final List<String> commands;
        @Nullable
        private final RateLimiter rateLimiter;

        public Action(String name, String eventType, boolean enabled, @Nullable RateLimiter rateLimiter, @Nullable List<String> messageStrings, @Nullable List<String> conditionStrings, @Nullable List<String> donationConditionStrings, @Nullable List<String> commands) {
            this.name = name;
            this.eventType = eventType;
            this.enabled = enabled;
            this.rateLimiter = rateLimiter;
            this.messages = messageStrings != null ? Message.parseAll(messageStrings) : List.of();
            this.conditionStrings = conditionStrings;
            this.donationConditionStrings = donationConditionStrings;
            this.commands = commands != null ? commands : List.of();
        }

        public static Action deserialize(ConfigurationSection section, Logger logger) {
            return new Action(
                    section.getName(),
                    section.getString("action", "unknown"),
                    section.getBoolean("enabled", true),
                    RateLimiter.deserialize(section.getConfigurationSection("rate_limiter"), logger),
                    getStringList(section, "messages"),
                    getStringList(section, "conditions"),
                    getStringList(section, "donation_conditions"),
                    getStringList(section, "commands")
            );
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

        @Nullable
        public RateLimiter getRateLimiter() {
            return rateLimiter;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public List<Condition> getConditions(StreamlabsEvent event) {
            if (this.conditionStrings == null) return new ArrayList<>();

            return Condition.parseAll(this.conditionStrings, event);
        }

        public List<Condition> getDonationConditions(BasicDonationEvent event, JsonObject baseObject) {
            if (this.donationConditionStrings == null) return new ArrayList<>();

            return Condition.parseDonationConditions(this.donationConditionStrings, event, baseObject);
        }

        public List<String> getCommands() {
            return commands;
        }
    }
}