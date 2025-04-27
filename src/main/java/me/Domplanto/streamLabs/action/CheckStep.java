package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
@ReflectUtil.ClassId("check")
public class CheckStep extends ConditionGroup implements StepBase<List> {
    private List<? extends StepBase<?>> steps = new ArrayList<>();
    @YamlProperty("else")
    private List<? extends StepBase<?>> elseSteps = new ArrayList<>();

    @Override
    public @NotNull Class<List> getExpectedDataType() {
        return List.class;
    }

    @Override
    public void load(@NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.steps = AbstractLogicStep.loadSteps(data, getExpectedDataType(), issueHelper, parent);
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException {
        List<? extends StepBase<?>> steps = this.check(ctx) ? this.steps : this.elseSteps;
        ctx.runSteps(steps, plugin);
    }

    @YamlPropertyCustomDeserializer(propertyName = "else")
    public List<? extends StepBase<?>> deserializeElseSteps(List<?> list, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return AbstractLogicStep.loadSteps(list, getExpectedDataType(), issueHelper, parent);
    }
}
