package com.membercat.streamlabs.statistics.permanent;

import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public class PermanentHistoryEventSelector extends PermanentHistorySelector<Integer> {
    private final boolean onlyDonations;
    private final @Nullable Set<String> events;

    protected PermanentHistoryEventSelector(boolean onlyDonations, @Nullable Set<String> events, @Nullable Instant start) {
        super(start);
        this.onlyDonations = onlyDonations;
        this.events = events;
    }

    protected @Nullable Integer queryDatabase(@NotNull DatabaseManager dbManager, @Nullable Instant end) {
        return dbManager.queryHistoryCount(this.events, this.onlyDonations, this.start, end);
    }

    static @NotNull PermanentHistoryEventSelector deserialize(@NotNull String input, @Nullable Instant start) throws IllegalArgumentException {
        ConfigIssueHelper issueHelper = new ConfigIssueHelper(null);
        boolean donations = input.equals("donations");
        Set<String> events = null;
        if (!donations && !input.equals("all"))
            events = PluginConfig.Action.parseEventTypes(input, issueHelper);
        try {
            issueHelper.complete();
        } catch (ConfigLoadedWithIssuesException e) {
            throw new IllegalArgumentException("Invalid event type(s) specified");
        }
        return new PermanentHistoryEventSelector(donations, events, start);
    }
}
