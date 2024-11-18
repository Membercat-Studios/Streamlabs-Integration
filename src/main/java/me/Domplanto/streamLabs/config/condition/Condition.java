package me.Domplanto.streamLabs.config.condition;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Condition {
    private static final Set<? extends Operator> OPERATORS = Operator.findOperatorClasses();
    private final Function<JsonObject, String> element1;
    private final Function<JsonObject, String> element2;
    private final Operator operator;
    private final boolean invert;

    private Condition(Operator operator, boolean invert, Function<JsonObject, String> element1, Function<JsonObject, String> element2) {
        this.element1 = element1;
        this.element2 = element2;
        this.operator = operator;
        this.invert = invert;
    }

    public boolean check(JsonObject object) {
        String e1 = element1.apply(object);
        String e2 = element2.apply(object);
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
                    Operator op = OPERATORS.stream()
                            .filter(operator1 -> finalString.contains(operator1.getName()))
                            .findFirst().orElse(null);
                    if (op == null) return null;

                    String[] elements = finalString.split(op.getName());
                    return new Condition(op, invert, parseElement(elements[0], event), parseElement(elements[1], event));
                }).toList();
    }

    public static List<Condition> parseDonationConditions(List<String> donationConditionStrings, BasicDonationEvent event, JsonObject baseObject) {
        ArrayList<Condition> conditions = new ArrayList<>();
        for (String string : donationConditionStrings) {
            Operator op = OPERATORS.stream()
                    .filter(operator1 -> string.contains(operator1.getName()))
                    .findFirst().orElse(null);
            if (op == null) continue;

            String[] elements = string.split(op.getName());
            if (elements[0].equals(event.getCurrency(baseObject)))
                conditions.add(new Condition(op, false, o -> String.valueOf(event.calculateAmount(o)), parseElement(elements[1], event)));
        }

        return conditions;
    }

    private static Function<JsonObject, String> parseElement(String elementString, StreamlabsEvent event) {
        Function<JsonObject, String> defaultFunc = o -> elementString;
        if (!elementString.startsWith("{") || !elementString.endsWith("}") || elementString.length() < 3)
            return defaultFunc;

        String placeholderName = elementString.substring(1, elementString.length() - 1);
        return event.getPlaceholders()
                .stream()
                .filter(placeholder -> placeholder.name().equals(placeholderName))
                .min(Comparator.comparingInt(p -> p.name().length()))
                .map(ActionPlaceholder::valueFunction)
                .orElse(defaultFunc);
    }
}
