package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "condition")
public class Condition implements ConditionBase {
    private static final Set<? extends Operator> OPERATORS = Operator.findOperatorClasses();
    private static final String SEPARATOR_REGEX = "\\|";
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
                        issueHelper.push(ConditionGroup.class, String.valueOf(rawConditions.indexOf(objMap)));
                        ConfigurationSection newSection = new MemoryConfiguration().createSection("group", objMap);
                        ConditionGroup group = YamlPropertyObject.createInstance(ConditionGroup.class, newSection, issueHelper);
                        issueHelper.pop();
                        return group;
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static ConditionBase parseStr(int idx, String input, ConfigIssueHelper issueHelper, boolean isDonation) {
        String str = input.trim();
        return Objects.requireNonNullElseGet(parseStr(idx, str, issueHelper),
                () -> Condition.parseConditionStr(idx, str, issueHelper, isDonation));
    }

    @Nullable
    private static ConditionBase parseStr(int idx, String input, ConfigIssueHelper issueHelper) {
        ConditionGroup.Mode mode = ConditionGroup.Mode.getFromStartBracket(input.charAt(0));
        if (mode == null) return null;
        issueHelper.push(ConditionGroup.class, String.valueOf(idx));
        int end = input.lastIndexOf(mode.getEndBracket());
        if (end == -1) {
            issueHelper.appendAtPath(HCG0);
            issueHelper.pop();
            return null;
        }

        ConditionGroup root = new ConditionGroup();
        root.groupMode = mode;
        int conditionIdx = 0;
        String[] subElements = input.substring(1, end).split(SEPARATOR_REGEX);
        AtomicInteger skipped;
        for (int i = 0; i < subElements.length; i++) {
            ConditionBase condition = tryParseFromElements(skipped = new AtomicInteger(0), conditionIdx,
                    Arrays.copyOfRange(subElements, i, subElements.length), issueHelper);
            if (condition != null)
                root.conditions.add(condition);
            conditionIdx++;
            i += skipped.get();
        }

        issueHelper.pop();
        return root;
    }

    private static ConditionBase tryParseFromElements(AtomicInteger skipped, int conditionIdx, String[] elements, ConfigIssueHelper issueHelper) {
        ConditionBase base = parseStr(conditionIdx, elements[0], issueHelper, false);
        if (issueHelper.lastIssueIs(HCG0) && elements.length > 1) {
            do issueHelper.removeLast();
            while (issueHelper.lastIssueIs(WC1, WC2, HC0));
            skipped.incrementAndGet();
            String[] newElements = {elements[0] + "|" + elements[1]};
            if (elements.length > 2)
                newElements = ArrayUtils.addAll(newElements, Arrays.copyOfRange(elements, 2, elements.length));
            return tryParseFromElements(skipped, conditionIdx, newElements, issueHelper);
        }

        return base;
    }

    @Nullable
    private static Condition parseConditionStr(int idx, String string, ConfigIssueHelper issueHelper, boolean isDonation) {
        issueHelper.push(isDonation ? DonationCondition.class : Condition.class, String.valueOf(idx));
        boolean invert = string.startsWith("!");
        if (invert)
            string = string.substring(1);

        final String finalString = string;
        Operator op = findOperator(finalString);
        if (op == null) {
            issueHelper.appendAtPath(WC1);
            issueHelper.pop();
            return null;
        }

        String[] elements = finalString.split(op.getName());
        if (elements.length < 1) {
            issueHelper.appendAtPath(WC2);
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
            issueHelper.appendAtPath(HC0);

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
