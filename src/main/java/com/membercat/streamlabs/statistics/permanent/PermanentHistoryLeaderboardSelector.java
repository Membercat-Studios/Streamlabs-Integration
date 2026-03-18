package com.membercat.streamlabs.statistics.permanent;

import com.membercat.streamlabs.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class PermanentHistoryLeaderboardSelector extends PermanentHistorySelector<String> {
    private final int place;

    protected PermanentHistoryLeaderboardSelector(int place, @Nullable Instant start) {
        super(start);
        this.place = place;
    }

    protected @Nullable String queryDatabase(@NotNull DatabaseManager dbManager, @Nullable Instant end) {
        try {
            //TODO: Add proper DB leaderboard caching
            return dbManager.queryHistoryLeaderboard(this.start, end).get(this.place);
        } catch (IndexOutOfBoundsException e) {
            return "---";
        }
    }

    static @NotNull PermanentHistoryLeaderboardSelector deserialize(@NotNull String input, @Nullable Instant start) throws IllegalArgumentException {
        try {
            int place = Integer.parseInt(input) - 1;
            if (place < 0) throw new IllegalArgumentException("Place number out of range");
            return new PermanentHistoryLeaderboardSelector(place, start);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid place number");
        }
    }
}
