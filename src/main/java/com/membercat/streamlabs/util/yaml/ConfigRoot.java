package com.membercat.streamlabs.util.yaml;

import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.config.versioning.ConfigMigrationManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import static com.membercat.streamlabs.config.issue.Issues.*;

public abstract class ConfigRoot implements YamlPropertyObject {
    private final ConfigIssueHelper issueHelper;
    private boolean loaded = false;

    public ConfigRoot(ComponentLogger logger) {
        this.issueHelper = new ConfigIssueHelper(logger);
    }

    public final void load(@NotNull File configFile) throws ConfigLoadedWithIssuesException {
        this.issueHelper.initialize();
        YamlConfiguration config = new YamlConfiguration();
        boolean loadSucceeded = false;
        try {
            config.load(configFile);
            try {
                new ConfigMigrationManager(config).checkAndMigrate(configFile, issueHelper);
                loadSucceeded = true;
                this.loaded = true;
                this.acceptYamlProperties(config, this.issueHelper);
                this.customLoad(config);
            } catch (ConfigMigrationManager.MigrationFailureException ignore) {
            } catch (Exception e) {
                this.issueHelper.appendAtPathAndLog(EM0, e);
            }
        } catch (IOException e) {
            this.issueHelper.appendAtPathAndLog(EL3, e);
        } catch (InvalidConfigurationException e) {
            this.issueHelper.appendAtPath(EL2.apply(e.getMessage()));
        }

        if (!loadSucceeded) this.issueHelper.appendAtPath(loaded ? EL1 : EL0);
        this.issueHelper.complete();
    }

    public abstract void customLoad(@NotNull FileConfiguration config);

    public ConfigIssueHelper issueHelper() {
        return this.issueHelper;
    }
}
