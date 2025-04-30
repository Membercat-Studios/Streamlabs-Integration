package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static me.Domplanto.streamLabs.config.issue.Issues.WC0;

@ConfigPathSegment(id = "condition_group")
public class ConditionGroup implements ConditionBase, YamlPropertyObject {
    @YamlProperty("conditions")
    public List<ConditionBase> conditions = new ArrayList<>();
    @YamlProperty("donation_conditions")
    public List<DonationCondition> donationConditions = new ArrayList<>();
    @YamlProperty("mode")
    public Mode groupMode = Mode.AND;

    @YamlPropertyCustomDeserializer(propertyName = "conditions")
    private List<ConditionBase> deserializeConditions(@NotNull List<Object> rawConditions, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return Condition.parseConditions(rawConditions, issueHelper);
    }

    @YamlPropertyCustomDeserializer(propertyName = "donation_conditions")
    private List<DonationCondition> deserializeDonationConditions(@NotNull List<String> rawDonationConditions, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return Condition.parseDonationConditions(rawDonationConditions, issueHelper);
    }

    @YamlPropertyCustomDeserializer(propertyName = "mode")
    private Mode deserializeGroupMode(@NotNull String modeString, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        try {
            return Mode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WC0.apply(modeString, this.groupMode));
            return this.groupMode;
        }
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        return (conditions.isEmpty() || groupMode.check(ctx, this.conditions))
                && (donationConditions.isEmpty() || (ctx.isDonation() && Mode.AND.check(ctx, this.donationConditions)));
    }

    public enum Mode {
        AND((ctx, conditionList) -> {
            for (ConditionBase condition : conditionList)
                if (!condition.check(ctx)) return false;

            return true;
        }, '[', ']'),
        OR((ctx, conditionList) -> {
            for (ConditionBase condition : conditionList)
                if (condition.check(ctx)) return true;

            return false;
        }, '(', ')');

        private final BiFunction<ActionExecutionContext, List<? extends ConditionBase>, Boolean> checkFunc;
        private final char startBracket;
        private final char endBracket;

        Mode(BiFunction<ActionExecutionContext, List<? extends ConditionBase>, Boolean> checkFunc, char startBracket, char endBracket) {
            this.checkFunc = checkFunc;
            this.startBracket = startBracket;
            this.endBracket = endBracket;
        }

        public boolean check(ActionExecutionContext ctx, List<? extends ConditionBase> conditions) {
            return checkFunc.apply(ctx, conditions);
        }

        @Nullable
        public static Mode getFromStartBracket(char startBracket) {
            return Arrays.stream(values())
                    .filter(mode -> mode.startBracket == startBracket)
                    .findAny().orElse(null);
        }

        public char getStartBracket() {
            return this.startBracket;
        }

        public char getEndBracket() {
            return this.endBracket;
        }
    }
}
