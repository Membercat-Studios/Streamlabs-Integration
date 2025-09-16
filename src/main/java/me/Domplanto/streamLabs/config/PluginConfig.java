package me.Domplanto.streamLabs.config;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.ActionExecutor;
import me.Domplanto.streamLabs.action.StepExecutor;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.placeholder.CustomPlaceholder;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import me.Domplanto.streamLabs.step.AbstractStep;
import me.Domplanto.streamLabs.step.StepBase;
import me.Domplanto.streamLabs.util.yaml.*;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

public class PluginConfig extends ConfigRoot {
    @YamlProperty(value = "streamlabs")
    private PluginOptions options = new PluginOptions();
    @YamlProperty("affected_players")
    private List<String> affectedPlayers = new ArrayList<>();
    @YamlPropertySection(value = "goal_types", elementClass = DonationGoal.class)
    private Map<String, DonationGoal> goals = new HashMap<>();
    @YamlPropertySection(value = "actions", elementClass = Action.class)
    private Map<String, Action> actions = new HashMap<>();
    @YamlPropertySection(value = "custom_placeholders", elementClass = CustomPlaceholder.class)
    private Map<String, CustomPlaceholder> customPlaceholders = new HashMap<>();
    @YamlPropertySection(value = "functions", elementClass = Function.class)
    private Map<String, Function> functions = new HashMap<>();

    public PluginConfig(ComponentLogger logger) {
        super(logger);
    }

    @Override
    public void customLoad(@NotNull FileConfiguration config) {
    }

    public List<Action> getActionsForEvent(@NotNull StreamlabsEvent event) {
        return actions.values().stream()
                .filter(action -> action.shouldHandle(event))
                .toList();
    }

    public Set<RateLimiter> fetchRateLimiters() {
        return Stream.concat(actions.values().stream(), goals.values().stream())
                .map(action -> action.rateLimiter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public @Nullable DonationGoal getGoal(@NotNull String id) {
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

    public @Nullable Function getFunction(@NotNull String id) {
        return this.functions.get(id);
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

    @SuppressWarnings("rawtypes")
    public static abstract class AbstractAction extends ConditionGroup implements StepExecutor {
        @YamlProperty("!SECTION")
        @NotNull
        public String id;
        @YamlProperty("instancing_behavior")
        public ActionExecutor.ActionInstancingBehaviour instancingBehaviour = ActionExecutor.ActionInstancingBehaviour.CANCEL_PREVIOUS;
        @YamlProperty("queue_delay")
        public long queueDelay = 0;
        @YamlProperty("stop_on_failure")
        public boolean stopOnFailure;
        @Nullable
        @YamlProperty("rate_limiter")
        public RateLimiter rateLimiter;
        @YamlProperty("steps")
        private List<? extends StepBase> steps = List.of();

        @YamlPropertyCustomDeserializer(propertyName = "steps")
        private List<? extends StepBase> deserializeSteps(@NotNull List<Object> sections, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
            return AbstractStep.INITIALIZER.parseAll(sections, parent, issueHelper);
        }

        @YamlPropertyCustomDeserializer(propertyName = "instancing_behavior")
        private ActionExecutor.ActionInstancingBehaviour deserializeBehavior(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
            return ActionExecutor.ActionInstancingBehaviour.fromString(input, issueHelper);
        }

        @Override
        public boolean check(ActionExecutionContext ctx) {
            if (!ctx.bypassRateLimiters() && (rateLimiter != null && !rateLimiter.check(ctx))) return false;
            return super.check(ctx);
        }

        @Override
        public @NotNull String getName() {
            return "action %s".formatted(this.id);
        }

        @Override
        public @NotNull Collection<? extends StepBase> getSteps(ActionExecutionContext ctx) {
            return this.steps;
        }
    }

    @ConfigPathSegment(id = "action")
    public static class Action extends AbstractAction {
        private static final Set<String> ACTION_IDS = StreamLabs.getCachedEventObjects().stream()
                .map(StreamlabsEvent::getId).collect(Collectors.toSet());
        @YamlProperty("events")
        private Set<String> eventTypes = Set.of();
        @YamlProperty("enabled")
        private boolean enabled = true;

        @YamlPropertyCustomDeserializer(propertyName = "events")
        public Set<String> deserializeEventTypes(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
            String[] data = input.split(",");
            return Arrays.stream(data)
                    .map(String::strip)
                    .distinct()
                    .filter(type -> {
                        boolean filter = ACTION_IDS.contains(type);
                        if (!filter) issueHelper.appendAtPath(WA0.apply(type));
                        return filter;
                    }).collect(Collectors.toSet());
        }

        @YamlPropertyIssueAssigner(propertyName = "events")
        public void assignToAction(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (!actuallySet) issueHelper.appendAtPath(WA1);
        }

        public boolean shouldHandle(@NotNull StreamlabsEvent event) {
            return this.enabled && this.eventTypes.contains(event.getId());
        }
    }

    @SuppressWarnings("rawtypes")
    @ConfigPathSegment(id = "function")
    public static class Function implements StepExecutor, YamlPropertyObject {
        @YamlProperty("!SECTION")
        @NotNull
        public String id;
        @YamlProperty("steps")
        private List<? extends StepBase> steps = List.of();
        @YamlProperty("output")
        private @Nullable String output;

        @YamlPropertyCustomDeserializer(propertyName = "steps")
        private List<? extends StepBase> deserializeSteps(@NotNull List<Object> sections, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
            return AbstractStep.INITIALIZER.parseAll(sections, parent, issueHelper);
        }

        @Override
        public @NotNull String getName() {
            return "function %s".formatted(this.id);
        }

        @Override
        public @NotNull Collection<? extends StepBase> getSteps(ActionExecutionContext ctx) {
            return this.steps;
        }

        public @Nullable String getOutput() {
            return this.output;
        }
    }

    @ConfigPathSegment(id = "plugin_options")
    public static class PluginOptions implements YamlPropertyObject {
        @YamlProperty("socket_token")
        public String socketToken = "";
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
            try {
                StreamlabsSocketClient.createURI(this.socketToken);
            } catch (IllegalArgumentException e) {
                if (!(e.getCause() instanceof URISyntaxException syntaxException)
                        || !syntaxException.getMessage().contains("Illegal character")
                        || syntaxException.getIndex() == -1)
                    issueHelper.appendAtPath(WS0);
                else {
                    int start = StreamlabsSocketClient.getURIString(socketToken).indexOf(socketToken);
                    int index = syntaxException.getIndex() - start;
                    String character = String.valueOf(this.socketToken.charAt(index));
                    issueHelper.appendAtPath(WS0D.apply(character, index));
                }
                this.socketToken = "";
            }
        }

        @YamlPropertyIssueAssigner(propertyName = "debug_mode")
        private void assignToDebugMode(ConfigIssueHelper issueHelper, boolean actuallySet) {
            if (this.debugMode)
                issueHelper.appendAtPath(HI0);
        }
    }
}