package com.membercat.streamlabs.statistics.permanent;

import com.membercat.streamlabs.database.DatabaseManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public abstract class PermanentHistorySelector<T> {
    protected final @Nullable Instant start;

    protected PermanentHistorySelector(@Nullable Instant start) {
        this.start = start;
    }

    public final @Nullable T queryDatabase(@NotNull DatabaseManager dbManager) {
        return this.queryDatabase(dbManager, this.start != null ? Instant.now() : null);
    }

    protected abstract @Nullable T queryDatabase(@NotNull DatabaseManager dbManager, @Nullable Instant end);

    public static @NotNull PermanentHistorySelector<?> deserialize(@NotNull String input) throws IllegalArgumentException {
        int bracketIdx = input.indexOf('[');
        Instant start = null;
        int endBracketIdx = input.lastIndexOf(']');
        if (bracketIdx != -1 && endBracketIdx != -1) {
            String timeSelector = input.substring(bracketIdx + 1, endBracketIdx);
            Pair<ChronoUnit, Integer> result = parseTimeSelector(timeSelector);
            if (result.getKey().ordinal() > ChronoUnit.YEARS.ordinal())
                throw new IllegalArgumentException("Time selector unit out of range");
            LocalDateTime date = LocalDateTime.now(ZoneId.of("UTC"));
            start = date.minus(result.getValue(), result.getKey()).toInstant(ZoneOffset.UTC);
        } else if (bracketIdx != -1) throw new IllegalArgumentException("Missing closing bracket in time selector");

        String eventSelector = input.substring(0, bracketIdx == -1 ? input.length() : bracketIdx);
        int cn = eventSelector.indexOf(':');
        if (cn == -1) throw new IllegalArgumentException("Missing statistic type");
        String type = eventSelector.substring(0, cn);
        String actualSelector = eventSelector.substring(Math.min(cn + 1, eventSelector.length()));
        return switch (type) {
            case "ec" -> PermanentHistoryEventSelector.deserialize(actualSelector, start);
            case "lb" -> PermanentHistoryLeaderboardSelector.deserialize(actualSelector, start);
            default -> throw new IllegalArgumentException("Unknown statistic type");
        };
    }

    private static @NotNull Pair<ChronoUnit, Integer> parseTimeSelector(@NotNull String timeSelector) throws IllegalArgumentException {
        String[] parts = timeSelector.split("=");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid time selector (may be missing amount)");
        int amount;
        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time selector amount (not a number)");
        }
        if (amount > 400)
            throw new IllegalArgumentException("Time selector amount out of range (consider using a different time unit)");
        ChronoUnit unit;
        try {
            unit = ChronoUnit.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid time selector unit \"%s\"".formatted(parts[0]));
        }

        return Pair.of(unit, amount);
    }
}