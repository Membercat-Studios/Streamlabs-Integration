package me.Domplanto.streamLabs.action;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.exception.UnexpectedJsonFormatException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public class ActionExecutor {
    private final RewardsConfig rewardsConfig;
    private final Set<? extends StreamlabsEvent> eventSet;
    private final JavaPlugin plugin;

    public ActionExecutor(RewardsConfig rewardsConfig, Set<? extends StreamlabsEvent> eventSet, JavaPlugin plugin) {
        this.rewardsConfig = rewardsConfig;
        this.eventSet = eventSet;
        this.plugin = plugin;
    }

    public void parseAndExecute(JsonElement data) throws UnexpectedJsonFormatException {
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
        List<RewardsConfig.Action> actions = rewardsConfig.getActionsForEvent(event.getId());
        for (RewardsConfig.Action action : actions) {
            if (!action.isEnabled()) continue;

            if (event.checkConditions(action, baseObject)) {
                executeAction(action, event, baseObject);
            }
        }
    }

    private void executeAction(RewardsConfig.Action action, StreamlabsEvent event, JsonObject baseObject) {
        List<String> affectedPlayers = plugin.getConfig().getStringList("affected_players");
        action.getMessages()
                .stream().map(message -> message.replacePlaceholders(event, rewardsConfig, baseObject))
                .forEach(message -> affectedPlayers.stream()
                        .map(playerName -> plugin.getServer().getPlayerExact(playerName))
                        .forEach(message::send));

        for (String command : action.getCommands()) {
            int executeAmount = 1;
            if (command.startsWith("[") && command.contains("]")) {
                String content = command.substring(1, command.indexOf(']'));
                content = ActionPlaceholder.replacePlaceholders(content, event, rewardsConfig, baseObject);
                command = command.substring(command.indexOf(']') + 1);
                try {
                    executeAmount = new DoubleEvaluator().evaluate(content).intValue();
                } catch (Exception ignore) {
                }
            }

            command = ActionPlaceholder.replacePlaceholders(command, event, rewardsConfig, baseObject);
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
}
