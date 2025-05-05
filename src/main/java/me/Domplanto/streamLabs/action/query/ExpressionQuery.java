package me.Domplanto.streamLabs.action.query;

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

import static me.Domplanto.streamLabs.config.issue.Issues.WE0;
import static me.Domplanto.streamLabs.config.issue.Issues.WE1;

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

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.pattern = data;
        if (this.usePlaceholders) return;
        try {
            this.compiledPattern = Pattern.compile(this.pattern);
        } catch (PatternSyntaxException e) {
            this.pattern = "";
            issueHelper.appendAtPath(WE0.apply(e.getMessage()));
        }
    }

    @Override
    protected String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        Pattern matchingPattern = this.compiledPattern;
        if (matchingPattern == null)
            matchingPattern = Pattern.compile(ActionPlaceholder.replacePlaceholders(pattern, ctx));

        Matcher matcher = matchingPattern.matcher(input);
        return switch (this.action) {
            case REPLACE -> matcher.replaceAll(this.replacement);
            case MATCH -> matcher.find() ? matcher.group() : null;
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
        REPLACE
    }
}
