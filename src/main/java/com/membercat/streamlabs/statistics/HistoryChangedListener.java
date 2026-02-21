package com.membercat.streamlabs.statistics;

public interface HistoryChangedListener {
    void onHistoryChanged(EventHistory history, EventHistory.LoggedEvent newEvent);
}
