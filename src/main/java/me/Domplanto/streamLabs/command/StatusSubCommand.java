package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class StatusSubCommand extends SubCommand {
    public StatusSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        sender.sendMessage(ChatColor.BLUE + "Streamlabs Status: " +
                (getPlugin().getSocketClient().isOpen() ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected"));
        
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
