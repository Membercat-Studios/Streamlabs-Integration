package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.StepExecutor;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
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
    public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException {
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
