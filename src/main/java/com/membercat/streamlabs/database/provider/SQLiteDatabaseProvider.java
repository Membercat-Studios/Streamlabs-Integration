package com.membercat.streamlabs.database.provider;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabaseProvider implements DatabaseProvider {
    private final Path path;

    public SQLiteDatabaseProvider(@NotNull Path path) throws IllegalStateException {
        this.path = path;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not SQLite JDBC driver in classpath", e);
        }
    }

    @Override
    public @NotNull Connection createConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:%s".formatted(this.path.toAbsolutePath().toString()));
    }

    @Override
    public @NotNull String getDisplayName() {
        String fileName = this.path.getFileName().toString();
        return "SQLite (%s)".formatted(fileName);
    }
}
