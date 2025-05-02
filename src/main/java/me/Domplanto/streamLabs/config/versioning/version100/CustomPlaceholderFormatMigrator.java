package me.Domplanto.streamLabs.config.versioning.version100;

import me.Domplanto.streamLabs.config.versioning.ConfigMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.Domplanto.streamLabs.config.versioning.version100.StepsMigrator.getSubSections;

@SuppressWarnings("unused")
public class CustomPlaceholderFormatMigrator implements ConfigMigrator {
    private static final String OLD_FORMAT = "\\{%s\\}";
    private static final String NEW_FORMAT = "\\{!%s\\}";
    private Map<String, String> replacements;

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public long getVersion() {
        return 100;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        ConfigurationSection customPlaceholders = root.getConfigurationSection("custom_placeholders");
        if (customPlaceholders == null) return;

        replacements = getSubSections(customPlaceholders).stream()
                .map(ConfigurationSection::getName)
                .collect(Collectors.toMap(OLD_FORMAT::formatted, NEW_FORMAT::formatted));
        this.replaceRecursive(root);
    }

    private void replaceRecursive(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            if (section.isString(key)) {
                section.set(key, replace(section.getString(key)));
                continue;
            }

            if (section.isConfigurationSection(key)) {
                this.replaceRecursive(Objects.requireNonNull(section.getConfigurationSection(key)));
                continue;
            }

            if (!section.isList(key)) continue;
            List<?> list = section.getList(key, List.of());
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                //noinspection unchecked
                ((List<Object>) list).set(list.indexOf(o), replace(s));
            }
            section.set(key, list);
        }
    }

    private String replace(String input) {
        for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
            input = input.replaceAll(entry.getKey(), entry.getValue());
        }
        return input;
    }
}
