package me.Domplanto.streamLabs.statistics;

import org.jetbrains.annotations.NotNull;

public interface EventFilter {
    boolean check(@NotNull EventHistory.LoggedEvent event);

    boolean checkRelativeId(int id);
}
