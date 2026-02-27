package com.membercat.streamlabs.database;

import com.membercat.streamlabs.database.provider.DatabaseProvider;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    protected <T> @Nullable T runPrepared(@NotNull @Language("sql") String sql, @Nullable FailableConsumer<PreparedStatement, SQLException> paramActions, @NotNull FailableFunction<PreparedStatement, T, SQLException> executor) throws SQLException {
        if (!this.ensureConnection()) return null;
        PreparedStatement statement = this.connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        if (paramActions != null) paramActions.accept(statement);
        statement.closeOnCompletion();
        return executor.apply(statement);
    }

    protected <T> @Nullable T queryOne(@NotNull @Language("sql") String sql, @Nullable FailableConsumer<PreparedStatement, SQLException> paramActions, @NotNull String column, @NotNull Class<T> cls) {
        try (ResultSet resultSet = this.query(sql, paramActions)) {
            if (resultSet == null) return null;
            return resultSet.getObject(column, cls);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to perform SQL data conversion", e);
        }
    }

    protected @Nullable ResultSet query(@NotNull @Language("sql") String sql, @Nullable FailableConsumer<PreparedStatement, SQLException> paramActions) {
        try {
            ResultSet resultSet = this.runPrepared(sql, paramActions, PreparedStatement::executeQuery);
            if (resultSet == null) return null;
            if (!resultSet.next()) {
                resultSet.close();
                return null;
            }
            return resultSet;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to query the database", e);
            return null;
        }
    }

    protected @Nullable Integer update(@NotNull @Language("sql") String sql, @Nullable FailableConsumer<PreparedStatement, SQLException> paramActions) {
        try {
            return this.runPrepared(sql, paramActions, PreparedStatement::executeUpdate);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to apply changes to database", e);
            return null;
        }
    }

    private boolean ensureConnection() {
        if (this.connection != null) return true;
        LOGGER.info("Connecting to database \"%s\"...".formatted(this.provider.getDisplayName()));
        try {
            this.connection = this.provider.createConnection();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to database \"%s\"", e);
            return false;
        }
    }

    protected @NotNull DatabaseProvider provider() {
        return this.provider;
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
