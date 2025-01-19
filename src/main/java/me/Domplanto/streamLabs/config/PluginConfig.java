package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.message.Message;
import me.Domplanto.streamLabs.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PluginConfig {
    private Map<String, List<Action>> actionsByEvent;
    private Map<String, CustomPlaceholder> customPlaceholders;
    private PluginOptions options;
    private final ConfigIssueHelper issueHelper;

    public PluginConfig(Logger logger) {
        this.issueHelper = new ConfigIssueHelper(logger);
    }

    public void load(FileConfiguration config) throws ConfigLoadedWithIssuesException {
        this.issueHelper.reset();
        this.actionsByEvent = new HashMap<>();
        this.customPlaceholders = new HashMap<>();

        issueHelper.newSection("streamlabs");
        this.options = YamlPropertyObject.createInstance(PluginOptions.class, config.getConfigurationSection("streamlabs"), issueHelper);

        issueHelper.newSection("actions");
        ConfigurationSection actions = config.getConfigurationSection("actions");
        for (String actionKey : getSectionKeys(actions)) {
            assert actions != null;
            issueHelper.push(Action.class, actionKey);
            try {
                ConfigurationSection actionSection = actions.getConfigurationSection(actionKey);
                if (actionSection == null) {
                    issueHelper.pop();
                    continue;
                }

                // Store the action by its event type for easy lookup
                Action action = YamlPropertyObject.createInstance(Action.class, actionSection, issueHelper);
                actionsByEvent.computeIfAbsent(Objects.requireNonNull(action).eventType, k -> new ArrayList<>())
                        .add(action);
            } catch (Exception e) {
                issueHelper.appendAtPathAndLog(ConfigIssue.Level.ERROR, "Internal error during deserialization", e);
            }
            issueHelper.pop();
        }

        issueHelper.newSection("custom_placeholders");
        ConfigurationSection customPlaceholders = config.getConfigurationSection("custom_placeholders");
        for (String placeholderId : getSectionKeys(customPlaceholders)) {
            assert customPlaceholders != null;
            issueHelper.push(CustomPlaceholder.class, placeholderId);
            try {
                ConfigurationSection placeholderSection = customPlaceholders.getConfigurationSection(placeholderId);
                if (placeholderSection == null) {
                    issueHelper.pop();
                    continue;
                }

                CustomPlaceholder placeholder = CustomPlaceholder.deserialize(placeholderSection, issueHelper);
                this.customPlaceholders.put(placeholderId, placeholder);
            } catch (Exception e) {
                issueHelper.appendAtPathAndLog(ConfigIssue.Level.ERROR, "Internal error during deserialization", e);
            }
            issueHelper.pop();
        }

        this.issueHelper.pop();
        this.issueHelper.complete();
    }

    @Nullable
    public static List<String> getStringList(ConfigurationSection section, String key) {
        return section.getKeys(true).contains(key) ? section.getStringList(key) : null;
    }

    @Nullable
    public static String getString(ConfigurationSection section, String key) {
        return section.getKeys(true).contains(key) ? section.getString(key) : null;
    }

    @NotNull
    public static Set<String> getSectionKeys(@Nullable ConfigurationSection section) {
        return getSectionKeys(section, false);
    }

    @NotNull
    public static Set<String> getSectionKeys(@Nullable ConfigurationSection section, boolean recursive) {
        if (section == null) return new HashSet<>();
        return section.getKeys(recursive);
    }

    public List<Action> getActionsForEvent(String eventType) {
        return actionsByEvent.getOrDefault(eventType, List.of());
    }

    public Set<RateLimiter> fetchRateLimiters() {
        return actionsByEvent.values().stream()
                .flatMap(Collection::stream)
                .map(action -> action.rateLimiter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Collection<CustomPlaceholder> getCustomPlaceholders() {
        return customPlaceholders.values();
    }

    public PluginOptions getOptions() {
        return options;
    }

    @ConfigPathSegment(id = "action")
    public static class Action implements YamlPropertyObject {
        @YamlProperty("action")
        public String eventType = "unknown";
        @YamlProperty("enabled")
        public boolean enabled;
        @Nullable
        @YamlProperty("conditions")
        private List<String> conditionStrings;
        @Nullable
        @YamlProperty("donation_conditions")
        private List<String> donationConditionStrings;
        @YamlProperty("messages")
        public List<Message> messages = List.of();
        @YamlProperty("commands")
        public List<String> commands = List.of();
        @Nullable
        @YamlProperty("rate_limiter")
        public RateLimiter rateLimiter;

        @YamlPropertyCustomDeserializer(propertyName = "messages")
        private List<Message> deserializeMessages(@NotNull List<String> messageStrings) {
            return Message.parseAll(messageStrings);
        }

        public List<Condition> getConditions(StreamlabsEvent event) {
            if (this.conditionStrings == null) return new ArrayList<>();

            return Condition.parseAll(this.conditionStrings, event);
        }

        public List<Condition> getDonationConditions(BasicDonationEvent event, JsonObject baseObject) {
            if (this.donationConditionStrings == null) return new ArrayList<>();

            return Condition.parseDonationConditions(this.donationConditionStrings, event, baseObject);
        }
    }

    public static class PluginOptions implements YamlPropertyObject {
        @YamlProperty("socket_token")
        public String socketToken;
        @YamlProperty("debug_mode")
        public boolean debugMode = false;
        @YamlProperty("show_status_messages")
        public boolean showStatusMessages = true;

        @YamlPropertyIssueAssigner(propertyName = "socket_token")
        private void assignToSocketToken(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (!actuallySet || this.socketToken.isBlank())
                issueHelper.appendAtPath(ConfigIssue.Level.ERROR, "Your socket token has not been configured yet, make sure to follow our guide on setting up this plugin!");
        }

        @YamlPropertyIssueAssigner(propertyName = "debug_mode")
        private void assignToDebugMode(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (this.debugMode)
                issueHelper.appendAtPath(ConfigIssue.Level.HINT, "Debug mode should ONLY be used for development or to help with reporting issues, it will spam your console with Streamlabs API data!");
        }
    }
}