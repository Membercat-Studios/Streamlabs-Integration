package me.Domplanto.streamLabs.config.issue;

import java.util.*;
import java.util.logging.Logger;

public class ConfigIssueHelper {
    private final List<ConfigIssue> issues;
    private final ConfigPathStack pathStack;
    private final Logger logger;
    private final Set<String> globalSuppressions;

    public ConfigIssueHelper(Logger logger) {
        this.issues = new ArrayList<>();
        this.globalSuppressions = new HashSet<>();
        this.pathStack = new ConfigPathStack();
        this.logger = logger;
    }

    public void reset() {
        this.issues.clear();
        this.pathStack.clear();
        this.globalSuppressions.clear();
    }

    public void complete() throws ConfigLoadedWithIssuesException {
        if (!pathStack.empty())
            throw new IllegalStateException("Path stack not empty");

        if (!this.issues.isEmpty())
            throw new ConfigLoadedWithIssuesException(this.issues);
    }

    public void pushSection(String name) {
        this.push(ConfigPathStack.Section.class, name);
    }

    public void newSection(String name) {
        this.popIfSection();
        this.pushSection(name);
    }

    public void pushProperty(String name) {
        this.push(ConfigPathStack.Property.class, name);
    }

    public void push(Class<?> segment, String name) {
        ConfigPathSegment annotation = segment.getAnnotation(ConfigPathSegment.class);
        this.pathStack.push(new ConfigPathStack.Entry(segment, annotation, name,
                !this.pathStack.empty() ? this.pathStack.peek().suppressedIssues() : new HashSet<>()));
    }

    public void pop() {
        this.pathStack.pop();
    }

    public void popIfSection() {
        if (!this.pathStack.isEmpty() && this.pathStack.peek().isSection())
            this.pathStack.pop();
    }

    public void popIfProperty() {
        if (!this.pathStack.isEmpty() && this.pathStack.peek().isProperty())
            this.pathStack.pop();
    }

    public void suppress(Collection<String> issueIds) {
        if (this.pathStack.isEmpty()) return;
        this.pathStack.peek().suppress(issueIds);
    }

    public void suppressGlobally(Collection<String> issueIds) {
        this.globalSuppressions.addAll(issueIds);
    }

    public void appendAtPath(ConfigIssue issue) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(new PathSpecificConfigIssue(issue, this.pathStack.clone()));
    }

    public void appendAtPathAndLog(ConfigIssue issue, Throwable throwable) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(new PathSpecificConfigIssue(issue, this.pathStack.clone()));
        this.logger.log(issue.getLevel().getLogLevel(), "Detailed exception for config issue \"%s\" at %s:".formatted(issue.getDescription(), pathStack.toFormattedString()), throwable);
    }

    private boolean checkIssueSuppressed(ConfigIssue issue) {
        return this.globalSuppressions.contains(issue.getId()) || (!this.pathStack.isEmpty() && this.pathStack.peek().suppressedIssues().contains(issue.getId()));
    }
}
