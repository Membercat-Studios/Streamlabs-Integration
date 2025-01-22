package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@ConfigPathSegment(id = "condition_group")
public class ConditionGroup implements ConditionBase, YamlPropertyObject {
    @YamlProperty("conditions")
    private List<ConditionBase> conditions = new ArrayList<>();
    @YamlProperty("donation_conditions")
    private List<DonationCondition> donationConditions = new ArrayList<>();
    @YamlProperty("mode")
    private Mode groupMode = Mode.AND;

    @YamlPropertyCustomDeserializer(propertyName = "conditions")
    private List<ConditionBase> deserializeConditions(@NotNull List<Object> rawConditions, ConfigIssueHelper issueHelper) {
        return Condition.parseConditions(rawConditions, issueHelper);
    }

    @YamlPropertyCustomDeserializer(propertyName = "donation_conditions")
    private List<DonationCondition> deserializeDonationConditions(@NotNull List<String> rawDonationConditions, ConfigIssueHelper issueHelper) {
        return Condition.parseDonationConditions(rawDonationConditions, issueHelper);
    }

    @YamlPropertyCustomDeserializer(propertyName = "mode")
    private Mode deserializeGroupMode(@NotNull String modeString, ConfigIssueHelper issueHelper) {
        try {
            return Mode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(ConfigIssue.Level.WARNING, "No condition group mode \"%s\" could be found, defaulting to %s".formatted(modeString, this.groupMode));
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
        }),
        OR((ctx, conditionList) -> {
            for (ConditionBase condition : conditionList)
                if (condition.check(ctx)) return true;

            return false;
        });

        private final BiFunction<ActionExecutionContext, List<? extends ConditionBase>, Boolean> checkFunc;

        Mode(BiFunction<ActionExecutionContext, List<? extends ConditionBase>, Boolean> checkFunc) {
            this.checkFunc = checkFunc;
        }

        public boolean check(ActionExecutionContext ctx, List<? extends ConditionBase> conditions) {
            return checkFunc.apply(ctx, conditions);
        }
    }
}
