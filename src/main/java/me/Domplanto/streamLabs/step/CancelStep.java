package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.placeholder.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static me.Domplanto.streamLabs.config.issue.Issues.HCS0;

@ReflectUtil.ClassId("cancel")
public class CancelStep extends AbstractStep<String> {
    private String enable = Boolean.TRUE.toString();

    public CancelStep() {
        super(String.class);
    }

    @Override
    public @NotNull Set<Serializer<?, String>> getOptionalDataSerializers() {
        return Set.of(new Serializer<>(Boolean.class, String.class, (b, helper) -> b.toString()));
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        if (Boolean.FALSE.toString().equals(data.toLowerCase())) issueHelper.appendAtPath(HCS0);
        this.enable = data;
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        boolean execute = Boolean.parseBoolean(ActionPlaceholder.replacePlaceholders(this.enable, ctx));
        if (execute) ctx.stop();
    }
}
