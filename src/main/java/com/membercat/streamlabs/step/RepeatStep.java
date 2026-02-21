package com.membercat.streamlabs.step;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import com.membercat.streamlabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.membercat.streamlabs.config.issue.Issues.WRP0;

@ReflectUtil.ClassId("repeat")
public class RepeatStep extends AbstractLogicStep {
    @YamlProperty("amount")
    private String amount = String.valueOf(2);

    @YamlPropertyCustomDeserializer(propertyName = "amount")
    public String deserializeAmount(Integer input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (input < 0) {
            input = 0;
            issueHelper.appendAtPath(WRP0);
        }
        return input.toString();
    }

    @Override
    public @NotNull Collection<? extends StepBase<?>> getSteps(ActionExecutionContext ctx) {
        String parsed = AbstractPlaceholder.replacePlaceholders(this.amount, ctx);
        int amount;
        try {
            amount = Integer.parseInt(parsed);
            if (amount < 0) {
                StreamLabs.LOGGER.warning("Negative repeat amount found (%s, resolved from \"%s\") at %s, skipping!".formatted(amount, this.amount, getLocation().toFormattedString()));
                return List.of();
            }
        } catch (NumberFormatException e) {
            StreamLabs.LOGGER.warning("Failed to parse repeat amount \"%s\" (resolved from \"%s\") at %s, skipping!".formatted(parsed, this.amount, getLocation().toFormattedString()));
            return List.of();
        }

        List<StepBase<?>> steps = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            steps.addAll(steps());
        }
        return steps;
    }
}
