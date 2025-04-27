package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.font.DefaultFontInfo;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import static me.Domplanto.streamLabs.config.issue.Issues.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@ReflectUtil.ClassId("message")
public class MessageStep extends AbstractStep<String> {
    private String content;
    private MessageType type;
    @YamlProperty("title_fade_in")
    private Duration titleFadeIn = Title.DEFAULT_TIMES.fadeIn();
    @YamlProperty("title_stay")
    private Duration titleStay = Title.DEFAULT_TIMES.stay();
    @YamlProperty("title_fade_out")
    private Duration titleFadeOut = Title.DEFAULT_TIMES.fadeOut();

    public MessageStep() {
        super(String.class);
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
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
        StreamLabs plugin = getPlugin();
        runOnServerThread(() -> ctx.config().getAffectedPlayers().stream()
                .map(playerName -> plugin.getServer().getPlayerExact(playerName))
                .filter(Objects::nonNull)
                .forEach(player -> {
                    if (this.type == MessageType.TITLE) {
                        player.clearTitle();
                        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(titleFadeIn, titleStay, titleFadeOut));
                    }
                    this.type.sendMessage(player, finalMessage);
                }));
    }

    private Component parsingError(Exception e) {
        String location = this.getLocation().toFormattedString();
        StreamLabs.LOGGER.log(Level.WARNING, "Failed to parse message at %s: ".formatted(location), e);
        return translatable()
                .key("streamlabs.error.message.parse_failed")
                .arguments(text(location))
                .style(Style.style(ColorScheme.INVALID, TextDecoration.ITALIC))
                .build();
    }

    @YamlPropertyCustomDeserializer(propertyName = "title_fade_in")
    public Duration serializeFadeIn(Integer input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (input < 0) {
            issueHelper.appendAtPath(WM2);
            return titleFadeIn;
        }
        return Duration.ofMillis(input);
    }

    @YamlPropertyCustomDeserializer(propertyName = "title_stay")
    public Duration serializeStay(Integer input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (input < 0) {
            issueHelper.appendAtPath(WM2);
            return titleStay;
        }
        return Duration.ofMillis(input);
    }

    @YamlPropertyCustomDeserializer(propertyName = "title_fade_out")
    public Duration serializeFadeOut(Integer input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (input < 0) {
            issueHelper.appendAtPath(WM2);
            return titleFadeOut;
        }
        return Duration.ofMillis(input);
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
