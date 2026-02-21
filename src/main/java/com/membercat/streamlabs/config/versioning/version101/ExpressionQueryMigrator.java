package com.membercat.streamlabs.config.versioning.version101;

import com.membercat.streamlabs.config.versioning.ConfigMigrator;
import com.membercat.streamlabs.step.query.AbstractQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import static com.membercat.streamlabs.config.versioning.version101.StepIterationUtil.applyForAndAfterStep;
import static com.membercat.streamlabs.config.versioning.version101.StepIterationUtil.replaceStr;

@SuppressWarnings("unused")
public class ExpressionQueryMigrator implements ConfigMigrator {
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
        applyForAndAfterStep(root, "expression", step -> {
            if (step.containsKey("action")
                    && "GROUP".equalsIgnoreCase(String.valueOf(step.get("action")))
                    && step.containsKey("group")) {
                String group = step.get("group").toString();
                step.remove("group");
                step.put("action", "MATCH_GROUPS");
                return Pair.of(group, String.valueOf(step.get("output")));
            } else return null;
        }, (step, params) -> {
            String format = AbstractQuery.QueryPlaceholder.FORMAT.formatted(params.getRight());
            String newFormat = AbstractQuery.QueryPlaceholder.FORMAT.formatted("%s.%s".formatted(params.getRight(), params.getLeft()));
            replaceStr(step, str -> str.replaceAll(format, newFormat));
        });
    }
}
