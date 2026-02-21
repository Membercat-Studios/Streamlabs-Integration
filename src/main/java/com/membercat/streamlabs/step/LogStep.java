package com.membercat.streamlabs.step;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.BracketResolver;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.membercat.streamlabs.config.issue.Issues.WL0;

@ReflectUtil.ClassId("log")
public class LogStep extends AbstractStep<String> {
    private String message;
    private Level logLevel;
    private Logger logger;
    @YamlProperty("prefix")
    private String prefix = "SL / Log Step";
    @YamlProperty("include_path")
    private boolean includePath = false;

    public LogStep() {
        super(String.class);
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.logger = Logger.getLogger(this.prefix);
        this.logLevel = Level.INFO;
        BracketResolver resolver = new BracketResolver(data).resolve(issueHelper);
        this.message = resolver.getContent();
        String levelStr = resolver.getBracketContents().map(String::toUpperCase).orElse("INFO");
        try {
            this.logLevel = Level.parse(levelStr);
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WL0.apply(levelStr, logLevel.getName()));
        }
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        String message = AbstractPlaceholder.replacePlaceholders(this.message, ctx);
        if (includePath) message += " (at %s)".formatted(getLocation().toFormattedString());
        this.logger.log(this.logLevel, message);
    }
}
