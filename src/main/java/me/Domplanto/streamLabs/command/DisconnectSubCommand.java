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
public class DisconnectSubCommand extends SubCommand {
    public DisconnectSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        StreamlabsSocketClient socketClient = getPlugin().getSocketClient();
        if (socketClient.isOpen()) {
            socketClient.close();
            sender.sendMessage(ChatColor.RED + "Disconnected from Streamlabs!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Not connected to Streamlabs!");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
