package me.Domplanto.streamLabs.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.socket.serializer.SocketSerializerException;
import me.Domplanto.streamLabs.statistics.EventHistory;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ActionExecutor {
    private final PluginConfig pluginConfig;
    private final EventHistory eventHistory;
    @Nullable
    private DonationGoal activeGoal;
    private final JavaPlugin plugin;

    public ActionExecutor(PluginConfig pluginConfig, JavaPlugin plugin) {
        this.pluginConfig = pluginConfig;
        this.plugin = plugin;
        this.eventHistory = new EventHistory();
    }

    public void parseAndExecute(JsonElement data) throws SocketSerializerException {
        try {
            if (data.getAsJsonArray().size() < 2 || !data.getAsJsonArray().get(1).isJsonObject()) {
                if (StreamLabs.isDebugMode())
                    plugin.getLogger().info("Skipping streamlabs message with invalid formatting");
                return;
            }

            JsonObject object = data.getAsJsonArray().get(1).getAsJsonObject();
            String type = object.get("a").getAsString();
            if (StreamLabs.isDebugMode() && (!type.equals("alertPlaying") && !type.equals("streamlabels") && !type.equals("streamlabels.underlying")))
                plugin.getLogger().info(String.format("Streamlabs message: %s", data));

            String platform = object.has("for") ? object.get("for").getAsString() : "streamlabs";
            Set<? extends StreamlabsEvent> events = StreamLabs.getCachedEventObjects().stream()
                    .filter(e -> e.getApiName().equals(type) && e.getPlatform().compare(platform))
                    .collect(Collectors.toSet());

            for (StreamlabsEvent event : events) {
                JsonObject baseObject = event.getBaseObject(object);
                this.checkAndExecute(event, baseObject);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON content of a streamlabs message:", e);
        }
    }

    public boolean checkAndExecute(StreamlabsEvent event, JsonObject baseObject) {
        try {
            event.onExecute(this, baseObject);
            this.eventHistory.store(event, this.pluginConfig, baseObject);
            List<PluginConfig.Action> actions = pluginConfig.getActionsForEvent(event.getId());
            for (PluginConfig.Action action : actions) {
                if (!action.enabled) continue;

                ActionExecutionContext context = new ActionExecutionContext(event, this, this.pluginConfig, action, baseObject);
                if (event.checkConditions(context))
                    executeAction(context);
            }

            this.updateGoal(new ActionExecutionContext(event, this, this.pluginConfig, null, baseObject));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing actions for %s:".formatted(event.getId()), e);
            return false;
        }
    }

    private void executeAction(ActionExecutionContext ctx) {
        ctx.action().messages
                .stream().map(message -> message.replacePlaceholders(ctx))
                .forEach(message -> ctx.config().getAffectedPlayers().stream()
                        .map(playerName -> plugin.getServer().getPlayerExact(playerName))
                        .forEach(message::send));

        ctx.action().commands.forEach(cmd -> cmd.run(Bukkit.getConsoleSender(), this.plugin, ctx));
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
}
