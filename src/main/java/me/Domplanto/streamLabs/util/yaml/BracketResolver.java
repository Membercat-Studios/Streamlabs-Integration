package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static me.Domplanto.streamLabs.config.issue.Issues.HB0;

public class BracketResolver {
    @NotNull
    private String content;
    @Nullable
    private String bracketContents;

    public BracketResolver(@NotNull String input) {
        this.content = Objects.requireNonNull(input);
        this.bracketContents = null;
    }

    public BracketResolver resolve(ConfigIssueHelper issueHelper) {
        int closeBracketIdx = content.indexOf(']');
        if (content.startsWith("[") && closeBracketIdx == -1)
            issueHelper.appendAtPath(HB0);
        if (!content.startsWith("[") || closeBracketIdx == -1) return this;

        this.bracketContents = content.substring(1, closeBracketIdx);
        this.content = content.substring(closeBracketIdx + 1);
        return this;
    }

    public @NotNull String getContent() {
        return this.content;
    }

    public @NotNull Optional<String> getBracketContents() {
        return Optional.ofNullable(this.bracketContents);
    }
}
