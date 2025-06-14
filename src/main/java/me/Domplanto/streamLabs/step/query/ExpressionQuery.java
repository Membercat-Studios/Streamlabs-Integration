package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ReflectUtil.ClassId("expression")
public class ExpressionQuery extends TransformationQuery<String> {
    private String pattern;
    private @Nullable Pattern compiledPattern;
    @YamlProperty("action")
    private Action action = Action.MATCH;
    @YamlProperty("use_placeholders")
    private boolean usePlaceholders;
    @YamlProperty("replacement")
    private String replacement = "";
    @YamlProperty("group")
    private String group;

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.pattern = data;
        if (this.usePlaceholders) return;
        try {
            this.compiledPattern = Pattern.compile(this.pattern);
            if (this.action != Action.GROUP) return;
            if (this.group == null) {
                issueHelper.appendAtPath(WE3);
                return;
            }
            if (!this.compiledPattern.namedGroups().containsKey(this.group)) {
                issueHelper.appendAtPath(WE2.apply(this.group));
                this.group = null;
            }
        } catch (PatternSyntaxException e) {
            this.pattern = "";
            issueHelper.appendAtPath(WE0.apply(e.getMessage()));
        }
    }

    @Override
    protected String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        Pattern matchingPattern = this.compiledPattern;
        try {
            if (matchingPattern == null)
                matchingPattern = Pattern.compile(ActionPlaceholder.replacePlaceholders(this.pattern, ctx));
        } catch (PatternSyntaxException e) {
            StreamLabs.LOGGER.warning("Failed to compile expression with placeholders from \"%s\"".formatted(this.pattern));
            return null;
        }

        Matcher matcher = matchingPattern.matcher(input);
        return switch (this.action) {
            case REPLACE -> matcher.replaceAll(this.replacement);
            case MATCH -> matcher.find() ? matcher.group() : null;
            case GROUP -> {
                if (!matcher.find() || this.group == null) yield null;
                try {
                    yield matcher.group(this.group);
                } catch (IllegalArgumentException e) {
                    StreamLabs.LOGGER.warning("Couldn't find named group \"%s\" in expression \"%s\"".formatted(this.group, this.pattern));
                    yield null;
                }
            }
        };
    }

    @Override
    public @NotNull Class<String> getExpectedDataType() {
        return String.class;
    }

    @YamlPropertyCustomDeserializer(propertyName = "action")
    public Action deserializeAction(@Nullable String action, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (action == null) return Action.MATCH;
        try {
            return Action.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WE1.apply(action));
            return Action.MATCH;
        }
    }

    public enum Action {
        MATCH,
        GROUP,
        REPLACE
    }
}
