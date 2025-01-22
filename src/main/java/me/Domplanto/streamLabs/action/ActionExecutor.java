package me.Domplanto.streamLabs.action;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.socket.serializer.SocketSerializerException;
import me.Domplanto.streamLabs.statistics.EventHistory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public class ActionExecutor {
    private final PluginConfig pluginConfig;
    private final Set<? extends StreamlabsEvent> eventSet;
    private final EventHistory eventHistory;
    private final JavaPlugin plugin;

    public ActionExecutor(PluginConfig pluginConfig, Set<? extends StreamlabsEvent> eventSet, JavaPlugin plugin) {
        this.pluginConfig = pluginConfig;
        this.eventSet = eventSet;
        this.plugin = plugin;
        this.eventHistory = new EventHistory();
    }

    public void parseAndExecute(JsonElement data) throws SocketSerializerException {
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
    }

    private void executeAction(ActionExecutionContext ctx) {
        List<String> affectedPlayers = plugin.getConfig().getStringList("affected_players");
        ctx.action().messages
                .stream().map(message -> message.replacePlaceholders(ctx))
                .forEach(message -> affectedPlayers.stream()
                        .map(playerName -> plugin.getServer().getPlayerExact(playerName))
                        .forEach(message::send));

        for (String command : ctx.action().commands) {
            int executeAmount = 1;
            if (command.startsWith("[") && command.contains("]")) {
                String content = command.substring(1, command.indexOf(']'));
                content = ActionPlaceholder.replacePlaceholders(content, ctx);
                command = command.substring(command.indexOf(']') + 1);
                try {
                    executeAmount = new DoubleEvaluator().evaluate(content).intValue();
                } catch (Exception ignore) {
                }
            }

            command = ActionPlaceholder.replacePlaceholders(command, ctx);
            List<String> players = command.contains("{player}") ? affectedPlayers : List.of("");
            for (int i = 0; i < executeAmount; i++) {
                for (String player : players) {
                    String finalCommand = command.replace("{player}", player);
                    Bukkit.getScheduler().runTask(this.plugin, () ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                }
            }
        }
    }

    public EventHistory getEventHistory() {
        return eventHistory;
    }
}
