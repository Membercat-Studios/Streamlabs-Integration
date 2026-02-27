package com.membercat.streamlabs.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.socket.serializer.SocketSerializerException;
import com.membercat.streamlabs.statistics.EventHistory;
import com.membercat.streamlabs.statistics.goal.DonationGoal;
import com.membercat.streamlabs.util.components.Translations;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.membercat.streamlabs.config.issue.Issues.WA2;

public class ActionExecutor {
    private final PluginConfig pluginConfig;
    private final EventHistory eventHistory;
    private final StreamlabsIntegration plugin;
    private final ConcurrentMap<String, Set<UUID>> runningActions;
    private final ConcurrentMap<String, Queue<ActionExecutionContext>> queuedActions;
    private final Queue<ActionExecutionContext> globalQueue;
    @Nullable
    private DonationGoal activeGoal;

    public ActionExecutor(PluginConfig pluginConfig, StreamlabsIntegration plugin) {
        this.pluginConfig = pluginConfig;
        this.plugin = plugin;
        this.eventHistory = new EventHistory(plugin::dbManager);
        this.runningActions = new ConcurrentHashMap<>();
        this.queuedActions = new ConcurrentHashMap<>();
        this.globalQueue = new ConcurrentLinkedQueue<>();
    }

    public void parseAndExecute(JsonElement data) throws SocketSerializerException {
        try {
            if (data.getAsJsonArray().size() < 2 || !data.getAsJsonArray().get(1).isJsonObject()) {
                if (StreamlabsIntegration.isDebugMode())
                    plugin.getLogger().info("Skipping streamlabs message with invalid formatting");
                return;
            }

            JsonObject object = data.getAsJsonArray().get(1).getAsJsonObject();
            String type = object.get("type").getAsString();
            if (StreamlabsIntegration.isDebugMode() && (!type.equals("alertPlaying") && !type.equals("streamlabels") && !type.equals("streamlabels.underlying")))
                plugin.getLogger().info(String.format("Streamlabs message: %s", data));

            String platform = object.has("for") ? object.get("for").getAsString() : "streamlabs";
            Set<? extends StreamlabsEvent> events = StreamlabsIntegration.getCachedEventObjects().stream()
                    .filter(e -> e.getApiName().equals(type) && e.getPlatform().compare(platform))
                    .collect(Collectors.toSet());

            for (StreamlabsEvent event : events) {
                JsonObject baseObject = event.getBaseObject(object);
                if (!this.checkAndExecute(event, baseObject, false, false))
                    Translations.sendPrefixedToPlayers(Translations.ACTION_FAILURE, plugin.getServer(), false);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON content of a streamlabs message:", e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkAndExecute(StreamlabsEvent event, JsonObject baseObject, boolean bypassRateLimiters, boolean isTest) {
        if (!event.isEventValid(baseObject)) return true;
        event.onExecute(this, baseObject);
        this.eventHistory.store(event, this.pluginConfig, baseObject, isTest);
        List<PluginConfig.Action> actions = pluginConfig.getActionsForEvent(event);
        boolean successful = true;
        for (PluginConfig.Action action : actions) {
            try {
                this.executeAction(new ActionExecutionContext(event, this, this.pluginConfig, action, bypassRateLimiters, false, baseObject));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing action %s for %s:".formatted(action.id, event.getId()), e);
                successful = false;
            }
        }

        this.updateGoal(new ActionExecutionContext(event, this, this.pluginConfig, null, bypassRateLimiters, false, baseObject));
        return successful;
    }

    public void executeAction(ActionExecutionContext ctx) {
        if (!ctx.checkConditions()) return;
        String actionId = ctx.action().id;
        Set<UUID> instances = runningActions.computeIfAbsent(actionId, s -> new HashSet<>());
        if (getInstanceCount() != 0 && ctx.action().instancingBehaviour == ActionInstancingBehaviour.GLOBAL_QUEUE) {
            this.globalQueue.add(ctx);
            StreamlabsIntegration.LOGGER.info("New execution of action %s will be queued until all other running actions are done. Global queue size is now: %s".formatted(actionId, globalQueue.size()));
            return;
        }
        if (!instances.isEmpty()) switch (ctx.action().instancingBehaviour) {
            case CANCEL_PREVIOUS -> instances.clear();
            case QUEUE -> {
                Queue<ActionExecutionContext> queue = this.queuedActions.computeIfAbsent(actionId, s -> new ConcurrentLinkedQueue<>());
                queue.add(ctx);
                StreamlabsIntegration.LOGGER.info("Queueing new execution of action %s since it is already running. Queue size is now: %s".formatted(actionId, queue.size()));
                return;
            }
        }

        UUID taskUUID = UUID.randomUUID();
        instances.add(taskUUID);
        ctx.setKeepExecutingCheck(context -> runningActions.get(actionId).contains(taskUUID) && context.shouldExecute().get());
        Bukkit.getAsyncScheduler().runNow(this.plugin, task -> {
            ctx.runSteps(ctx.action(), plugin);
            if (ctx.dirty().get())
                Translations.sendPrefixedToPlayers(Translations.ACTION_FAILURE, plugin.getServer(), false);
            Set<UUID> instanceSet = this.runningActions.get(actionId);
            instanceSet.remove(taskUUID);
            if (instanceSet.isEmpty()) this.runningActions.remove(actionId);

            if (queuedActions.containsKey(actionId)) {
                Queue<ActionExecutionContext> queue = queuedActions.get(actionId);
                ActionExecutionContext queuedCtx = queue.poll();
                assert queuedCtx != null;
                if (queue.isEmpty()) queuedActions.remove(actionId);
                StreamlabsIntegration.LOGGER.info("Action %s has finished, now executing queued runs. Queue size is now: %s".formatted(actionId, queue.size()));
                try {
                    Thread.sleep(queuedCtx.action().queueDelay);
                } catch (InterruptedException ignored) {
                }
                this.executeAction(queuedCtx);
                return;
            }
            if (getInstanceCount() == 0 && !globalQueue.isEmpty()) {
                ActionExecutionContext globalQueuedCtx = globalQueue.poll();
                StreamlabsIntegration.LOGGER.info("All actions have finished, now executing globally queued runs. Global queue size is now: %s".formatted(globalQueue.size()));
                try {
                    Thread.sleep(globalQueuedCtx.action().queueDelay);
                } catch (InterruptedException ignored) {
                }
                this.executeAction(globalQueuedCtx);
            }
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
            this.executeAction(new ActionExecutionContext(null, this, this.pluginConfig, goal, false, true, null));
            this.stopGoal();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error while updating goal %s on event %s:".formatted(ctx.event().getId(), this.activeGoal), e);
        }
    }

    public void shutdown() {
        StreamlabsIntegration.LOGGER.info("Attempting to cancel active action runs...");
        this.runningActions.clear();
        this.globalQueue.clear();
        this.queuedActions.clear();
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

    public int getRunningCount() {
        return this.runningActions.size();
    }

    public long getInstanceCount() {
        return this.runningActions.values()
                .stream().mapToLong(Collection::size).sum();
    }

    public int getInstanceCount(@NotNull String actionId) {
        return this.runningActions.getOrDefault(actionId, Set.of()).size();
    }

    public int getQueuedCount() {
        return this.queuedActions.size();
    }

    public long getQueuedInstanceCount() {
        return this.queuedActions.values()
                .stream().mapToLong(Collection::size).sum();
    }

    public int getQueuedInstanceCount(@NotNull String actionId) {
        return this.queuedActions.getOrDefault(actionId, new ConcurrentLinkedQueue<>()).size();
    }

    public int getGlobalQueuedCount() {
        return this.globalQueue.size();
    }

    public enum ActionInstancingBehaviour {
        CANCEL_PREVIOUS,
        RUN_IN_PARALLEL,
        QUEUE,
        GLOBAL_QUEUE;

        public static @NotNull ActionExecutor.ActionInstancingBehaviour fromString(@NotNull String s, ConfigIssueHelper issueHelper) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                issueHelper.appendAtPath(WA2.apply(s));
                return CANCEL_PREVIOUS;
            }
        }
    }
}
