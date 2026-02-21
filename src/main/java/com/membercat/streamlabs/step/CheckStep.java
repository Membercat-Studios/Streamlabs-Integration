package com.membercat.streamlabs.step;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.action.StepExecutor;
import com.membercat.streamlabs.condition.ConditionGroup;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathStack;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import com.membercat.streamlabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("rawtypes")
@ReflectUtil.ClassId("check")
public class CheckStep extends ConditionGroup implements StepBase<List>, StepExecutor {
    private ConfigPathStack location;
    private List<? extends StepBase<?>> steps = new ArrayList<>();
    @YamlProperty("else")
    private List<? extends StepBase<?>> elseSteps = new ArrayList<>();

    @Override
    public @NotNull Class<List> getExpectedDataType() {
        return List.class;
    }

    @Override
    public void load(@NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.location = issueHelper.stackCopy();
        this.steps = AbstractLogicStep.loadSteps(data, getExpectedDataType(), issueHelper, parent);
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamlabsIntegration plugin) throws AbstractStep.ActionFailureException {
        this.runSteps(ctx, plugin);
    }

    @YamlPropertyCustomDeserializer(propertyName = "else")
    public List<? extends StepBase<?>> deserializeElseSteps(List<?> list, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return AbstractLogicStep.loadSteps(list, getExpectedDataType(), issueHelper, parent);
    }

    @Override
    public @NotNull String getName() {
        return "check step at %s".formatted(this.location.toFormattedString());
    }

    @Override
    public @NotNull Collection<? extends StepBase<?>> getSteps(ActionExecutionContext ctx) {
        return this.check(ctx) ? this.steps : this.elseSteps;
    }
}
