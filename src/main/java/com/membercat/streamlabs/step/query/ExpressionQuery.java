package com.membercat.streamlabs.step.query;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.config.placeholder.PropertyPlaceholder;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import com.membercat.streamlabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.membercat.streamlabs.config.issue.Issues.*;

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
            issueHelper.appendAtPath(e.getIndex() != -1 ? WE0D.apply(e.getDescription(), e.getIndex()) : WE0.apply(e.getDescription()));
        }
    }

    @Override
    protected @NotNull AbstractPlaceholder query(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        Pattern matchingPattern = this.compiledPattern;
        try {
            if (matchingPattern == null)
                matchingPattern = Pattern.compile(AbstractPlaceholder.replacePlaceholders(this.pattern, ctx));
        } catch (PatternSyntaxException e) {
            StreamLabs.LOGGER.warning("Failed to compile expression with placeholders from \"%s\": %s".formatted(this.pattern, e.getDescription()));
            return createPlaceholder(null);
        }

        Matcher matcher = matchingPattern.matcher(input);
        return switch (this.action) {
            case REPLACE -> createPlaceholder(matcher.replaceAll(this.replacement));
            case MATCH -> createPlaceholder(matcher.find() ? matcher.group() : null);
            case MATCH_GROUPS -> {
                if (!matcher.find()) yield createPlaceholder(null);
                PropertyPlaceholder placeholder = new PropertyPlaceholder(outputName(), QueryPlaceholder.FORMAT)
                        .withDefaultValue(matcher.group());
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    final int groupId = i;
                    String name = matcher.namedGroups()
                            .entrySet().stream()
                            .filter(e -> e.getValue() == groupId)
                            .findAny().map(Map.Entry::getKey)
                            .orElse("group" + groupId);
                    placeholder.addProperty(name, matcher.group(i));
                }
                yield placeholder;
            }
        };
    }

    @Override
    protected @Nullable String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        return null;
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
        MATCH_GROUPS,
        REPLACE
    }
}
