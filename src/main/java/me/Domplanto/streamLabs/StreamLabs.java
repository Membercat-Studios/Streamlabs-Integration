package me.Domplanto.streamLabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.BasicDonationEvent;
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

import java.util.List;
import java.util.Set;

public class StreamLabs extends JavaPlugin {
    private static final Set<? extends StreamlabsEvent> STREAMLABS_EVENTS = StreamlabsEvent.findEventClasses();
    private StreamlabsSocketClient socketClient;
    private RewardsConfig rewardsConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        String socketToken = config.getString("streamlabs.socket_token", "");
        this.rewardsConfig = new RewardsConfig(config);
        if (socketToken.isEmpty()) {
            getLogger().warning("Streamlabs socket token not configured!");
            getLogger().warning("Please set your token in config.yml");
            return;
        }

        this.socketClient = new StreamlabsSocketClient(socketToken, getLogger(), this::handleStreamlabsEvent)
                .setConnectionOpenListener(this::onConnectionOpen)
                .setConnectionCloseListener(this::onConnectionClosed)
                .setInvalidTokenListener(this::onInvalidSocketToken);
        this.socketClient.connect();
    }

    private void onConnectionOpen(ServerHandshake handshake) {
        Bukkit.broadcastMessage(ChatColor.GREEN + "Successfully connected to Streamlabs!");
    }

    private void onConnectionClosed(String message) {
        Bukkit.broadcastMessage(ChatColor.RED + "Connection to Streamlabs lost!");
    }

    private void onInvalidSocketToken() {
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
        String platform = object.has("for") ? object.get("for").getAsString() : "streamlabs";

        StreamlabsEvent event = STREAMLABS_EVENTS.stream()
                .filter(e -> e.getId().equals(type) && e.getPlatform().compare(platform))
                .findFirst().orElse(null);
        if (event == null) return;


        JsonObject baseObject = event.getBaseObject(object);
        String message = event.getMessage(baseObject);
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(message);
        }

        List<RewardsConfig.Action> actions = rewardsConfig.getActionsForEvent(type.toLowerCase());
        for (RewardsConfig.Action action : actions) {
            if (!action.isEnabled()) continue;

            if (event.checkThreshold(baseObject, action.getThreshold())) {
                String username = event.getRelatedUser(baseObject);
                double amount = event instanceof BasicDonationEvent donationEvent
                        ? donationEvent.calculateAmount(baseObject) : 0;
                executeAction(action, username, amount);
            }
        }
    }

    private void executeAction(RewardsConfig.Action action, String username, double amount) {
        for (String command : action.getCommands()) {
            // Replace placeholders
            command = command.replace("{player}", username)
                    .replace("{amount}", String.valueOf((int) amount))
                    .replace("{amount_double}", String.format("%.2f", amount));

            // Execute command
            String finalCommand = command;
            Bukkit.getScheduler().runTask(this, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("streamlabs")) {
            if (!sender.hasPermission("streamlabs.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 1) {
                switch (args[0].toLowerCase()) {
                    case "connect":
                        if (!socketClient.isOpen()) {
                            this.socketClient.reconnect();
                            sender.sendMessage(ChatColor.GREEN + "Connecting to Streamlabs...");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Already connected to Streamlabs!");
                        }
                        return true;

                    case "disconnect":
                        if (socketClient.isOpen() && socketClient != null) {
                            socketClient.close();
                            sender.sendMessage(ChatColor.RED + "Disconnected from Streamlabs!");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Not connected to Streamlabs!");
                        }
                        return true;

                    case "status":
                        sender.sendMessage(ChatColor.BLUE + "Streamlabs Status: " +
                                (socketClient.isOpen() ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected"));
                        return true;

                    case "reload":
                        reloadConfig();
                        rewardsConfig = new RewardsConfig(getConfig());
                        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                        return true;
                }
            }
        }
        return false;
    }
}
