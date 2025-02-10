package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static net.kyori.adventure.text.Component.text;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length != 3) {
            Translations.sendPrefixedResponse("streamlabs.commands.player.error_no_name", ColorScheme.INVALID, sender);
            return true;
        }

        Set<String> players = getPlugin().pluginConfig().getAffectedPlayers();
        if (args[1].equals("add")) {
            if (players.contains(args[2])) {
                Translations.sendPrefixedResponse("streamlabs.commands.player.error_already_in_list", ColorScheme.DISABLE, sender, text(args[2]));
                return true;
            }

            players.add(args[2]);
            Translations.sendPrefixedResponse("streamlabs.commands.player.player_added", ColorScheme.SUCCESS, sender, text(args[2]));
        } else if (args[1].equals("remove")) {
            if (!players.contains(args[2])) {
                Translations.sendPrefixedResponse("streamlabs.commands.player.error_not_in_list", ColorScheme.DISABLE, sender, text(args[2]));
                return true;
            }

            players.removeIf(player -> player.equals(args[2]));
            Translations.sendPrefixedResponse("streamlabs.commands.player.player_removed", ColorScheme.SUCCESS, sender, text(args[2]));
        } else
            Translations.sendPrefixedResponse("streamlabs.command.error.invalid_sub_command", ColorScheme.INVALID, sender, text(args[1]));

        getPlugin().pluginConfig().setAffectedPlayers(getPlugin(), players);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
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
