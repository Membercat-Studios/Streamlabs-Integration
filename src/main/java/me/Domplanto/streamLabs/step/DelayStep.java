package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static me.Domplanto.streamLabs.config.issue.Issues.WD0;

@ReflectUtil.ClassId("delay")
public class DelayStep extends AbstractStep<String> {
    private static final String THREAD_WARNING_URL = Translations.wikiPage("/Misc.-Features-&-Guides#when-to-use-server_thread");
    private String delay;

    public DelayStep() {
        super(String.class);
    }

    @Override
    public @NotNull Set<Serializer<?, String>> getOptionalDataSerializers() {
        return Set.of(new Serializer<>(Integer.class, String.class, (i, helper) -> i.toString()));
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        try {
            long input = Long.parseLong(data);
            if (input < 1) {
                this.delay = "1000";
                issueHelper.appendAtPath(WD0.apply(data, this.delay));
                return;
            }
        } catch (NumberFormatException ignore) {
        }

        this.delay = data;
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        String parsed = AbstractPlaceholder.replacePlaceholders(this.delay, ctx);
        long delay;
        try {
            delay = Long.parseLong(parsed);
            if (delay < 1) {
                StreamLabs.LOGGER.warning("Zero or negative delay %sms (resolved from \"%s\") detected at %s, skipping!".formatted(delay, this.delay, getLocation().toFormattedString()));
                return;
            }
        } catch (NumberFormatException e) {
            StreamLabs.LOGGER.warning("Failed to parse delay \"%s\" (resolved from \"%s\") at %s, skipping!".formatted(parsed, this.delay, getLocation().toFormattedString()));
            return;
        }

        if (Bukkit.isPrimaryThread())
            StreamLabs.LOGGER.warning("Delay step at %s is running on the main server thread, this will freeze your server! Read more at %s".formatted(getLocation().toFormattedString(), THREAD_WARNING_URL));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignore) {
        }
    }
}
