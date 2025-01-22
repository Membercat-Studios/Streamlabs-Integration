package me.Domplanto.streamLabs.config.issue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
        this.pathStack.push(new ConfigPathStack.Entry(segment, annotation, name));
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

    public void appendAtPath(ConfigIssue issue) {
        this.issues.add(new PathSpecificConfigIssue(issue, this.pathStack.clone()));
    }

    public void appendAtPathAndLog(ConfigIssue issue, Throwable throwable) {
        this.issues.add(new PathSpecificConfigIssue(issue, this.pathStack.clone()));
        this.logger.log(issue.getLevel().getLogLevel(), "Detailed exception for config issue \"%s\" at %s:".formatted(issue.getDescription(), pathStack.toFormattedString()), throwable);
    }
}
