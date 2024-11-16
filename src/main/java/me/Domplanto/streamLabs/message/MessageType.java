package me.Domplanto.streamLabs.message;

import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public enum MessageType {
    MESSAGE(Player::sendMessage),
    @SuppressWarnings("deprecation")
    TITLE((player, message) -> player.sendTitle(message, null)),
    @SuppressWarnings("deprecation")
    SUBTITLE((player, message) -> player.sendTitle(null, message));

    private final BiConsumer<Player, String> action;

    MessageType(BiConsumer<Player, String> action) {
        this.action = action;
    }

    public void sendMessage(Player player, String message) {
        this.action.accept(player, message);
    }
}
