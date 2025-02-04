package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public abstract class ConfigRoot implements YamlPropertyObject {
    private final ConfigIssueHelper issueHelper;

    public ConfigRoot(Logger logger) {
        this.issueHelper = new ConfigIssueHelper(logger);
    }

    public final void load(@NotNull FileConfiguration config) throws ConfigLoadedWithIssuesException {
        this.issueHelper.reset();
        this.acceptYamlProperties(config, this.issueHelper);
        this.customLoad(config);
        this.issueHelper.complete();
    }

    public abstract void customLoad(@NotNull FileConfiguration config);

    public ConfigIssueHelper issueHelper() {
        return this.issueHelper;
    }

    @Nullable
    public <T extends YamlPropertyObject> T createInstance(Class<T> type, ConfigurationSection section) {
        return YamlPropertyObject.createInstance(type, section, this.issueHelper);
    }
}
