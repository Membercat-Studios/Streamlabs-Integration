package com.membercat.streamlabs.config.versioning.version101;

import com.membercat.streamlabs.config.versioning.ConfigMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

import static com.membercat.streamlabs.config.versioning.version101.StepIterationUtil.applyForStep;
import static com.membercat.streamlabs.config.versioning.version101.StepIterationUtil.replaceStr;

@SuppressWarnings("unused")
public class NamedCollectionReworkMigrator implements ConfigMigrator {
    @Override
    public long getVersion() {
        return 101;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void apply(@NotNull ConfigurationSection root, long targetVersion) {
        applyForStep(root, "extract", s -> replaceWithFilter(s, "extract"));
        applyForStep(root, "random_element", s -> replaceWithFilter(s, "random_element"));
        applyForStep(root, "bulk_random_elements", s -> {
            replaceWithFilter(s, "collection");
            Stream<StepIterationUtil.StepResult> result = StepIterationUtil.StepResult.stream(s.get("bulk_random_elements"));
            result.flatMap(StepIterationUtil::withSubStepsRecursive).map(StepIterationUtil.StepResult::step)
                    .forEach(step -> replaceStr(step, str -> {
                        str = str.replace("{$element_name}", "{$element.display_name_safe}");
                        return str.replace("{$element_id}", "{$element}");
                    }));
        });
    }

    private void replaceWithFilter(@NotNull Map<String, Object> step, @NotNull String property) {
        if (!step.containsKey(property)) return;
        String value = step.get(property).toString();
        step.put(property, switch (value.toLowerCase()) {
            case "none" -> "empty";
            case "living_entity_type" -> "entity_type[{alive}=true]";
            case "non_air_block" -> "block";
            case "solid_block" -> "block[{solid}=true|{occluding}=true]";
            default -> value;
        });
    }
}
