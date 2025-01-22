package me.Domplanto.streamLabs.statistics;

import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;

import java.util.*;

public class EventHistorySelector {
    private final int relativeId;
    private final Set<Condition> placeholderConditions;

    private EventHistorySelector(int relativeId) {
        this.relativeId = relativeId;
        this.placeholderConditions = new HashSet<>();
    }

    private void addConditions(Collection<Condition> conditions) {
        this.placeholderConditions.addAll(conditions);
    }

    public boolean check(int eventIdx, EventHistory.LoggedEvent event) {
        if (eventIdx != this.relativeId) return false;
        for (Condition condition : this.placeholderConditions)
            if (!condition.check(event.createContext())) return false;

        return true;
    }

    public static EventHistorySelector deserialize(String input) throws ConfigLoadedWithIssuesException {
        int end = input.contains("[") ? input.indexOf('[') : input.length() - 1;
        int relId = parseRelativeId(input.substring(0, end));
        EventHistorySelector selector = new EventHistorySelector(relId);
        if (!input.contains("[") || !input.contains("]")) return selector;

        String selectorContent = input.substring(input.indexOf('[') + 1, input.indexOf(']'));
        List<String> conditionStrings = Arrays.stream(selectorContent.split(",")).map(String::trim).toList();
        ConfigIssueHelper issueHelper = new ConfigIssueHelper(null);
        selector.addConditions(Condition.parseConditions(conditionStrings, issueHelper));
        issueHelper.complete();

        return selector;
    }

    private static int parseRelativeId(String input) {
        if (input.equals("last")) return 0;
        return Math.max(0, Integer.parseInt(input));
    }
}
