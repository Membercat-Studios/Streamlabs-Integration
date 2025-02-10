package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@SuppressWarnings("unused")
public class StatusSubCommand extends SubCommand {
    private static final Component CONNECTED = translatable()
            .key("streamlabs.status.connected")
            .color(ColorScheme.SUCCESS)
            .build();
    private static final Component DISCONNECTED = translatable()
            .key("streamlabs.status.disconnected")
            .color(ColorScheme.DISABLE)
            .build();

    public StatusSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        Component status = translatable()
                .key("streamlabs.commands.status.prefix")
                .color(ColorScheme.STREAMLABS)
                .append(text(" "))
                .append(getPlugin().getSocketClient().isOpen() ? CONNECTED : DISCONNECTED)
                .build();

        sender.sendMessage(Translations.withPrefix(status, true));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return List.of();
    }
}
