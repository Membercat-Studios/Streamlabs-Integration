package com.membercat.streamlabs.database;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.database.provider.DatabaseProvider;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.Set;

import static com.membercat.streamlabs.StreamlabsIntegration.LOGGER;

public class DatabaseManager extends StatementExecutor {
    private static final int SCHEMA_VERSION = 100;
    private final boolean logEvents;
    private final @NotNull Set<String> ignoredEvents;

    public DatabaseManager(@NotNull DatabaseProvider provider, boolean logEvents, @NotNull Set<String> ignoredEvents) {
        super(provider);
        this.logEvents = logEvents;
        this.ignoredEvents = ignoredEvents;
    }

    @Override
    public void init() {
        super.init();
        this.update("CREATE TABLE IF NOT EXISTS _meta (key VARCHAR(255) PRIMARY KEY, value BLOB)", null);
        Integer version = this.queryOne("SELECT value FROM _meta WHERE key = 'SCHEMA_VERSION'", null, "value", Integer.class);
        // TODO: Support database migration in future updates
        if (version != null && version == SCHEMA_VERSION) return;
        if (version == null) this.seedDatabase();
        else LOGGER.severe("Found unknown database schema version %d, is the plugin up-to-date?");
    }

    public void logEvent(@NotNull StreamlabsEvent event, @NotNull JsonObject baseObject) {
        if (!this.logEvents) return;
        if (this.ignoredEvents.contains(event.getId())) {
            if (StreamlabsIntegration.isDebugMode())
                LOGGER.info("Event \"%s\" is set to be ignored, not writing to database!".formatted(event.getId()));
            return;
        }

        this.update("INSERT INTO event_history (type,related_user,donation_amount,donation_currency) VALUES (?, ?, ?, ?)", s -> {
            s.setString(1, event.getId());
            s.setString(2, event.getRelatedUser(baseObject));
            if (event instanceof BasicDonationEvent donationEvent) {
                s.setDouble(3, donationEvent.calculateAmount(baseObject));
                s.setString(4, donationEvent.getCurrency(baseObject));
            } else {
                s.setNull(3, Types.DOUBLE);
                s.setNull(4, Types.VARCHAR);
            }
        });
    }

    private void seedDatabase() {
        LOGGER.info("Seeding database \"%s\"...".formatted(provider().getDisplayName()));
        this.update("INSERT INTO _meta VALUES ('SCHEMA_VERSION', ?)", s -> s.setInt(1, SCHEMA_VERSION));
        this.update("CREATE TABLE event_history (type VARCHAR NOT NULL, related_user VARCHAR NOT NULL, donation_amount DOUBLE UNSIGNED, donation_currency VARCHAR(20), timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)", null);
    }
}
