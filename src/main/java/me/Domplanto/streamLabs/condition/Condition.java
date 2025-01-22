package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.*;

@ConfigPathSegment(id = "condition")
public class Condition implements ConditionBase {
    private static final Set<? extends Operator> OPERATORS = Operator.findOperatorClasses();
    private final ActionPlaceholder.PlaceholderFunction element1;
    private final ActionPlaceholder.PlaceholderFunction element2;
    private final Operator operator;
    private final boolean invert;

    protected Condition(Operator operator, boolean invert, ActionPlaceholder.PlaceholderFunction element1, ActionPlaceholder.PlaceholderFunction element2) {
        this.element1 = element1;
        this.element2 = element2;
        this.operator = operator;
        this.invert = invert;
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        String e1 = element1.execute(ctx.baseObject(), ctx);
        String e2 = element2.execute(ctx.baseObject(), ctx);
        try {
            return invert != this.operator.check(Double.parseDouble(e1), Double.parseDouble(e2));
        } catch (NumberFormatException e) {
            return invert != this.operator.check(e1, e2);
        }
    }

    public static List<ConditionBase> parseConditions(List<?> rawConditions, ConfigIssueHelper issueHelper) {
        return Condition.parseAll(rawConditions, issueHelper, false);
    }

    public static List<DonationCondition> parseDonationConditions(List<String> rawDonationConditions, ConfigIssueHelper issueHelper) {
        return Condition.parseAll(rawDonationConditions, issueHelper, true).stream()
                .filter(condition -> condition instanceof DonationCondition)
                .map(condition -> (DonationCondition) condition)
                .toList();
    }

    private static List<ConditionBase> parseAll(List<?> rawConditions, ConfigIssueHelper issueHelper, boolean isDonation) {
        return rawConditions.stream()
                .map(obj -> {
                    if (obj instanceof String string)
                        return parseStr(rawConditions.indexOf(string), string, issueHelper, isDonation);
                    if (obj instanceof HashMap<?, ?> objMap) {
                        ConfigurationSection newSection = new MemoryConfiguration().createSection("group", objMap);
                        return YamlPropertyObject.createInstance(ConditionGroup.class, newSection, issueHelper);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static Condition parseStr(int idx, String string, ConfigIssueHelper issueHelper, boolean isDonation) {
        issueHelper.push(isDonation ? DonationCondition.class : Condition.class, String.valueOf(idx));
        boolean invert = string.startsWith("!");
        if (invert)
            string = string.substring(1);

        final String finalString = string;
        Operator op = findOperator(finalString);
        if (op == null) {
            issueHelper.appendAtPath(ConfigIssue.Level.WARNING, "No valid condition operator found, skipping condition");
            issueHelper.pop();
            return null;
        }

        String[] elements = finalString.split(op.getName());
        if (elements.length < 1) {
            issueHelper.appendAtPath(ConfigIssue.Level.WARNING, "Condition contains no elements and will be skipped");
            issueHelper.pop();
            return null;
        }

        ActionPlaceholder.PlaceholderFunction e1 = parseElement(elements[0], issueHelper);
        ActionPlaceholder.PlaceholderFunction e2 = elements.length >= 2 ? parseElement(elements[1], issueHelper)
                : ActionPlaceholder.PlaceholderFunction.of("");
        issueHelper.pop();
        if (isDonation)
            return new DonationCondition(op, invert, e1, e2);
        else
            return new Condition(op, invert, e1, e2);
    }

    private static ActionPlaceholder.PlaceholderFunction parseElement(String elementString, ConfigIssueHelper issueHelper) {
        if (elementString.contains("{") && !elementString.contains("}"))
            issueHelper.appendAtPath(ConfigIssue.Level.HINT, "Condition element contains a non-terminated placeholder");

        return ActionPlaceholder.PlaceholderFunction.of((object, ctx) -> ActionPlaceholder.replacePlaceholders(elementString, ctx));
    }

    private static Operator findOperator(String condition) {
        return OPERATORS.stream()
                .sorted(Comparator.comparing(o -> o.getName().length(), (len1, len2) -> len2 - len1))
                .filter(operator1 -> condition.contains(operator1.getName()))
                .findFirst().orElse(null);
    }

    protected ActionPlaceholder.PlaceholderFunction getElement1() {
        return element1;
    }

    protected ActionPlaceholder.PlaceholderFunction getElement2() {
        return element2;
    }

    protected Operator getOperator() {
        return operator;
    }

    protected boolean isInverted() {
        return invert;
    }
}
