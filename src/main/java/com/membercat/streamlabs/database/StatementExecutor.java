package com.membercat.streamlabs.database;

import com.membercat.streamlabs.database.provider.DatabaseProvider;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import static com.membercat.streamlabs.StreamlabsIntegration.LOGGER;

public abstract class StatementExecutor {
    private final DatabaseProvider provider;
    private Connection connection;

    protected StatementExecutor(@NotNull DatabaseProvider provider) {
        this.provider = provider;
    }

    public void init() {
        this.ensureConnection();
    }

    private void ensureConnection() {
        if (this.connection != null) return;
        LOGGER.info("Connecting to database \"%s\"...".formatted(this.provider.getDisplayName()));
        try {
            this.connection = this.provider.createConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        LOGGER.info("Closing database connection...");
        try {
            if (this.connection != null) this.connection.close();
            this.connection = null;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to close database connection to %s".formatted(this.provider.getDisplayName()), e);
        }
    }
}
