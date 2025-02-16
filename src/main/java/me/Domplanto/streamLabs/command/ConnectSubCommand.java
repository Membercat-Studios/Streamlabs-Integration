package me.Domplanto.streamLabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ConnectSubCommand extends SubCommand {
    public ConnectSubCommand(StreamLabs pluginInstance) {
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
