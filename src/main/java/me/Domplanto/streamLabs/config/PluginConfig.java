package me.Domplanto.streamLabs.config;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.command.ActionCommand;
import me.Domplanto.streamLabs.action.message.Message;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

public class PluginConfig {
    private Map<String, List<Action>> actionsByEvent;
    private Set<String> affectedPlayers;
    private Map<String, CustomPlaceholder> customPlaceholders;
    private Map<String, DonationGoal> goals;
    private PluginOptions options;
    private final ConfigIssueHelper issueHelper;

    public PluginConfig(Logger logger) {
        this.issueHelper = new ConfigIssueHelper(logger);
    }

    public void load(FileConfiguration config) throws ConfigLoadedWithIssuesException {
        this.issueHelper.reset();
        this.actionsByEvent = new HashMap<>();
        this.affectedPlayers = new HashSet<>();
        this.customPlaceholders = new HashMap<>();
        this.goals = new HashMap<>();
        if (getSectionKeys(config, false).contains("__suppress"))
            issueHelper.suppressGlobally(config.getStringList("__suppress"));

        issueHelper.newSection("streamlabs");
        this.options = YamlPropertyObject.createInstance(PluginOptions.class, config.getConfigurationSection("streamlabs"), issueHelper);

        issueHelper.newSection("affected_players");
        if (getSectionKeys(config, false).contains("affected_players"))
            this.affectedPlayers = new HashSet<>(config.getStringList("affected_players"));

        // Store the action by its event type for easy lookup
        this.loadSection(config, "actions", Action.class, (id, action) ->
                actionsByEvent.computeIfAbsent(Objects.requireNonNull(action).eventType, k -> new ArrayList<>()).add(action));
        this.loadSection(config, "custom_placeholders", CustomPlaceholder.class, (id, placeholder) ->
                this.customPlaceholders.put(id, placeholder));
        this.loadSection(config, "goal_types", DonationGoal.class, (id, goal) ->
                this.goals.put(id, goal));

        this.issueHelper.pop();
        this.issueHelper.complete();
    }

    private <T extends YamlPropertyObject> void loadSection(FileConfiguration config, String sectionId, Class<T> cls, BiConsumer<String, T> action) {
        issueHelper.newSection(sectionId);
        ConfigurationSection section = config.getConfigurationSection(sectionId);
        for (String key : getSectionKeys(section)) {
            assert section != null;
            issueHelper.push(cls, key);
            try {
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection == null) {
                    issueHelper.pop();
                    continue;
                }

                T element = YamlPropertyObject.createInstance(cls, subSection, issueHelper);
                action.accept(key, element);
            } catch (Exception e) {
                issueHelper.appendAtPathAndLog(EI0, e);
            }
            issueHelper.pop();
        }
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
        return Stream.concat(actionsByEvent.values().stream().flatMap(Collection::stream), goals.values().stream())
                .map(action -> action.rateLimiter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Nullable
    public DonationGoal getGoal(String id) {
        return this.goals.get(id);
    }

    public Collection<DonationGoal> getGoals() {
        return this.goals.values();
    }

    public Collection<CustomPlaceholder> getCustomPlaceholders() {
        return customPlaceholders.values();
    }

    public PluginOptions getOptions() {
        return options;
    }

    public Set<String> getAffectedPlayers() {
        return affectedPlayers;
    }

    public void setAffectedPlayers(StreamLabs plugin, Set<String> affectedPlayers) {
        this.affectedPlayers = affectedPlayers;
        plugin.getConfig().set("affected_players", new ArrayList<>(affectedPlayers));
        plugin.saveConfig();
        plugin.reloadConfig();
    }

    public static abstract class AbstractAction extends ConditionGroup {
        @YamlProperty("!SECTION")
        @NotNull
        public String id;
        @YamlProperty("messages")
        public List<Message> messages = List.of();
        @YamlProperty("commands")
        public List<ActionCommand> commands = List.of();
        @Nullable
        @YamlProperty("rate_limiter")
        public RateLimiter rateLimiter;

        @YamlPropertyCustomDeserializer(propertyName = "messages")
        private List<Message> deserializeMessages(@NotNull List<String> messageStrings, ConfigIssueHelper issueHelper) {
            return Message.parseAll(messageStrings, issueHelper);
        }

        @YamlPropertyCustomDeserializer(propertyName = "commands")
        private List<ActionCommand> deserializeCommands(@NotNull List<String> rawCommands, ConfigIssueHelper issueHelper) {
            return ActionCommand.parseAll(rawCommands, issueHelper);
        }
    }

    @ConfigPathSegment(id = "action")
    public static class Action extends AbstractAction {
        @YamlProperty("action")
        public String eventType = "unknown";
        @YamlProperty("enabled")
        public boolean enabled;
    }

    public static class PluginOptions implements YamlPropertyObject {
        @YamlProperty("socket_token")
        public String socketToken;
        @YamlProperty("debug_mode")
        public boolean debugMode = false;
        @YamlProperty("show_status_messages")
        public boolean showStatusMessages = true;
        @YamlProperty("auto_connect")
        public boolean autoConnect = true;

        @YamlPropertyIssueAssigner(propertyName = "socket_token")
        private void assignToSocketToken(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (!actuallySet || this.socketToken.isBlank())
                issueHelper.appendAtPath(ES0);
        }

        @YamlPropertyIssueAssigner(propertyName = "debug_mode")
        private void assignToDebugMode(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (this.debugMode)
                issueHelper.appendAtPath(HI0);
        }
    }
}