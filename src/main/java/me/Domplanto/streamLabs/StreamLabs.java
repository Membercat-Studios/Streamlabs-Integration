package me.Domplanto.streamLabs;

import com.google.gson.JsonElement;
import me.Domplanto.streamLabs.action.ActionExecutor;
import me.Domplanto.streamLabs.command.SubCommand;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.font.DefaultFontInfo;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getLogger());
        try {
            this.pluginConfig.load(getConfig());
        } catch (ConfigLoadedWithIssuesException e) {
            this.printIssues(e.getIssues(), Bukkit.getConsoleSender());
        }

        DEBUG_MODE = pluginConfig.getOptions().debugMode;
        this.socketClient = new StreamlabsSocketClient(pluginConfig.getOptions().socketToken, getLogger(), this::onStreamlabsEvent)
                .setConnectionOpenListener(this::onConnectionOpen)
                .setConnectionCloseListener(this::onConnectionClosed)
                .setInvalidTokenListener(this::onInvalidSocketToken);
        // The StreamlabsSocketClient will not connect at all if a connection in onEnable is not attempted,
        // this is why we need to add it here, this issue should definitely be investigated in the future!
        new Thread(() -> {
            try {
                this.socketClient.connectBlocking();
            } catch (InterruptedException ignore) {
            }
            if (!pluginConfig.getOptions().autoConnect && this.socketClient.isOpen()) {
                this.socketClient.close();
                getLogger().info("Not connecting to Streamlabs at startup because auto_connect is disabled in the config!");
            }
        }, "Socket startup Thread").start();
    }

    public void printIssues(List<ConfigIssue> issues, CommandSender sender) {
        List<String> issueStr = issues.stream()
                .map(ConfigIssue::getMessage)
                .toList();
        getLogger().warning("Loaded config with %d issues".formatted(issueStr.size()));
        String bottom = "-".repeat(53);
        String top = DefaultFontInfo.centerMessage(" Configuration Issue List ", '-');
        top += "-".repeat((bottom.length() + 4) - top.length()) + "\n";
        String output = ChatColor.AQUA + top + String.join("\n", issueStr) + "\n" + ChatColor.AQUA + bottom;
        sender.sendMessage(output);
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
        ActionExecutor executor = new ActionExecutor(this.pluginConfig, STREAMLABS_EVENTS, this);
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

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    private boolean showStatusMessages() {
        return this.pluginConfig.getOptions().showStatusMessages;
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
