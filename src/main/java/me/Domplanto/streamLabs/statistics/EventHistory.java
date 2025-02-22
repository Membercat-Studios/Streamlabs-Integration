package me.Domplanto.streamLabs.statistics;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class EventHistory {
    private final Stack<LoggedEvent> executionHistory;
    private final HashMap<String, String> giftedMembershipIdMap;

    public EventHistory() {
        this.executionHistory = new Stack<>();
        this.giftedMembershipIdMap = new HashMap<>();
    }

    public void store(StreamlabsEvent event, PluginConfig config, JsonObject baseObject) {
        this.executionHistory.push(new LoggedEvent(event, config, baseObject, new Date().getTime()));
    }

    public void storeGiftedMembershipId(String id, String userName) {
        this.giftedMembershipIdMap.put(id, userName);
    }

    @Nullable
    public String getUserForMembershipId(String id) {
        return this.giftedMembershipIdMap.get(id);
    }

    @Nullable
    public LoggedEvent getEvent(EventFilter filter) {
        List<LoggedEvent> matchingEvents = executionHistory.reversed()
                .stream().filter(filter::check)
                .toList();

        for (int i = 0; i < matchingEvents.size(); i++) {
            if (filter.checkRelativeId(i))
                return matchingEvents.get(i);
        }

        return null;
    }

    public record LoggedEvent(
            StreamlabsEvent event,
            PluginConfig config,
            JsonObject baseObject,
            long timestamp
    ) {
        public String replacePlaceholders(String originalString) {
            return ActionPlaceholder.replacePlaceholders(originalString, createContext());
        }

        public ActionExecutionContext createContext() {
            return new ActionExecutionContext(event, null, config, null, baseObject);
        }
    }
}
