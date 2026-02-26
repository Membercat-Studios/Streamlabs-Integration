package com.membercat.streamlabs.database;

import com.membercat.streamlabs.database.provider.DatabaseProvider;
import org.jetbrains.annotations.NotNull;

public class DatabaseManager extends StatementExecutor {
    public DatabaseManager(@NotNull DatabaseProvider provider) {
        super(provider);
    }
}
