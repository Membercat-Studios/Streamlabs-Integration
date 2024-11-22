package me.Domplanto.streamLabs;

import com.google.gson.JsonElement;
import me.Domplanto.streamLabs.action.ActionExecutor;
import me.Domplanto.streamLabs.command.SubCommand;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
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

        this.socketClient = new StreamlabsSocketClient(socketToken, getLogger(), this::onStreamlabsEvent)
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

    private void onStreamlabsEvent(JsonElement rawData) {
        ActionExecutor executor = new ActionExecutor(this.rewardsConfig, STREAMLABS_EVENTS, this);
        executor.parseAndExecute(rawData);
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.isOpen()) {
            socketClient.close();
        }
    }

    public StreamlabsSocketClient getSocketClient() {
        return socketClient;
    }

    public Set<? extends StreamlabsEvent> getCachedEventObjects() {
        return STREAMLABS_EVENTS;
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
