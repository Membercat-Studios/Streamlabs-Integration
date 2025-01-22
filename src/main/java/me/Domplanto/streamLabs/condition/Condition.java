package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@ConfigPathSegment(id = "condition")
public class Condition {
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

    public boolean check(ActionExecutionContext ctx) {
        String e1 = element1.execute(ctx.baseObject(), ctx);
        String e2 = element2.execute(ctx.baseObject(), ctx);
        try {
            return invert != this.operator.check(Double.parseDouble(e1), Double.parseDouble(e2));
        } catch (NumberFormatException e) {
            return invert != this.operator.check(e1, e2);
        }
    }

    public static List<Condition> parseConditions(List<String> conditionStrings, ConfigIssueHelper issueHelper) {
        return Condition.parseAll(conditionStrings, issueHelper, false);
    }

    public static List<DonationCondition> parseDonationConditions(List<String> conditionStrings, ConfigIssueHelper issueHelper) {
        return Condition.parseAll(conditionStrings, issueHelper, true).stream()
                .filter(condition -> condition instanceof DonationCondition)
                .map(condition -> (DonationCondition) condition)
                .toList();
    }

    private static List<Condition> parseAll(List<String> conditionStrings, ConfigIssueHelper issueHelper, boolean isDonation) {
        return conditionStrings.stream()
                .map(string -> {
                    issueHelper.push(Condition.class, String.valueOf(conditionStrings.indexOf(string)));
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
                }).toList();
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
