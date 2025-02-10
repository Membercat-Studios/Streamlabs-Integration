package me.Domplanto.streamLabs.config.issue;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static me.Domplanto.streamLabs.config.issue.Issues.EI1;

public class ConfigIssueHelper {
    private final List<ConfigIssue> issues;
    private final ConfigPathStack pathStack;
    private final Logger logger;

    public ConfigIssueHelper(Logger logger) {
        this.issues = new ArrayList<>();
        this.pathStack = new ConfigPathStack();
        this.logger = logger;
    }

    public void reset() {
        this.issues.clear();
        this.pathStack.clear();
    }

    public void complete() throws ConfigLoadedWithIssuesException {
        if (!pathStack.empty()) {
            this.reset();
            this.appendAtPathAndLog(EI1, new IllegalStateException("Path stack not empty"));
        }

        if (!this.issues.isEmpty())
            throw new ConfigLoadedWithIssuesException(this.issues);
    }

    public void pushSection(String name) {
        this.push(ConfigPathStack.Section.class, name);
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

    public void appendAtPath(ConfigIssue issue) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(new PathSpecificConfigIssue(issue, this.stackCopy()));
    }

    public void appendAtPathAndLog(ConfigIssue issue, Throwable throwable) {
        if (checkIssueSuppressed(issue)) return;
        this.issues.add(new PathSpecificConfigIssue(issue, this.stackCopy()));
        this.logger.log(issue.getLevel().getLogLevel(), "Detailed exception for config issue \"%s\" at %s:".formatted(issue.getDescription(), pathStack.toFormattedString()), throwable);
    }

    private boolean checkIssueSuppressed(ConfigIssue issue) {
        return !this.pathStack.isEmpty() && this.pathStack.peek().suppressedIssues().contains(issue.getId());
    }

    public @NotNull ConfigPathStack stackCopy() {
        return this.pathStack.clone();
    }
}
