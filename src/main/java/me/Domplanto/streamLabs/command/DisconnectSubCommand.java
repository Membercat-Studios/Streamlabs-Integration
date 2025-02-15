package me.Domplanto.streamLabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;

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
