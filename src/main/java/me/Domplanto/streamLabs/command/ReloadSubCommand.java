package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.socket.StreamlabsSocketClient;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class ReloadSubCommand extends SubCommand {
    public ReloadSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        Translations.sendPrefixedResponse("streamlabs.commands.config.reload", ColorScheme.DONE, sender);
        getPlugin().reloadConfig();
        try {
            getPlugin().pluginConfig().load(getPlugin().getConfig());
        } catch (ConfigLoadedWithIssuesException e) {
            getPlugin().printIssues(e.getIssues(), sender);
        }
        getPlugin().getSocketClient().updateToken(getPlugin().pluginConfig().getOptions().socketToken);
        if (strings.length > 1 && strings[1].equals("noreconnect")) return true;
        if (getPlugin().getSocketClient().isOpen() || (getPlugin().pluginConfig().getOptions().autoConnect && !getPlugin().getSocketClient().isOpen())) {
            StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(getPlugin().getSocketClient());
            getPlugin().getSocketClient().reconnectAsync();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return strings.length > 1 ? List.of("noreconnect") : List.of();
    }
}
