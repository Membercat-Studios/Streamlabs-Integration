package me.Domplanto.streamLabs.action.message;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

import static me.Domplanto.streamLabs.config.issue.Issues.WM0;
import static me.Domplanto.streamLabs.config.issue.Issues.WM1;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@ConfigPathSegment(id = "message")
public class Message {
    private final ConfigPathStack location;
    private final String content;
    private final MessageType type;

    private Message(MessageType type, String content, ConfigPathStack location) {
        this.type = type;
        this.content = content;
        this.location = location;
    }

    public void send(Player player) {
        Component message;
        try {
            message = MiniMessage.miniMessage().deserialize(this.content);
        } catch (ParsingException e) {
            // Kind of hacky way to determine if MiniMessage throws an exception related to
            // legacy formatting codes, there seems to be no other way of determining the exception reason.
            if (e.getMessage().contains("Legacy formatting codes"))
                message = LegacyComponentSerializer.legacySection().deserialize(this.content);
            else
                message = parsingError(e);

        } catch (Exception e) {
            message = parsingError(e);
        }

        this.type.sendMessage(player, message);
    }

    private Component parsingError(Exception e) {
        String location = this.location.toFormattedString();
        StreamLabs.LOGGER.log(Level.WARNING, "Failed to parse message at %s: ".formatted(location), e);
        return translatable()
                .key("streamlabs.error.message.parse_failed")
                .arguments(text(location))
                .style(Style.style(ColorScheme.INVALID, TextDecoration.ITALIC))
                .build();
    }

    public Message replacePlaceholders(ActionExecutionContext ctx) {
        String newContent = ActionPlaceholder.replacePlaceholders(this.content, ctx);
        //noinspection deprecation
        return new Message(this.type, ChatColor.translateAlternateColorCodes('&', newContent), this.location);
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

                    //noinspection deprecation
                    if (!ChatColor.stripColor(resolver.getContent()).equals(resolver.getContent()))
                        issueHelper.appendAtPath(WM1);

                    ConfigPathStack stack = issueHelper.stackCopy();
                    issueHelper.pop();
                    return new Message(type, resolver.getContent(), stack);
                })
                .toList();
    }
}
