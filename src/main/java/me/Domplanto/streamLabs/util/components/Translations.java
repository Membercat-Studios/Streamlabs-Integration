package me.Domplanto.streamLabs.util.components;

import me.Domplanto.streamLabs.command.ReloadSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.*;

public final class Translations {
    private static final String STATUS_MESSAGE_PERMISSION = "streamlabs.status";
    private static final String MINIMESSAGE_URL = "https://docs.advntr.dev/minimessage";
    public static final Component MINIMESSAGE_LINK = text()
            .content(MINIMESSAGE_URL)
            .style(Style.style(ColorScheme.DONE, TextDecoration.UNDERLINED))
            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, MINIMESSAGE_URL))
            .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.more_info")))
            .build();
    private static final String REPO_URL = "https://github.com/Membercat-Studios/Streamlabs-Integration";
    private static final String WIKI_URL = "%s/wiki".formatted(REPO_URL);
    public static final String ISSUES_URL = "%s/issues".formatted(REPO_URL);
    public static Component UNEXPECTED_ERROR = translatable()
            .key("streamlabs.command.error.unexpected")
            .arguments(createIssue("streamlabs.command.error.unexpected"))
            .color(ColorScheme.ERROR).build();
    public static Component ACTION_FAILURE = translatable()
            .key("streamlabs.error.action_failure")
            .arguments(createIssue("streamlabs.error.action_failure"))
            .color(ColorScheme.ERROR).build();
    public static Component SEPARATOR_LINE = translatable()
            .key("streamlabs.chat.separator")
            .decorate(TextDecoration.STRIKETHROUGH)
            .color(ColorScheme.STREAMLABS).build();

    private static Component createIssue(String baseKey) {
        return translatable()
                .key("%s.issue_report".formatted(baseKey))
                .style(Style.style(ColorScheme.INVALID, TextDecoration.UNDERLINED))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, ISSUES_URL))
                .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.report_issue")))
                .build();
    }

    public static String wikiPage(@NotNull String url) {
        return WIKI_URL + url;
    }

    public static Component withPrefix(Component component, boolean responsePrefix) {
        return text()
                .content(responsePrefix ? "" : "[").color(ColorScheme.COMMENT)
                .append(translatable()
                        .key(responsePrefix ? "streamlabs.prefix.response" : "streamlabs.prefix")
                        .color(ColorScheme.STREAMLABS)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, WIKI_URL))
                        .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.more_info"))))
                .append(text().content(responsePrefix ? " -> " : "] ").color(ColorScheme.COMMENT))
                .append(component)
                .build();
    }

    public static Component withViewInConsole(Component component) {
        return component.append(space())
                .append(translatable()
                        .key("streamlabs.issue.list.show_in_console")
                        .style(Style.style(ColorScheme.DONE, TextDecoration.UNDERLINED))
                        .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.show_in_console")))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, ReloadSubCommand.SHOW_IN_CONSOLE))
                        .build())
                .append(newline());
    }

    public static void sendPrefixedResponse(String translationKey, TextColor color, CommandSender sender, ComponentLike... args) {
        sender.sendMessage(withPrefix(translatable(translationKey, args).color(color), true));
    }

    public static void sendPrefixedToPlayers(String translationKey, TextColor color, Server server, ComponentLike... args) {
        sendPrefixedToPlayers(translationKey, color, server, false, args);
    }

    public static void sendPrefixedToPlayers(String translationKey, TextColor color, Server server, boolean response, ComponentLike... args) {
        sendPrefixedToPlayers(translatable(translationKey, color, args), server, response);
    }

    public static void sendPrefixedToPlayers(Component component, Server server, boolean response) {
        server.getOnlinePlayers()
                .stream().filter(player -> player.hasPermission(STATUS_MESSAGE_PERMISSION))
                .forEach(player -> player.sendMessage(withPrefix(component, response)));
    }
}
