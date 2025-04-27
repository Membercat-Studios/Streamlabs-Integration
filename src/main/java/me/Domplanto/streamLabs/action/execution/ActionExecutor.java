package me.Domplanto.streamLabs.action.execution;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.AbstractStep;
import me.Domplanto.streamLabs.action.StepBase;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.socket.serializer.SocketSerializerException;
import me.Domplanto.streamLabs.statistics.EventHistory;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static me.Domplanto.streamLabs.config.issue.Issues.WA2;

public class ActionExecutor {
    private final PluginConfig pluginConfig;
    private final EventHistory eventHistory;
    private final StreamLabs plugin;
    private final ConcurrentMap<String, Set<UUID>> runningActions;
    @Nullable
    private DonationGoal activeGoal;

    public ActionExecutor(PluginConfig pluginConfig, StreamLabs plugin) {
        this.pluginConfig = pluginConfig;
        this.plugin = plugin;
        this.eventHistory = new EventHistory();
        this.runningActions = new ConcurrentHashMap<>();
    }

    public void parseAndExecute(JsonElement data) throws SocketSerializerException {
        try {
            if (data.getAsJsonArray().size() < 2 || !data.getAsJsonArray().get(1).isJsonObject()) {
                if (StreamLabs.isDebugMode())
                    plugin.getLogger().info("Skipping streamlabs message with invalid formatting");
                return;
            }

            JsonObject object = data.getAsJsonArray().get(1).getAsJsonObject();
            String type = object.get("type").getAsString();
            if (StreamLabs.isDebugMode() && (!type.equals("alertPlaying") && !type.equals("streamlabels") && !type.equals("streamlabels.underlying")))
                plugin.getLogger().info(String.format("Streamlabs message: %s", data));

            String platform = object.has("for") ? object.get("for").getAsString() : "streamlabs";
            Set<? extends StreamlabsEvent> events = StreamLabs.getCachedEventObjects().stream()
                    .filter(e -> e.getApiName().equals(type) && e.getPlatform().compare(platform))
                    .collect(Collectors.toSet());

            for (StreamlabsEvent event : events) {
                JsonObject baseObject = event.getBaseObject(object);
                if (!this.checkAndExecute(event, baseObject))
                    Translations.sendPrefixedToPlayers("streamlabs.error.action_failure", ColorScheme.ERROR, plugin.getServer());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON content of a streamlabs message:", e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkAndExecute(StreamlabsEvent event, JsonObject baseObject) {
        event.onExecute(this, baseObject);
        this.eventHistory.store(event, this.pluginConfig, baseObject);
        List<PluginConfig.Action> actions = pluginConfig.getActionsForEvent(event.getId());
        boolean successful = true;
        for (PluginConfig.Action action : actions) {
            try {
                if (!action.enabled) continue;

                ActionExecutionContext context = new ActionExecutionContext(event, this, this.pluginConfig, action, baseObject);
                if (event.checkConditions(context)) executeAction(context);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing action %s for %s:".formatted(action.id, event.getId()), e);
                successful = false;
            }
        }

        this.updateGoal(new ActionExecutionContext(event, this, this.pluginConfig, null, baseObject));
        return successful;
    }

    private void executeAction(ActionExecutionContext ctx) {
        String actionId = ctx.action().id;
        Set<UUID> instances = runningActions.containsKey(actionId) ? this.runningActions.get(actionId) : new HashSet<>();
        if (!instances.isEmpty() && ctx.action().instancingBehaviour == ActionInstancingBehaviour.CANCEL_PREVIOUS)
            instances.clear();

        UUID taskUUID = UUID.randomUUID();
        instances.add(taskUUID);
        this.runningActions.put(actionId, instances);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            int id = 0;
            for (StepBase<?> step : ctx.action().steps) {
                if (!runningActions.get(actionId).contains(taskUUID)) return;
                if (!ctx.shouldExecute().get()) break;
                try {
                    step.execute(ctx, this.plugin);
                } catch (AbstractStep.ActionFailureException e) {
                    plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing step %s in action %s for event %s: %s".formatted(id, actionId, ctx.event().getId(), e.getMessage()), e.getCause());
                }
                id++;
            }
            Set<UUID> instanceSet = this.runningActions.get(actionId);
            instanceSet.remove(taskUUID);
            if (instanceSet.isEmpty()) this.runningActions.remove(actionId);
        });
    }

    public void updateGoal(ActionExecutionContext ctx) {
        try {
            if (this.activeGoal == null) return;
            DonationGoal goal = pluginConfig.getGoal(this.activeGoal.id);
            if (goal == null) {
                this.removeGoal();
                return;
            }

            if (!this.activeGoal.add(ctx)) return;
            this.executeAction(new ActionExecutionContext(null, this, this.pluginConfig, goal, null));
            this.stopGoal();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error while updating goal %s on event %s:".formatted(ctx.event().getId(), this.activeGoal), e);
        }
    }

    public void activateGoal(DonationGoal goal, int max) {
        if (this.activeGoal != null)
            this.activeGoal.reset();
        this.activeGoal = goal.start(max);
    }

    public void removeGoal() {
        this.stopGoal();
        this.activeGoal = null;
    }

    public void stopGoal() {
        if (activeGoal == null) return;
        this.activeGoal.disable();
    }

    @Nullable
    public DonationGoal getActiveGoal() {
        return activeGoal;
    }

    public EventHistory getEventHistory() {
        return eventHistory;
    }

    public enum ActionInstancingBehaviour {
        CANCEL_PREVIOUS,
        RUN_IN_PARALLEL;

        public static @NotNull ActionExecutor.ActionInstancingBehaviour fromString(@NotNull String s, ConfigIssueHelper issueHelper) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException e) {
                issueHelper.appendAtPath(WA2.apply(s));
                return CANCEL_PREVIOUS;
            }
        }
    }
}
