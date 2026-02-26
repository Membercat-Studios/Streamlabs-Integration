package com.membercat.streamlabs.database.provider;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathSegment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static com.membercat.streamlabs.config.issue.Issues.WDB0;

@ConfigPathSegment(id = "database_provider")
public interface DatabaseProvider {
    static @NotNull DatabaseProvider deserialize(@NotNull String input, @Nullable ConfigIssueHelper issueHelper) {
        if (!input.equalsIgnoreCase("sqlite") && issueHelper != null) issueHelper.appendAtPath(WDB0.apply(input));
        File file = StreamlabsIntegration.dataPath().resolve("_data").toFile();
        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();
        Path dbFile = file.toPath().resolve("history.sqlite");
        return new SQLiteDatabaseProvider(dbFile);
    }

    @NotNull Connection createConnection() throws SQLException;

    @NotNull String getDisplayName();
}
