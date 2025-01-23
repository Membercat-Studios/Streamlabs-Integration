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

public class ActionExecutor {
    private final PluginConfig pluginConfig;
    private final Set<? extends StreamlabsEvent> eventSet;
    private final EventHistory eventHistory;
    @Nullable
    private DonationGoal activeGoal;
    private final JavaPlugin plugin;

    public ActionExecutor(PluginConfig pluginConfig, Set<? extends StreamlabsEvent> eventSet, JavaPlugin plugin) {
        this.pluginConfig = pluginConfig;
        this.eventSet = eventSet;
        this.plugin = plugin;
        this.eventHistory = new EventHistory();
    }

    public void parseAndExecute(JsonElement data) throws SocketSerializerException {
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
        StreamlabsEvent event = eventSet.stream()
                .filter(e -> e.getApiName().equals(type) && e.getPlatform().compare(platform))
                .findFirst().orElse(null);
        if (event == null) return;

        JsonObject baseObject = event.getBaseObject(object);
        this.checkAndExecute(event, baseObject);
    }

    public void checkAndExecute(StreamlabsEvent event, JsonObject baseObject) {
        this.eventHistory.store(event, this.pluginConfig, baseObject);
        List<PluginConfig.Action> actions = pluginConfig.getActionsForEvent(event.getId());
        for (PluginConfig.Action action : actions) {
            if (!action.enabled) continue;

            ActionExecutionContext context = new ActionExecutionContext(event, this.pluginConfig, action, baseObject);
            if (event.checkConditions(context))
                executeAction(context);
        }

        this.updateGoal(new ActionExecutionContext(event, this.pluginConfig, null, baseObject));
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
        if (this.activeGoal == null) return;
        DonationGoal goal = pluginConfig.getGoal(this.activeGoal.id);
        if (goal == null) {
            this.removeGoal();
            return;
        }

        if (!this.activeGoal.add(ctx)) return;
        this.executeAction(new ActionExecutionContext(null, this.pluginConfig, goal, null));
        this.stopGoal();
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
