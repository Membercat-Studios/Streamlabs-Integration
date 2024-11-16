package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class ConnectSubCommand extends SubCommand {
    public ConnectSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "connect";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        StreamlabsSocketClient socketClient = getPlugin().getSocketClient();
        if (!socketClient.isOpen()) {
            socketClient.reconnectAsync();
            sender.sendMessage(ChatColor.GREEN + "Connecting to Streamlabs...");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Already connected to Streamlabs!");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
