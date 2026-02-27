package com.membercat.streamlabs.statistics;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.database.DatabaseManager;
import com.membercat.streamlabs.events.StreamlabsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class EventHistory {
    private final Stack<LoggedEvent> executionHistory;
    private final HashMap<String, String> giftedMembershipIdMap;
    private final Set<HistoryChangedListener> listeners;
    private final Supplier<DatabaseManager> dbManager;

    public EventHistory(@NotNull Supplier<DatabaseManager> dbManager) {
        this.dbManager = dbManager;
        this.executionHistory = new Stack<>();
        this.giftedMembershipIdMap = new HashMap<>();
        this.listeners = new HashSet<>();
    }

    public void store(StreamlabsEvent event, PluginConfig config, JsonObject baseObject, boolean isTest) {
        LoggedEvent newEvent = new LoggedEvent(event, config, baseObject, new Date().getTime());
        if (!isTest) this.dbManager.get().logEvent(event, baseObject);
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
            return AbstractPlaceholder.replacePlaceholders(originalString, createContext());
        }

        public ActionExecutionContext createContext() {
            return new ActionExecutionContext(event, null, config, null, baseObject);
        }
    }
}
