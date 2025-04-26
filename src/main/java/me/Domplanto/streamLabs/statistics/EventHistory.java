package me.Domplanto.streamLabs.statistics;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EventHistory {
    private final Stack<LoggedEvent> executionHistory;
    private final HashMap<String, String> giftedMembershipIdMap;
    private final Set<HistoryChangedListener> listeners;

    public EventHistory() {
        this.executionHistory = new Stack<>();
        this.giftedMembershipIdMap = new HashMap<>();
        this.listeners = new HashSet<>();
    }

    public void store(StreamlabsEvent event, PluginConfig config, JsonObject baseObject) {
        LoggedEvent newEvent = new LoggedEvent(event, config, baseObject, new Date().getTime());
        this.executionHistory.push(newEvent);
        this.listeners.forEach(listener -> listener.onHistoryChanged(this, newEvent));
    }

    public void storeGiftedMembershipId(String id, String userName) {
        this.giftedMembershipIdMap.put(id, userName);
    }

    public void registerListeners(HistoryChangedListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
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
