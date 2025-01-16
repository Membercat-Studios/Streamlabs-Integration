package me.Domplanto.streamLabs.message;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
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

    public Message replacePlaceholders(ActionExecutionContext ctx) {
        return new Message(this.type, ActionPlaceholder.replacePlaceholders(this.content, ctx));
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
