package com.membercat.streamlabs.statistics.permanent;

import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.database.DatabaseManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

public class PermanentHistorySelector {
    private final boolean onlyDonations;
    private final @Nullable Set<String> events;
    private final @Nullable Instant start;

    private PermanentHistorySelector(boolean onlyDonations, @Nullable Set<String> events, @Nullable Instant start) {
        this.onlyDonations = onlyDonations;
        this.events = events;
        this.start = start;
    }

    public @Nullable Integer queryDatabase(@NotNull DatabaseManager dbManager) {
        return dbManager.queryHistoryCount(this.events, this.onlyDonations, this.start, this.start != null ? Instant.now() : null);
    }
    
    public static @NotNull PermanentHistorySelector deserialize(@NotNull String input) throws IllegalArgumentException {
        ConfigIssueHelper issueHelper = new ConfigIssueHelper(null);
        int bracketIdx = input.indexOf('[');
        String eventSelector = input.substring(0, bracketIdx == -1 ? input.length() : bracketIdx);
        boolean donations = eventSelector.equals("donations");
        Set<String> events = null;
        if (!donations && !eventSelector.equals("all"))
            events = PluginConfig.Action.parseEventTypes(eventSelector, issueHelper);
        try {
            issueHelper.complete();
        } catch (ConfigLoadedWithIssuesException e) {
            throw new IllegalArgumentException("Invalid event type(s) specified");
        }

        Instant start = null;
        Instant now = Instant.now();
        int endBracketIdx = input.lastIndexOf(']');
        if (bracketIdx != -1 && endBracketIdx != -1) {
            String timeSelector = input.substring(bracketIdx + 1, endBracketIdx);
            Pair<ChronoUnit, Integer> result = parseTimeSelector(timeSelector);
            if (result.getKey().ordinal() > ChronoUnit.YEARS.ordinal())
                throw new IllegalArgumentException("Time selector unit out of range");
            start = now.minus(result.getValue(), result.getKey());
        } else if (bracketIdx != -1) throw new IllegalArgumentException("Missing closing bracket in time selector");

        return new PermanentHistorySelector(donations, events, start);
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