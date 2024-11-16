package me.Domplanto.streamLabs;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.command.SubCommand;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.exception.UnexpectedJsonFormatException;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class StreamLabs extends JavaPlugin {
    private static final Set<? extends StreamlabsEvent> STREAMLABS_EVENTS = StreamlabsEvent.findEventClasses();
    private final Set<? extends SubCommand> SUB_COMMANDS = SubCommand.findSubCommandClasses(this);
    private static boolean DEBUG_MODE = false;
    private StreamlabsSocketClient socketClient;
    private RewardsConfig rewardsConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        String socketToken = config.getString("streamlabs.socket_token", "");
        DEBUG_MODE = config.getBoolean("debug_mode", false);
        this.rewardsConfig = new RewardsConfig(config);
        if (socketToken.isEmpty()) {
            getLogger().warning("Streamlabs socket token not configured!");
            getLogger().warning("Please set your token in config.yml");
        }

        this.socketClient = new StreamlabsSocketClient(socketToken, getLogger(), this::handleStreamlabsEvent)
                .setConnectionOpenListener(this::onConnectionOpen)
                .setConnectionCloseListener(this::onConnectionClosed)
                .setInvalidTokenListener(this::onInvalidSocketToken);
        this.socketClient.connect();
    }

    private void onConnectionOpen(ServerHandshake handshake) {
        if (this.showStatusMessages())
            Bukkit.broadcastMessage(ChatColor.GREEN + "Successfully connected to Streamlabs!");
    }

    private void onConnectionClosed(String message) {
        if (this.showStatusMessages())
            Bukkit.broadcastMessage(ChatColor.RED + "Connection to Streamlabs lost!");
    }

    private void onInvalidSocketToken() {
        if (this.showStatusMessages())
            Bukkit.broadcastMessage(ChatColor.YELLOW + "The socket token specified is invalid!");
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.isOpen()) {
            socketClient.close();
        }
    }

    private void handleStreamlabsEvent(JsonElement data) throws UnexpectedJsonFormatException {
        JsonObject object = data.getAsJsonArray().get(1).getAsJsonObject();
        String type = object.get("type").getAsString();
        if (StreamLabs.isDebugMode() && (!type.equals("alertPlaying") && !type.equals("streamlabels") && !type.equals("streamlabels.underlying")))
            getLogger().info(String.format("Streamlabs message: %s", data));

        String platform = object.has("for") ? object.get("for").getAsString() : "streamlabs";
        StreamlabsEvent event = STREAMLABS_EVENTS.stream()
                .filter(e -> e.getApiName().equals(type) && e.getPlatform().compare(platform))
                .findFirst().orElse(null);
        if (event == null) return;


        JsonObject baseObject = event.getBaseObject(object);
        List<RewardsConfig.Action> actions = rewardsConfig.getActionsForEvent(event.getId());
        for (RewardsConfig.Action action : actions) {
            if (!action.isEnabled()) continue;

            if (event.checkConditions(action, baseObject)) {
                executeAction(action, event, baseObject);
            }
        }
    }

    private void executeAction(RewardsConfig.Action action, StreamlabsEvent event, JsonObject baseObject) {
        List<String> affectedPlayers = getConfig().getStringList("affected_players");
        action.getMessages()
                .stream().map(message -> message.replacePlaceholders(event, baseObject))
                .forEach(message -> affectedPlayers.stream()
                        .map(playerName -> getServer().getPlayerExact(playerName))
                        .forEach(message::send));

        for (String command : action.getCommands()) {
            int executeAmount = 1;
            if (command.startsWith("[") && command.contains("]")) {
                String content = command.substring(1, command.indexOf(']'));
                content = ActionPlaceholder.replacePlaceholders(content, event, baseObject);
                command = command.substring(command.indexOf(']') + 1);
                try {
                    executeAmount = new DoubleEvaluator().evaluate(content).intValue();
                } catch (Exception ignore) {
                }
            }

            command = ActionPlaceholder.replacePlaceholders(command, event, baseObject);
            List<String> players = command.contains("{player}") ? affectedPlayers : List.of("");
            for (int i = 0; i < executeAmount; i++) {
                for (String player : players) {
                    String finalCommand = command.replace("{player}", player);
                    Bukkit.getScheduler().runTask(this, () ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                }
            }
        }
    }

    public StreamlabsSocketClient getSocketClient() {
        return socketClient;
    }

    public void setRewardsConfig(RewardsConfig rewardsConfig) {
        this.rewardsConfig = rewardsConfig;
    }

    private boolean showStatusMessages() {
        return getConfig().getBoolean("show_status_messages", true);
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("streamlabs")) {
            if (!sender.hasPermission("streamlabs.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Please add a sub-command!");
                return true;
            }

            return SUB_COMMANDS.stream()
                    .filter(c -> c.getName().equals(args[0]))
                    .findFirst()
                    .map(c -> c.onCommand(sender, command, label, args))
                    .orElseGet(() -> {
                        sender.sendMessage(ChatColor.RED + "Unknown sub-command!");
                        return true;
                    });
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            return SUB_COMMANDS.stream()
                    .map(SubCommand::getName)
                    .toList();
        } else {
            return SUB_COMMANDS.stream()
                    .filter(c -> c.getName().equals(args[0]))
                    .findFirst()
                    .map(c -> c.onTabComplete(sender, command, alias, args)).orElse(List.of());
        }
    }
}
