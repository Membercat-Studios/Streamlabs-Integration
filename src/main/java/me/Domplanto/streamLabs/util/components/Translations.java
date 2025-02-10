package me.Domplanto.streamLabs.util.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

public class Translations {
    private static final String STATUS_MESSAGE_PERMISSION = "streamlabs.status";

    public static Component withPrefix(Component component) {
        return text()
                .content("[").color(ColorScheme.COMMENT)
                .append(translatable().key("streamlabs.prefix").color(ColorScheme.STREAMLABS))
                .append(text().content("] ").color(ColorScheme.COMMENT))
                .append(component)
                .build();
    }

    public static void sendPrefixed(String translationKey, TextColor color, CommandSender sender) {
        sender.sendMessage(withPrefix(translatable(translationKey).color(color)));
    }

    public static void sendPrefixedToPlayers(String translationKey, TextColor color, Server server) {
        server.getOnlinePlayers()
                .stream().filter(player -> player.hasPermission(STATUS_MESSAGE_PERMISSION))
                .forEach(player -> sendPrefixed(translationKey, color, player));
    }
}
