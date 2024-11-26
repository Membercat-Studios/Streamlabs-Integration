package me.Domplanto.streamLabs.condition;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Condition {
    private static final Set<? extends Operator> OPERATORS = Operator.findOperatorClasses();
    private final ActionPlaceholder.PlaceholderFunction element1;
    private final ActionPlaceholder.PlaceholderFunction element2;
    private final Operator operator;
    private final boolean invert;

    private Condition(Operator operator, boolean invert, ActionPlaceholder.PlaceholderFunction element1, ActionPlaceholder.PlaceholderFunction element2) {
        this.element1 = element1;
        this.element2 = element2;
        this.operator = operator;
        this.invert = invert;
    }

    public boolean check(StreamlabsEvent event, JsonObject object) {
        String e1 = element1.execute(object, event);
        String e2 = element2.execute(object, event);
        try {
            return invert != this.operator.check(Double.parseDouble(e1), Double.parseDouble(e2));
        } catch (NumberFormatException e) {
            return invert != this.operator.check(e1, e2);
        }
    }

    public static List<Condition> parseAll(List<String> conditionStrings, StreamlabsEvent event) {
        return conditionStrings.stream()
                .map(string -> {
                    boolean invert = string.startsWith("!");
                    if (invert)
                        string = string.substring(1);

                    final String finalString = string;
                    Operator op = findOperator(finalString);
                    if (op == null) return null;

                    String[] elements = finalString.split(op.getName());
                    return new Condition(op, invert, parseElement(elements[0], event), parseElement(elements[1], event));
                }).toList();
    }

    public static List<Condition> parseDonationConditions(List<String> donationConditionStrings, BasicDonationEvent event, JsonObject baseObject) {
        ArrayList<Condition> conditions = new ArrayList<>();
        for (String string : donationConditionStrings) {
            Operator op = findOperator(string);
            if (op == null) continue;

            String[] elements = string.split(op.getName());
            if (elements[0].equals(event.getCurrency(baseObject)))
                conditions.add(new Condition(op, false, ActionPlaceholder.PlaceholderFunction.of(o -> String.valueOf(event.calculateAmount(o))),
                        parseElement(elements[1], event)));
        }

        return conditions;
    }

    private static ActionPlaceholder.PlaceholderFunction parseElement(String elementString, StreamlabsEvent event) {
        ActionPlaceholder.PlaceholderFunction defaultFunc = ActionPlaceholder.PlaceholderFunction.of(elementString);
        if (!elementString.startsWith("{") || !elementString.endsWith("}") || elementString.length() < 3)
            return defaultFunc;

        String placeholderName = elementString.substring(1, elementString.length() - 1);
        return event.getPlaceholders()
                .stream()
                .filter(placeholder -> placeholder.name().equals(placeholderName))
                .min(Comparator.comparingInt(p -> p.name().length()))
                .map(ActionPlaceholder::function)
                .orElse(defaultFunc);
    }

    private static Operator findOperator(String condition) {
        return OPERATORS.stream()
                .sorted(Comparator.comparing(o -> o.getName().length(), (len1, len2) -> len2 - len1))
                .filter(operator1 -> condition.contains(operator1.getName()))
                .findFirst().orElse(null);
    }
}
