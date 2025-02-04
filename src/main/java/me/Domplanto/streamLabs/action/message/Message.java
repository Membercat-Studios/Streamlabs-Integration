package me.Domplanto.streamLabs.action.message;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
        Component message = MiniMessage.miniMessage().deserialize(this.content);
        this.type.sendMessage(player, message);
    }

    public Message replacePlaceholders(ActionExecutionContext ctx) {
        return new Message(this.type, ActionPlaceholder.replacePlaceholders(this.content, ctx));
    }

    public static List<Message> parseAll(List<String> messageStrings, ConfigIssueHelper issueHelper) {
        return messageStrings.stream()
                .map(messageString -> {
                    issueHelper.push(Message.class, String.valueOf(messageStrings.indexOf(messageString)));
                    MessageType type = MessageType.MESSAGE;
                    BracketResolver resolver = new BracketResolver(messageString).resolve(issueHelper);
                    String typeStr = resolver.getBracketContents().map(String::toUpperCase).orElse("MESSAGE");
                    try {
                        type = MessageType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        issueHelper.appendAtPath(WM0.apply(typeStr, type.name()));
                    }

                    issueHelper.pop();
                    return new Message(type, resolver.getContent());
                })
                .toList();
    }
}
