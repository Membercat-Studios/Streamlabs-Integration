package com.membercat.streamlabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ConnectSubCommand extends SubCommand {
    public ConnectSubCommand(StreamlabsIntegration pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("connect")
                .executes(source -> exceptionHandler(source, sender -> {
                    StreamlabsSocketClient socketClient = getPlugin().getSocketClient();
                    if (!socketClient.isOpen()) {
                        socketClient.reconnectAsync();
                        Translations.sendPrefixedResponse("streamlabs.commands.connection.connecting", ColorScheme.DONE, sender);
                    } else
                        Translations.sendPrefixedResponse("streamlabs.commands.connection.already_connected", ColorScheme.DISABLE, sender);
                })).build();
    }
}
