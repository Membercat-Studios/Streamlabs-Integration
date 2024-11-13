package me.Domplanto.streamLabs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEventType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public class StreamLabs extends JavaPlugin {
    private WebSocketClient websocket;
    private Timer timer;
    private String socketToken;
    private boolean isConnected = false;
    private RewardsConfig rewardsConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        socketToken = config.getString("streamlabs.socket_token", "");
        rewardsConfig = new RewardsConfig(config);

        if (socketToken.isEmpty()) {
            getLogger().warning("Streamlabs socket token not configured!");
            getLogger().warning("Please set your token in config.yml");
        } else {
            connectToStreamlabs();
        }
    }

    @Override
    public void onDisable() {
        if (websocket != null && isConnected) {
            websocket.close();
        }
    }

    private void connectToStreamlabs() {
        try {
            String websocketUrl = String.format("wss://sockets.streamlabs.com/socket.io/?token=%s&transport=websocket", socketToken);
            websocket = new WebSocketClient(new URI(websocketUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected = true;
                    getLogger().info("Connected to Streamlabs!");
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Streamlabs connection established!");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        if (!message.startsWith("42")) return;

                        message = message.substring(2);
                        JsonObject jsonMessage = new Gson().fromJson(message, JsonObject.class);
                        handleStreamlabsEvent(jsonMessage);
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Error processing Streamlabs message", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    getLogger().warning("Disconnected from Streamlabs: " + reason);
                    Bukkit.broadcastMessage(ChatColor.RED + "Streamlabs connection lost!");
                }

                @Override
                public void onError(Exception ex) {
                    getLogger().log(Level.SEVERE, "Streamlabs websocket error", ex);
                }
            };

            websocket.connect();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (websocket != null && websocket.isOpen())
                        websocket.send("2");
                }
            }, 15000, 15000);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error connecting to Streamlabs", e);
        }
    }

    private void handleStreamlabsEvent(JsonObject jsonMessage) {
        String type = jsonMessage.get("type").getAsString();
        String platform = jsonMessage.get("platform").getAsString();
        JsonObject messageData = (JsonObject) jsonMessage.get("message");

        for (StreamlabsEventType eventType : StreamlabsEventType.values()) {
            if (eventType.getEventName().equals(type) &&
                    (eventType.getPlatform() == null || eventType.getPlatform().equals(platform))) {

                processEvent(eventType, messageData);
                break;
            }
        }
    }

    private void processEvent(StreamlabsEventType eventType, JsonObject data) {
        String username = data.get("name").getAsString();
        double amount = 0.0;
        String displayMessage = "";

        // Calculate amount based on event type
        switch (eventType) {
            case DONATION:
                amount = Double.parseDouble(data.get("amount").getAsString());
                String currency = data.get("currency").getAsString();
                displayMessage = ChatColor.GREEN + username + " donated " + amount + " " + currency + "!";
                break;

            case TWITCH_BITS:
                amount = ((Long) data.get("amount").getAsLong()).doubleValue() / 100.0;
                displayMessage = ChatColor.DARK_PURPLE + username + " cheered " + data.get("amount") + " bits!";
                break;

            case TWITCH_SUBSCRIPTION:
                String tier = data.get("sub_plan").getAsString();
                amount = switch (tier) {
                    case "2000" -> 10.0;
                    case "3000" -> 25.0;
                    default -> 5.0;
                };
                displayMessage = ChatColor.BLUE + username + " subscribed with Tier " + tier.charAt(0) + "!";
                break;

            case YOUTUBE_SUPERCHAT:
                amount = Double.parseDouble(data.get("amount").getAsString());
                displayMessage = ChatColor.RED + username + " sent a Superchat of " + amount + "!";
                break;

            case TWITCH_FOLLOW:
            case YOUTUBE_FOLLOW:
                displayMessage = ChatColor.AQUA + username + " followed!";
                break;

            case TWITCH_RAID:
                long viewers = data.get("viewers").getAsLong();
                displayMessage = ChatColor.GOLD + username + " raided with " + viewers + " viewers!";
                break;

            case TWITCH_HOST:
                viewers = data.get("viewers").getAsLong();
                displayMessage = ChatColor.YELLOW + username + " hosted with " + viewers + " viewers!";
                break;
        }

        // Broadcast the message if not empty
        if (!displayMessage.isEmpty()) {
            Bukkit.broadcastMessage(displayMessage);
        }

        // Process actions for this event
        String eventTypeName = eventType.name().toLowerCase();
        List<RewardsConfig.Action> actions = rewardsConfig.getActionsForEvent(eventTypeName);

        for (RewardsConfig.Action action : actions) {
            if (!action.isEnabled()) continue;
            if (amount >= action.getThreshold()) {
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("streamlabs")) {
            if (!sender.hasPermission("streamlabs.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 1) {
                switch (args[0].toLowerCase()) {
                    case "connect":
                        if (!isConnected) {
                            connectToStreamlabs();
                            sender.sendMessage(ChatColor.GREEN + "Connecting to Streamlabs...");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Already connected to Streamlabs!");
                        }
                        return true;

                    case "disconnect":
                        if (isConnected && websocket != null) {
                            timer.cancel();
                            websocket.close();
                            sender.sendMessage(ChatColor.RED + "Disconnected from Streamlabs!");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Not connected to Streamlabs!");
                        }
                        return true;

                    case "status":
                        sender.sendMessage(ChatColor.BLUE + "Streamlabs Status: " +
                                (isConnected ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected"));
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
