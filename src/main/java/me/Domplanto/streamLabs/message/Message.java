package me.Domplanto.streamLabs.message;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import org.bukkit.entity.Player;

import java.util.List;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "message")
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

    public static List<Message> parseAll(List<String> messageStrings, ConfigIssueHelper issueHelper) {
        return messageStrings.stream()
                .map(messageString -> {
                    issueHelper.push(Message.class, String.valueOf(messageStrings.indexOf(messageString)));
                    MessageType type = MessageType.MESSAGE;
                    if (messageString.startsWith("[") && messageString.contains("]")) {
                        String content = messageString.substring(1, messageString.indexOf(']'));
                        try {
                            type = MessageType.valueOf(content.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            issueHelper.appendAtPath(WM0.apply(content.toUpperCase(), type.name()));
                        }
                        messageString = messageString.substring(messageString.indexOf(']') + 1);
                    }

                    issueHelper.pop();
                    return new Message(type, messageString);
                })
                .toList();
    }
}
