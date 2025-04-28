package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.Domplanto.streamLabs.config.issue.Issues.WPI2;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicStep extends AbstractStep<List> {
    private List<? extends StepBase> steps = new ArrayList<>();

    public AbstractLogicStep() {
        super(List.class);
    }

    protected static List<? extends StepBase> loadSteps(@NotNull List<?> data, Class<?> expected, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        try {
            //noinspection unchecked
            return AbstractStep.INITIALIZER.parseAll((List<Object>) data, parent, issueHelper);
        } catch (ClassCastException e) {
            issueHelper.appendAtPath(WPI2(AbstractStep.NAME_ID, expected, data));
            return new ArrayList<>();
        }
    }

    @Override
    public void load(@NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.steps = loadSteps(data, getExpectedDataType(), issueHelper, parent);
    }

    public List<? extends StepBase> steps() {
        return this.steps;
    }
}
