package me.Domplanto.streamLabs.statistics.goal;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;

@ConfigPathSegment(id = "donation_goal")
public class DonationGoal extends PluginConfig.AbstractAction {
    private double value = 0;
    private double max = 0;
    private boolean active;
    @YamlProperty("add_amount")
    private String addAmount = String.valueOf(1);

    public boolean add(ActionExecutionContext ctx) {
        if (!this.active || !this.check(ctx)) return false;
        String expression = ActionPlaceholder.replacePlaceholders(this.addAmount, ctx);
        this.value += new DoubleEvaluator().evaluate(expression);

        return this.value >= this.max;
    }

    public DonationGoal start(int max) {
        this.reset();
        this.max = max;
        this.active = true;
        return this;
    }
    public void disable() {
        this.active = false;
    }

    public void reset() {
        this.value = 0;
        this.max = 0;
        this.active = false;
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        return (rateLimiter == null || rateLimiter.check(ctx)) && super.check(ctx);
    }

    public double getValue() {
        return this.value;
    }

    public double getGoal() {
        return max;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "DonationGoal{" +
                "id='" + id + '\'' +
                ", addAmount='" + addAmount + '\'' +
                ", active=" + active +
                ", max=" + max +
                ", value=" + value +
                '}';
    }
}
