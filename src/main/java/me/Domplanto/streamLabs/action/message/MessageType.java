package me.Domplanto.streamLabs.action.message;

import me.Domplanto.streamLabs.util.font.DefaultFontInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public enum MessageType {
    MESSAGE(Player::sendMessage),
    MESSAGE_CENTERED((player, message) -> player.sendMessage(DefaultFontInfo.centerMessage(message))),
    TITLE((player, message) -> player.showTitle(Title.title(message, Component.empty()))),
    SUBTITLE((player, message) -> player.showTitle(Title.title(Component.empty(), message)));

    private final BiConsumer<Player, Component> action;

    MessageType(BiConsumer<Player, Component> action) {
        this.action = action;
    }

    public void sendMessage(Player player, Component message) {
        if (player != null)
            this.action.accept(player, message);
    }
}
