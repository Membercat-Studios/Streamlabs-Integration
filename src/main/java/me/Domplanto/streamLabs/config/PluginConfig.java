package me.Domplanto.streamLabs.config;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.command.ActionCommand;
import me.Domplanto.streamLabs.action.message.Message;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import me.Domplanto.streamLabs.util.yaml.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.ES0;
import static me.Domplanto.streamLabs.config.issue.Issues.HI0;

public class PluginConfig extends ConfigRoot {
    @YamlProperty(value = "streamlabs")
    private PluginOptions options = new PluginOptions();
    @YamlProperty("affected_players")
    private List<String> affectedPlayers = new ArrayList<>();
    @YamlPropertySection(value = "actions", elementClass = Action.class)
    private Map<String, Action> actions = new HashMap<>();
    @YamlPropertySection(value = "custom_placeholders", elementClass = CustomPlaceholder.class)
    private Map<String, CustomPlaceholder> customPlaceholders = new HashMap<>();
    @YamlPropertySection(value = "goal_types", elementClass = DonationGoal.class)
    private Map<String, DonationGoal> goals = new HashMap<>();

    public PluginConfig(Logger logger) {
        super(logger);
    }

    @Override
    public void customLoad(@NotNull FileConfiguration config) {
    }

    public List<Action> getActionsForEvent(String eventType) {
        return actions.values().stream()
                .filter(action -> action.eventType.equals(eventType))
                .toList();
    }

    public Set<RateLimiter> fetchRateLimiters() {
        return Stream.concat(actions.values().stream(), goals.values().stream())
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
        return this.customPlaceholders.values();
    }

    public PluginOptions getOptions() {
        return this.options;
    }

    public Set<String> getAffectedPlayers() {
        return new HashSet<>(affectedPlayers);
    }

    public void setAffectedPlayers(StreamLabs plugin, Set<String> affectedPlayers) {
        this.affectedPlayers = new ArrayList<>(affectedPlayers);
        plugin.getConfig().set("affected_players", this.affectedPlayers);
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

    @ConfigPathSegment(id = "plugin_options")
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