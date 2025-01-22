package me.Domplanto.streamLabs.config;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.condition.DonationCondition;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.Domplanto.streamLabs.config.PluginConfig.getString;

@ConfigPathSegment(id = "custom_placeholder")
public final class CustomPlaceholder extends ActionPlaceholder implements YamlPropertyObject {
    public CustomPlaceholder(@NotNull String id, @Nullable String defaultValue, List<StateBasedValue> values) {
        super(id, PlaceholderFunction.of((object, ctx) -> getValue(values, defaultValue, ctx)));
    }

    @NotNull
    private static String getValue(List<StateBasedValue> values, String defaultValue, ActionExecutionContext ctx) {
        for (StateBasedValue value : values) {
            if (value.checkConditions(ctx))
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
                .filter(s -> !s.getName().equals("default_value"))
                .map(s -> {
                    issueHelper.push(StateBasedValue.class, s.getName());
                    StateBasedValue value = YamlPropertyObject.createInstance(StateBasedValue.class, s, issueHelper);
                    issueHelper.pop();
                    return value;
                })
                .toList();

        return new CustomPlaceholder(section.getName(), getString(section, "default_value"), values);
    }

    @ConfigPathSegment(id = "state_based_value")
    public static final class StateBasedValue implements YamlPropertyObject {
        @YamlProperty("!SECTION")
        private @NotNull String id;
        @YamlProperty("value")
        private @Nullable String value;
        @YamlProperty("conditions")
        private List<Condition> conditions = new ArrayList<>();
        @YamlProperty("donation_conditions")
        private List<DonationCondition> donationConditions = new ArrayList<>();

        @YamlPropertyCustomDeserializer(propertyName = "conditions")
        private List<Condition> deserializeConditions(@NotNull List<String> conditionStrings, ConfigIssueHelper issueHelper) {
            return Condition.parseConditions(conditionStrings, issueHelper);
        }

        @YamlPropertyCustomDeserializer(propertyName = "donation_conditions")
        private List<DonationCondition> deserializeDonationConditions(@NotNull List<String> donationConditionStrings, ConfigIssueHelper issueHelper) {
            return Condition.parseDonationConditions(donationConditionStrings, issueHelper);
        }

        public boolean checkConditions(ActionExecutionContext ctx) {
            ArrayList<Condition> conditionList = new ArrayList<>(this.conditions);
            if (ctx.event() instanceof BasicDonationEvent)
                conditionList.addAll(this.donationConditions);

            for (Condition condition : conditionList)
                if (!condition.check(ctx)) return false;

            return true;
        }
    }
}
