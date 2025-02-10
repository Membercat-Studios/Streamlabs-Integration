package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class DisconnectSubCommand extends SubCommand {
    public DisconnectSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        StreamlabsSocketClient socketClient = getPlugin().getSocketClient();
        if (socketClient.isOpen()) {
            StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(socketClient);
            Translations.sendPrefixedResponse("streamlabs.commands.connection.disconnecting", ColorScheme.DONE, sender);
        } else {
            Translations.sendPrefixedResponse("streamlabs.commands.connection.not_connected", ColorScheme.DISABLE, sender);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return List.of();
    }
}
