package com.membercat.streamlabs.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class OnlinePlayersArgumentType implements CustomArgumentType<List<Player>, String> {
    private final Server server;
    private final Predicate<Player> allow;

    private OnlinePlayersArgumentType(Server server, Predicate<Player> allow) {
        this.server = server;
        this.allow = allow;
    }

    public static OnlinePlayersArgumentType onlinePlayers(Server server, Predicate<Player> allow) {
        return new OnlinePlayersArgumentType(server, allow);
    }

    public static List<Player> getPlayers(CommandContext<CommandSourceStack> context, String name) {
        //noinspection unchecked
        return (List<Player>) context.getArgument(name, List.class);
    }

    @Override
    public @NotNull List<Player> parse(@NotNull StringReader stringReader) throws CommandSyntaxException {
        return this.server.selectEntities(Bukkit.getConsoleSender(), stringReader.readString())
                .stream().filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .filter(this.allow).toList();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        this.server.getOnlinePlayers()
                .stream().filter(this.allow)
                .map(Player::getName).forEach(builder::suggest);
        return builder.buildFuture();
    }
}
