package me.Domplanto.streamLabs.message;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.RewardsConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class Message {
    private final String content;
    private final MessageType type;

    private Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public void send(Player player) {
        this.type.sendMessage(player, this.content);
    }

    public Message replacePlaceholders(StreamlabsEvent event, RewardsConfig config, JsonObject baseObject) {
        return new Message(this.type, ActionPlaceholder.replacePlaceholders(this.content, event, config, baseObject));
    }

    public static List<Message> parseAll(List<String> messageStrings) {
        return messageStrings.stream()
                .map(messageString -> {
                    MessageType type = MessageType.MESSAGE;
                    if (messageString.startsWith("[") && messageString.contains("]")) {
                        String content = messageString.substring(1, messageString.indexOf(']'));
                        type = MessageType.valueOf(content.toUpperCase());
                        messageString = messageString.substring(messageString.indexOf(']') + 1);
                    }

                    return new Message(type, messageString);
                })
                .toList();
    }
}
