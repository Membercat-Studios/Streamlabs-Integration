package me.Domplanto.streamLabs.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.command.argument.OnlinePlayersArgumentType;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class PlayerSubCommand extends SubCommand {
    public PlayerSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return literal("player")
                .then(literal("add")
                        .then(argument("player", OnlinePlayersArgumentType.onlinePlayers(getPlugin().getServer(), Predicate.not(this::onList)))
                                .executes(ctx -> exceptionHandler(ctx, sender -> executeOnPlayers((player, players) -> players.add(player.getName()), ctx, "streamlabs.commands.player.player_added")))
                        ))
                .then(literal("remove")
                        .then(argument("player", OnlinePlayersArgumentType.onlinePlayers(getPlugin().getServer(), this::onList))
                                .executes(ctx -> exceptionHandler(ctx, sender -> executeOnPlayers((player, players) -> players.removeIf(p -> p.equals(player.getName())), ctx, "streamlabs.commands.player.player_removed")))
                        ))
                .build();
    }

    private void executeOnPlayers(BiConsumer<Player, Set<String>> action, CommandContext<CommandSourceStack> ctx, String translationKey) {
        Set<String> players = getPlugin().pluginConfig().getAffectedPlayers();
        for (Player player : OnlinePlayersArgumentType.getPlayers(ctx, "player")) {
            Translations.sendPrefixedResponse(translationKey, ColorScheme.SUCCESS, ctx.getSource().getSender(), player.displayName());
            action.accept(player, players);
        }

        getPlugin().pluginConfig().setAffectedPlayers(getPlugin(), players);
    }

    private boolean onList(Player player) {
        return getPlugin().pluginConfig().getAffectedPlayers().contains(player.getName());
    }
}
