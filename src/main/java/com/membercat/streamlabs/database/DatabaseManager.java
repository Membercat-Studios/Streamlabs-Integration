package com.membercat.streamlabs.database;

import com.membercat.streamlabs.database.provider.DatabaseProvider;
import org.jetbrains.annotations.NotNull;

import static com.membercat.streamlabs.StreamlabsIntegration.LOGGER;

public class DatabaseManager extends StatementExecutor {
    private static final int SCHEMA_VERSION = 100;

    public DatabaseManager(@NotNull DatabaseProvider provider) {
        super(provider);
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

    private void seedDatabase() {
        LOGGER.info("Seeding database \"%s\"...".formatted(provider().getDisplayName()));
        this.update("INSERT INTO _meta VALUES ('SCHEMA_VERSION', ?)", s -> s.setInt(1, SCHEMA_VERSION));
    }
}
