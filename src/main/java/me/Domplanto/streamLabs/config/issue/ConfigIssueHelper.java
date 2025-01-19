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

    public void appendAtPath(ConfigIssue.Level level, String description) {
        this.issues.add(new PathSpecificConfigIssue(level, this.pathStack.clone(), description));
    }

    public void appendAtPathAndLog(ConfigIssue.Level level, String description, Throwable throwable) {
        this.issues.add(new PathSpecificConfigIssue(level, this.pathStack.clone(), description));
        this.logger.log(level.getLogLevel(), "Detailed exception for config issue \"%s\" at %s:".formatted(description, pathStack.toFormattedString()), throwable);
    }
}
