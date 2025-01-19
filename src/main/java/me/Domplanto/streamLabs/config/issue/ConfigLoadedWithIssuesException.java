package me.Domplanto.streamLabs.config.issue;

import java.util.List;

public class ConfigLoadedWithIssuesException extends Throwable {
    private final List<ConfigIssue> issues;

    public ConfigLoadedWithIssuesException(List<ConfigIssue> issues) {
        super("The configuration was loaded, but contains issues preventing certain parts from loading correctly.");
        this.issues = issues;
    }

    public List<ConfigIssue> getIssues() {
        return issues;
    }
}
