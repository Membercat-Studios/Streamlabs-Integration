package com.membercat.streamlabs.command;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings({"unused"})
public class ReloadSubCommand extends SubCommand {
    public static String SHOW_IN_CONSOLE = "/streamlabs reload _console";

    public ReloadSubCommand(StreamlabsIntegration pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return literal("reload")
                .then(argument("option", new OptionsArgumentType())
                        .executes(ctx -> exceptionHandler(ctx, sender -> {
                            Option option = ctx.getArgument("option", Option.class);
                            this.runReload(option, sender);
                        }))
                )
                .executes(ctx -> exceptionHandler(ctx, sender -> this.runReload(null, sender)))
                .build();
    }

    private void runReload(Option option, CommandSender sender) {
        if (option != Option._CONSOLE)
            Translations.sendPrefixedResponse("streamlabs.commands.config.reload", ColorScheme.DONE, sender);
        if (option != Option.NORECONNECT) getPlugin().dbManager().close();
        try {
            getPlugin().reloadPluginConfig();
        } catch (ConfigLoadedWithIssuesException e) {
            getPlugin().printIssues(e.getIssues(), option != Option._CONSOLE ? sender : null);
        }
        if (option == Option.NORECONNECT) return;
        getPlugin().recreateDatabaseManager();
        getPlugin().dbManager().init();
        getPlugin().synchronizeSocketClients();
        Map<PluginConfig.StreamlabsAccount, StreamlabsSocketClient> cMap = getPlugin().pluginConfig().getAccounts()
                .stream().collect(Collectors.toUnmodifiableMap(k -> k, a -> Objects.requireNonNull(getPlugin().getSocketClient(a))));
        cMap.forEach((a, c) -> c.updateToken(a.socketToken));
        cMap.entrySet().stream()
                .filter(e -> e.getValue().isOpen() || (e.getKey().autoConnect && !e.getValue().isOpen()))
                .map(Map.Entry::getValue).forEach(client -> {
                    StreamlabsSocketClient.DisconnectReason.PLUGIN_RECONNECTING.close(client);
                    client.reconnectAsync();
                });
    }

    private static class OptionsArgumentType implements CustomArgumentType<Option, String> {
        @Override
        public @NotNull Option parse(StringReader reader) throws CommandSyntaxException {
            try {
                return Option.valueOf(reader.readString().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Option._NONE;
            }
        }

        @Override
        public @NotNull ArgumentType<String> getNativeType() {
            return StringArgumentType.string();
        }

        @Override
        public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
            Arrays.stream(Option.values())
                    .map(Objects::toString).map(String::toLowerCase)
                    .filter(str -> !str.startsWith("_"))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        }
    }

    private enum Option {
        NORECONNECT,
        _CONSOLE,
        _NONE
    }
}
