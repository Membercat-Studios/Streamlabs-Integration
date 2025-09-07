package me.Domplanto.streamLabs.config.placeholder;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@ConfigPathSegment(id = "custom_placeholder")
public final class CustomPlaceholder extends ActionPlaceholder implements YamlPropertyObject {
    public CustomPlaceholder(@NotNull String id, @Nullable String defaultValue, List<StateBasedValue> values) {
        super(id, PlaceholderFunction.of((object, ctx) -> getValue(values, defaultValue, ctx)));
    }

    @NotNull
    private static String getValue(List<StateBasedValue> values, String defaultValue, ActionExecutionContext ctx) {
        for (StateBasedValue value : values) {
            if (value.check(ctx))
                return value.value != null ? value.value : value.id;
        }

        return defaultValue != null ? defaultValue : "";
    }

    @YamlPropertyCustomDeserializer
    private static CustomPlaceholder deserialize(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        List<CustomPlaceholder.StateBasedValue> values = section.getKeys(false)
                .stream()
                .map(section::getConfigurationSection)
                .filter(Objects::nonNull)
                .filter(s -> !s.getName().equals("default_value") || !s.getName().equals("__suppress"))
                .map(s -> {
                    issueHelper.process(s.getName());
                    issueHelper.push(StateBasedValue.class, s.getName());
                    StateBasedValue value = YamlPropertyObject.createInstance(StateBasedValue.class, s, issueHelper);
                    issueHelper.pop();
                    return value;
                })
                .toList();

        issueHelper.process("default_value");
        return new CustomPlaceholder(section.getName(), YamlPropertyObject.getString(section, "default_value"), values);
    }

    @Override
    public @NotNull String getFormat() {
        return "{!%s}".formatted(name());
    }

    @ConfigPathSegment(id = "state_based_value")
    public static final class StateBasedValue extends ConditionGroup {
        @YamlProperty("!SECTION")
        private @NotNull String id;
        @YamlProperty("value")
        private @Nullable String value;
    }
}
