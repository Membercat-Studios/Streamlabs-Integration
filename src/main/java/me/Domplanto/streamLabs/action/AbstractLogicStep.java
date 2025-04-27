package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.Domplanto.streamLabs.config.issue.Issues.WS2;

@SuppressWarnings("rawtypes")
public abstract class AbstractLogicStep extends AbstractStep<List> {
    private List<? extends AbstractStep<?>> steps = new ArrayList<>();

    public AbstractLogicStep() {
        super(List.class);
    }

    @Override
    public void load(@NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        try {
            //noinspection unchecked
            this.steps = AbstractStep.parseAll((List<Object>) data, parent, issueHelper);
        } catch (ClassCastException e) {
            this.steps = new ArrayList<>();
            issueHelper.appendAtPath(WS2(getExpectedDataType(), data));
        }
    }

    public List<? extends AbstractStep<?>> steps() {
        return this.steps;
    }
}
