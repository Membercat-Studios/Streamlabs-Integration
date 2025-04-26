package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.font.DefaultFontInfo;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.logging.Level;

import static me.Domplanto.streamLabs.config.issue.Issues.WM0;
import static me.Domplanto.streamLabs.config.issue.Issues.WM1;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@ReflectUtil.ClassId("message")
public class MessageStep extends AbstractStep<String> {
    private ConfigPathStack location;
    private String content;
    private MessageType type;

    public MessageStep() {
        super(String.class);
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper) {
        MessageType type = MessageType.MESSAGE;
        BracketResolver resolver = new BracketResolver(data).resolve(issueHelper);
        this.content = resolver.getContent();
        String typeStr = resolver.getBracketContents().map(String::toUpperCase).orElse("MESSAGE");
        try {
            type = MessageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WM0.apply(typeStr, type.name()));
        }

        this.type = type;
        //noinspection deprecation
        if (!ChatColor.stripColor(resolver.getContent()).equals(resolver.getContent()))
            issueHelper.appendAtPath(WM1);

        this.location = issueHelper.stackCopy();
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        String content = ActionPlaceholder.replacePlaceholders(this.content, ctx);
        //noinspection deprecation
        content = ChatColor.translateAlternateColorCodes('&', content);
        Component message;
        try {
            message = MiniMessage.miniMessage().deserialize(content);
        } catch (ParsingException e) {
            // Kind of hacky way to determine if MiniMessage throws an exception related to
            // legacy formatting codes, there seems to be no other way of determining the exception reason.
            if (e.getMessage().contains("Legacy formatting codes"))
                message = LegacyComponentSerializer.legacySection().deserialize(content);
            else
                message = parsingError(e);

        } catch (Exception e) {
            message = parsingError(e);
        }

        Component finalMessage = message;
        runOnServerThread(() -> ctx.config().getAffectedPlayers().stream()
                .map(playerName -> getPlugin().getServer().getPlayerExact(playerName))
                .forEach(player -> this.type.sendMessage(player, finalMessage)));
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

    public enum MessageType {
        MESSAGE(Player::sendMessage),
        MESSAGE_CENTERED((player, message) -> player.sendMessage(DefaultFontInfo.centerMessage(message))),
        TITLE((player, message) -> player.sendTitlePart(TitlePart.TITLE, message)),
        SUBTITLE((player, message) -> player.sendTitlePart(TitlePart.SUBTITLE, message));

        private final BiConsumer<Player, Component> action;

        MessageType(BiConsumer<Player, Component> action) {
            this.action = action;
        }

        public void sendMessage(Player player, Component message) {
            if (player != null)
                this.action.accept(player, message);
        }
    }
}
