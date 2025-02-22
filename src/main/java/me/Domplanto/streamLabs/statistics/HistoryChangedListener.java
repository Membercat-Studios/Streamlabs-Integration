package me.Domplanto.streamLabs.statistics;

public interface HistoryChangedListener {
    void onHistoryChanged(EventHistory history, EventHistory.LoggedEvent newEvent);
}
