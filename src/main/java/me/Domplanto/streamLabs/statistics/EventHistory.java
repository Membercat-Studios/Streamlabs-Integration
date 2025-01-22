package me.Domplanto.streamLabs.statistics;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Stack;

public class EventHistory {
    private final Stack<LoggedEvent> executionHistory;

    public EventHistory() {
        this.executionHistory = new Stack<>();
    }

    public void store(StreamlabsEvent event, PluginConfig config, JsonObject baseObject) {
        this.executionHistory.push(new LoggedEvent(event, config, baseObject, new Date().getTime()));
    }

    @Nullable
    public LoggedEvent getEvent(EventHistorySelector selector) {
        for (int i = 0; i < this.executionHistory.reversed().size(); i++) {
            LoggedEvent event = executionHistory.reversed().get(i);
            if (selector.check(i, event)) return event;
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
            return new ActionExecutionContext(event, config, null, baseObject);
        }
    }
}
