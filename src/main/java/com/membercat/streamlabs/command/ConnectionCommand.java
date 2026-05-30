package com.membercat.streamlabs.command;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.command.argument.StreamlabsAccountSelectorArgumentType;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.java_websocket.client.WebSocketClient;
import org.jetbrains.annotations.NotNull;


import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.membercat.streamlabs.socket.StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static net.kyori.adventure.text.Component.*;


@ReflectUtil.Ignore
public class ConnectionCommand extends SubCommand {
    private static final String TK_BASE = "streamlabs.commands.connection.";
    private final String id, successTranslation, alreadyOnStateTranslation;
    private final Predicate<StreamlabsSocketClient> stateCheck;
    private final Consumer<StreamlabsSocketClient> action;

    private ConnectionCommand(
            @NotNull StreamlabsIntegration pluginInstance, @NotNull String id,
            @NotNull String successTranslation, @NotNull String alreadyOnStateTranslation,
            @NotNull Predicate<StreamlabsSocketClient> stateCheck, @NotNull Consumer<StreamlabsSocketClient> action
    ) {
        super(pluginInstance);
        this.id = id;
        this.successTranslation = successTranslation;
        this.alreadyOnStateTranslation = alreadyOnStateTranslation;
        this.stateCheck = stateCheck;
        this.action = action;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal(this.id)
                .then(argument("account", StreamlabsAccountSelectorArgumentType.accountSelector(getPlugin()))
                        .executes(source -> exceptionHandler(source, sender -> {
                            //noinspection unchecked
                            Collection<PluginConfig.StreamlabsAccount> accounts = source.getArgument("account", Collection.class);
                            Map<PluginConfig.StreamlabsAccount, StreamlabsSocketClient> cMap = accounts.stream()
                                    .collect(Collectors.toUnmodifiableMap(k -> k, a -> Objects.requireNonNull(getPlugin().getSocketClient(a))));
                            if (cMap.values().stream().allMatch(stateCheck)) {
                                Translations.sendPrefixedResponse(TK_BASE + alreadyOnStateTranslation, ColorScheme.DISABLE, sender, getAccountDisplay(accounts, true));
                                return;
                            }
                            cMap = cMap.entrySet().stream().filter(c -> !stateCheck.test(c.getValue()))
                                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
                            Translations.sendPrefixedResponse(TK_BASE + successTranslation, ColorScheme.DONE, sender, getAccountDisplay(cMap.keySet(), false));
                            cMap.values().forEach(action);
                        }))).build();
    }

    private @NotNull Component getAccountDisplay(@NotNull Collection<PluginConfig.StreamlabsAccount> accounts, boolean withVerb) throws IllegalArgumentException {
        Component display = accounts.size() > 1 ? translatable(TK_BASE + "account.count", text(String.valueOf(accounts.size())))
                : text("\"%s\"".formatted(accounts.stream().findAny().map(a -> a.id).orElseThrow(IllegalArgumentException::new)));
        display = display.color(ColorScheme.SUCCESS);
        return withVerb ? empty().append(display).appendSpace().append(translatable(TK_BASE + "account." + (accounts.size() > 1 ? "are" : "is"))) : display;
    }

    public static @NotNull ConnectionCommand connect(@NotNull StreamlabsIntegration plugin) {
        return new ConnectionCommand(plugin, "connect", "connecting", "already_connected", WebSocketClient::isOpen, StreamlabsSocketClient::reconnectAsync);
    }

    public static @NotNull ConnectionCommand disconnect(@NotNull StreamlabsIntegration plugin) {
        return new ConnectionCommand(plugin, "disconnect", "disconnecting", "not_connected", WebSocketClient::isClosed, PLUGIN_CLOSED_CONNECTION::close);
    }
}
