package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class PlayerSubCommand extends SubCommand {
    public PlayerSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "player";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Please specify a player name");
            return true;
        }

        FileConfiguration config = getPlugin().getConfig();
        List<String> players = config.getStringList("affected_players");
        if (args[1].equals("add")) {
            if (players.contains(args[2])) {
                sender.sendMessage(ChatColor.RED + String.format("%s is already in the affected player list", args[2]));
                return true;
            }

            players.add(args[2]);
            sender.sendMessage(ChatColor.GREEN + String.format("%s added to affected players", args[2]));
        } else if (args[1].equals("remove")) {
            if (!players.contains(args[2])) {
                sender.sendMessage(ChatColor.RED + String.format("%s is not in the affected player list", args[2]));
                return true;
            }

            players.removeIf(player -> player.equals(args[2]));
            sender.sendMessage(ChatColor.GREEN + String.format("%s removed from affected players", args[2]));
        } else {
            sender.sendMessage(ChatColor.RED + String.format("Unknown sub-command \"%s\"", args[1]));
        }

        config.set("affected_players", players);
        getPlugin().saveConfig();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> affectedPlayers = getPlugin().getConfig().getStringList("affected_players");
        if (args.length == 2)
            return List.of("add", "remove");
        if (args.length == 3 && args[1].equals("remove"))
            return affectedPlayers;
        if (args.length == 3 && args[1].equals("add")) {
            return getPlugin().getServer()
                    .getOnlinePlayers()
                    .stream().map(Player::getName)
                    .filter(name -> !affectedPlayers.contains(name))
                    .toList();
        }

        return null;
    }
}
