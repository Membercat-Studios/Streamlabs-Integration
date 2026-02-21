package com.membercat.streamlabs.config.versioning;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import static com.membercat.streamlabs.config.issue.Issues.*;

public class ConfigMigrationManager {
    private static final long[] VERSIONS = {0, 100, 101};
    private static final long CONFIG_VERSION = VERSIONS[VERSIONS.length - 1];
    private final YamlConfiguration config;

    public ConfigMigrationManager(YamlConfiguration config) {
        this.config = config;
    }

    public void checkAndMigrate(File file, ConfigIssueHelper issueHelper) throws RuntimeException, IOException {
        issueHelper.process("version");
        long version = this.getVersion();
        if (version == CONFIG_VERSION) return;
        if (version > CONFIG_VERSION) {
            issueHelper.appendAtPath(EM1.apply(version, CONFIG_VERSION));
            throw new MigrationFailureException();
        }

        if (!ArrayUtils.contains(VERSIONS, version)) {
            issueHelper.appendAtPath(EM2.apply(version, CONFIG_VERSION));
            throw new MigrationFailureException();
        }

        StreamLabs.LOGGER.warning("Your configuration file was last used in a lower version of this plugin and will have to be migrated!");
        try {
            File newFile = getBackupFile(file.getParentFile());
            this.config.save(newFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create a backup of the current config, exiting!");
        }

        int i = ArrayUtils.indexOf(VERSIONS, version) + 1;
        try {
            while (i < VERSIONS.length) {
                this.runMigrators(VERSIONS[i]);
                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run migrators", e);
        }

        this.config.save(file);
        StreamLabs.LOGGER.info("Configuration file successfully migrated!");
        issueHelper.appendAtPath(HCM0);
    }

    private void runMigrators(long targetVersion) {
        StreamLabs.LOGGER.info("Attempting to migrate config from version %s to %s...".formatted(getVersion(), targetVersion));
        ConfigMigrator.MIGRATORS.stream()
                .filter(migrator -> migrator.shouldUse(targetVersion))
                .sorted(Comparator.comparing(ConfigMigrator::getPriority))
                .forEach(migrator -> migrator.apply(this.config, targetVersion));
    }

    public long getVersion() {
        return config.getLong("version", VERSIONS[0]);
    }

    private File getBackupFile(File directory) {
        return directory.toPath().resolve("config-v%s.backup.yml".formatted(getVersion())).toFile();
    }

    public static class MigrationFailureException extends RuntimeException {
    }
}
