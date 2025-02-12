package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

public abstract class ConfigRoot implements YamlPropertyObject {
    private final ConfigIssueHelper issueHelper;
    private boolean loaded = false;

    public ConfigRoot(Logger logger) {
        this.issueHelper = new ConfigIssueHelper(logger);
    }

    public final void load(@NotNull File configFile) throws ConfigLoadedWithIssuesException {
        this.issueHelper.reset();
        YamlConfiguration config = new YamlConfiguration();
        boolean loadSucceeded = false;
        try {
            config.load(configFile);
            loadSucceeded = true;
            this.loaded = true;
            this.acceptYamlProperties(config, this.issueHelper);
            this.customLoad(config);
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
