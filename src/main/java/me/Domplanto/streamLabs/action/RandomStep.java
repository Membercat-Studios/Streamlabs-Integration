package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static me.Domplanto.streamLabs.config.issue.Issues.WS2;

@ReflectUtil.ClassId("random")
@SuppressWarnings("rawtypes")
public class RandomStep extends AbstractStep<List> {
    private List<? extends AbstractStep<?>> steps = new ArrayList<>();
    @YamlProperty("seed")
    private int seed;
    private Random random;

    public RandomStep() {
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

        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        if (steps.isEmpty()) return;
        int step = this.random.nextInt(0, steps.size());
        this.steps.get(step).execute(ctx, getPlugin());
    }
}
