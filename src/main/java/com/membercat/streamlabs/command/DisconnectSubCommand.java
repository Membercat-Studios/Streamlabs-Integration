package com.membercat.streamlabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class DisconnectSubCommand extends SubCommand {
    public DisconnectSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("disconnect")
                .executes(source -> exceptionHandler(source, sender -> {
                    StreamlabsSocketClient socketClient = getPlugin().getSocketClient();
                    if (socketClient.isOpen()) {
                        StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(socketClient);
                        Translations.sendPrefixedResponse("streamlabs.commands.connection.disconnecting", ColorScheme.DONE, sender);
                    } else
                        Translations.sendPrefixedResponse("streamlabs.commands.connection.not_connected", ColorScheme.DISABLE, sender);
                })).build();
    }
}
