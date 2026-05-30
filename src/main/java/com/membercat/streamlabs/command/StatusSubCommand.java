package com.membercat.streamlabs.command;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.*;

@SuppressWarnings({"unused"})
public class StatusSubCommand extends SubCommand {
    private static final Component CONNECTED = translatable()
            .key("streamlabs.status.connected")
            .color(ColorScheme.SUCCESS)
            .build();
    private static final Component DISCONNECTED = translatable()
            .key("streamlabs.status.disconnected")
            .color(ColorScheme.DISABLE)
            .build();

    public StatusSubCommand(StreamlabsIntegration pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("status")
                .executes(ctx -> exceptionHandler(ctx, sender -> {
                    List<PluginConfig.StreamlabsAccount> accounts = new ArrayList<>(getPlugin().pluginConfig().getAccounts());
                    List<? extends Component> states = accounts.stream().map(a -> {
                        StreamlabsSocketClient client = getPlugin().getSocketClient(a);
                        if (client == null) return null;
                        Component state = empty().append(Collections.nCopies(10 , space()));
                        state = state.append(translatable("streamlabs.commands.status.format", text(a.id, ColorScheme.DONE), client.isOpen() ? CONNECTED : DISCONNECTED));
                        return !accounts.getLast().equals(a) ? state.appendNewline() : state;
                    }).filter(Objects::nonNull).toList();
                    Component status = translatable()
                            .key("streamlabs.commands.status.title")
                            .color(ColorScheme.STREAMLABS)
                            .append(newline())
                            .append(states)
                            .build();

                    sender.sendMessage(Translations.withPrefix(status, true));
                })).build();
    }
}
