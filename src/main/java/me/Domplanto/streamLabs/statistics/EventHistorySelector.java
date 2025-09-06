package me.Domplanto.streamLabs.statistics;

import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.condition.ConditionBase;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.ToIntFunction;

public class EventHistorySelector implements EventFilter {
    private final int relativeId;
    private final Set<ConditionBase> placeholderConditions;

    private EventHistorySelector(int relativeId) {
        this.relativeId = relativeId;
        this.placeholderConditions = new HashSet<>();
    }

    private void addConditions(Collection<ConditionBase> conditions) {
        this.placeholderConditions.addAll(conditions);
    }

    @Override
    public boolean checkRelativeId(int id) {
        return this.relativeId == id;
    }

    @Override
    public boolean check(@NotNull EventHistory.LoggedEvent event) {
        for (ConditionBase condition : this.placeholderConditions)
            if (!condition.check(event.createContext())) return false;

        return true;
    }

    public static Pair<String, ConditionGroup> parseOptionalFilter(@NotNull String input, ConfigIssueHelper issueHelper) {
        ToIntFunction<ConditionGroup.Mode> getIndexFunc = mode -> input.indexOf(mode.getStartBracket());
        int idx = Arrays.stream(ConditionGroup.Mode.values())
                .sorted(Comparator.comparingInt(getIndexFunc))
                .filter(mode -> getIndexFunc.applyAsInt(mode) != -1)
                .findFirst().map(getIndexFunc::applyAsInt).orElse(input.length());
        String rawGroup = idx < input.length() - 1 ? input.substring(idx) : null;
        List<ConditionBase> parsed = Condition.parseConditions(Optional.ofNullable(rawGroup).map(List::of).orElseGet(List::of), issueHelper);
        return Pair.of(input.substring(0, idx), ConditionGroup.of(ConditionGroup.Mode.AND, parsed.toArray(new ConditionBase[0])));
    }

    public static EventHistorySelector deserialize(String input, ConfigIssueHelper issueHelper) {
        Pair<String, ConditionGroup> data = parseOptionalFilter(input, issueHelper);
        int relId = parseRelativeId(data.getKey());
        EventHistorySelector selector = new EventHistorySelector(relId);
        selector.addConditions(data.getValue().conditions);
        return selector;
    }

    private static int parseRelativeId(String input) {
        if (input.equals("last")) return 0;
        try {
            return Math.max(0, Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
