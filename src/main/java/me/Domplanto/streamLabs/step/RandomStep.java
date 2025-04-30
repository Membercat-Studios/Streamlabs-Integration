package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ReflectUtil.ClassId("random")
public class RandomStep extends AbstractLogicStep {
    @YamlProperty("seed")
    private int seed;
    private Random random;

    @Override
    public void load(@SuppressWarnings("rawtypes") @NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    public @NotNull Collection<? extends StepBase<?>> getSteps(ActionExecutionContext ctx) {
        if (steps().isEmpty()) return List.of();
        int step = this.random.nextInt(0, steps().size());
        return List.of(steps().get(step));
    }
}
