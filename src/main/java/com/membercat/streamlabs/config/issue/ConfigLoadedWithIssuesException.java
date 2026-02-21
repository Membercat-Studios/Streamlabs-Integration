package com.membercat.streamlabs.config.issue;

public class ConfigLoadedWithIssuesException extends Throwable {
    private final ConfigIssueHelper.IssueList issues;

    public ConfigLoadedWithIssuesException(ConfigIssueHelper.IssueList issues) {
        super("The configuration was loaded, but contains issues preventing certain parts from loading correctly.");
        this.issues = issues;
    }

    public ConfigIssueHelper.IssueList getIssues() {
        return issues;
    }
}
