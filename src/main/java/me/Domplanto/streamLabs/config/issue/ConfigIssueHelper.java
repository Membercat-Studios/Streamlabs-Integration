package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.command.ReloadSubCommand;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static me.Domplanto.streamLabs.config.issue.Issues.EI1;
import static net.kyori.adventure.text.Component.*;

public class ConfigIssueHelper {
    private final IssueList issues;
    private final ConfigPathStack pathStack;
    private final Logger logger;

    public ConfigIssueHelper(Logger logger) {
        this.issues = new IssueList();
        this.pathStack = new ConfigPathStack();
        this.logger = logger;
    }

    public void reset() {
        this.issues.clear();
        this.pathStack.clear();
    }

    public void complete() throws ConfigLoadedWithIssuesException {
        if (!pathStack.empty()) {
            this.reset();
            this.appendAtPathAndLog(EI1, new IllegalStateException("Path stack not empty"));
        }

        if (!this.issues.isEmpty())
            throw new ConfigLoadedWithIssuesException(this.issues);
    }

    public void pushSection(String name) {
        this.push(ConfigPathStack.Section.class, name);
    }

    public void pushProperty(String name) {
        this.push(ConfigPathStack.Property.class, name);
    }

    public void push(Class<?> segment, String name) {
        ConfigPathSegment annotation = segment.getAnnotation(ConfigPathSegment.class);
        this.pathStack.push(new ConfigPathStack.Entry(segment, annotation, name,
                !this.pathStack.empty() ? this.pathStack.peek().suppressedIssues() : new HashSet<>()));
    }

    public void pop() {
        this.pathStack.pop();
    }

    public void popIfSection() {
        if (!this.pathStack.isEmpty() && this.pathStack.peek().isSection())
            this.pathStack.pop();
    }

    public void popIfProperty() {
        if (!this.pathStack.isEmpty() && this.pathStack.peek().isProperty())
            this.pathStack.pop();
    }

    public void suppress(Collection<String> issueIds) {
        if (this.pathStack.isEmpty()) return;
        this.pathStack.peek().suppress(issueIds);
    }

    public void appendAtPath(ConfigIssue issue) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(issue, this.stackCopy());
    }

    public void appendAtPathAndLog(ConfigIssue issue, Throwable throwable) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(issue, this.stackCopy());
        this.logger.log(issue.getLevel().getLogLevel(), "Detailed exception for config issue \"%s\" at %s:".formatted(issue.getDescription(), pathStack.toFormattedString()), throwable);
    }

    private boolean checkIssueSuppressed(ConfigIssue issue) {
        return !this.pathStack.isEmpty() && this.pathStack.peek().suppressedIssues().contains(issue.getId());
    }

    public @NotNull ConfigPathStack stackCopy() {
        return this.pathStack.clone();
    }

    public static class IssueList extends ArrayList<IssueList.RecordedIssue> {
        public record RecordedIssue(
                ConfigIssue issue,
                ConfigPathStack location
        ) {
            public Component getMessage(boolean includePath) {
                TextColor color = issue.getLevel().getColor();
                Component description = issue.getDescription().color(color == ColorScheme.COMMENT ? ColorScheme.WHITE : color);
                List<Component> args = new ArrayList<>(List.of(issue.getLevel().translatable(), text(issue.getId(), color),
                        includePath ? location.toComponent().color(ColorScheme.COMMENT) : description));
                if (includePath)
                    args.add(description);
                return translatable()
                        .key(includePath ? "streamlabs.issue.format" : "streamlabs.issue.format.no_path")
                        .color(ColorScheme.WHITE).arguments(args)
                        .build();
            }
        }

        public void add(ConfigIssue issue, ConfigPathStack location) {
            this.add(new RecordedIssue(issue, location));
        }

        public Component getListMessage(long limit) {
            TextComponent.Builder builder = text()
                    .append(Translations.SEPARATOR_LINE).append(newline())
                    .append(translatable().key("streamlabs.issue.list.title").color(ColorScheme.DISABLE))
                    .append(text("\n\n"));
            Set<String> longIds = new HashSet<>();
            this.forEach(issue -> {
                if (this.indexOf(issue) >= (limit - 1) && limit != -1) return;
                if (longIds.contains(issue.issue().getId())) return;
                long count = stream().map(RecordedIssue::issue).filter(Predicate.isEqual(issue.issue())).count();
                if (count > 2) {
                    builder.append(text().content("x%d ".formatted(count)).color(ColorScheme.DISABLE));
                    longIds.add(issue.issue().getId());
                }

                builder.append(issue.getMessage(count <= 2));
                if (!this.getLast().equals(issue))
                    builder.append(text("\n\n"));
            });
            long distCount = stream().map(RecordedIssue::issue).distinct().count();
            if (distCount > limit && limit != -1)
                builder.append(translatable("streamlabs.issue.list.view_more", ColorScheme.COMMENT, text(distCount - limit))).append(space())
                        .append(translatable("streamlabs.issue.list.show_in_console", Style.style(ColorScheme.DONE, TextDecoration.UNDERLINED))
                                .hoverEvent(HoverEvent.showText(translatable("streamlabs.tooltip.show_in_console")))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, ReloadSubCommand.SHOW_IN_CONSOLE)))
                        .append(newline());

            return builder.append(Translations.SEPARATOR_LINE).build();
        }
    }
}
