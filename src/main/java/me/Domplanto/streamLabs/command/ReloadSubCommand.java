package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.RewardsConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class ReloadSubCommand extends SubCommand {
    public ReloadSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        getPlugin().reloadConfig();
        getPlugin().setRewardsConfig(new RewardsConfig(getPlugin().getConfig()));
        getPlugin().getSocketClient().updateToken(getPlugin().getConfig().getString("streamlabs.socket_token", ""));
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
        getPlugin().getSocketClient().reconnectAsync();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
