package com.membercat.streamlabs.action;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathStack;
import com.membercat.streamlabs.step.query.CommandQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.membercat.streamlabs.config.issue.Issues.WPS0;

public class PlayerSelector {
    private @Nullable String selector;
    private @Nullable ConfigPathStack location;
    private boolean isAffectedPlayers;
    private boolean containsPlaceholders;

    private PlayerSelector(@Nullable String selector, boolean containsPlaceholders, @NotNull ConfigPathStack location) {
        this(selector, containsPlaceholders);
        this.location = location;
    }

    private PlayerSelector(@Nullable String selector, boolean containsPlaceholders) {
        this.selector = selector;
        this.containsPlaceholders = containsPlaceholders;
    }

    private PlayerSelector(boolean isAffectedPlayers) {
        this.isAffectedPlayers = isAffectedPlayers;
    }

    public @NotNull List<Player> resolve(@NotNull ActionExecutionContext ctx, @NotNull StreamlabsIntegration plugin) {
        if (!Bukkit.isPrimaryThread())
            throw new WrongThreadException("Attempted to resolve player selector on non-main thread");
        try {
            if (this.isAffectedPlayers) return ctx.config()
                    .getAffectedPlayers().stream()
                    .map(playerName -> plugin.getServer().getPlayerExact(playerName))
                    .filter(Objects::nonNull).toList();

            if (this.selector == null) return List.of();
            String selector = containsPlaceholders ? AbstractPlaceholder.replacePlaceholders(this.selector, ctx) : this.selector;
            AtomicInteger invalidEntities = new AtomicInteger();
            List<Player> results = List.of();
            try {
                results = PlayerSelector.select(selector)
                        .stream().distinct()
                        .filter(e -> {
                            boolean valid = e instanceof Player;
                            if (!valid) invalidEntities.getAndIncrement();
                            return valid;
                        }).map(e -> (Player) e).toList();
            } catch (IllegalArgumentException e) {
                StreamlabsIntegration.LOGGER.warning("Failed to resolve %s: %s".formatted(getName(), e.getMessage()));
            }

            if (invalidEntities.get() > 0)
                StreamlabsIntegration.LOGGER.warning("%s returned %s non-player entities, consider using @a[options...]".formatted(getName(), invalidEntities));
            return results;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to resolve %s".formatted(getName()), e);
        }
    }

    public @NotNull String getName() {
        String selectorName = !isAffectedPlayers ? Objects.requireNonNullElse(this.selector, "(None)") : "(Affected players)";
        return Optional.ofNullable(location)
                .map(l -> "Player Selector \"%s\" at %s".formatted(selectorName, l.toFormattedString()))
                .orElse("Player selector \"%s\"".formatted(selectorName));
    }

    public static @NotNull PlayerSelector ofAffectedPlayers() {
        return new PlayerSelector(true);
    }

    public static @NotNull PlayerSelector none() {
        return new PlayerSelector(null, false);
    }

    public static @NotNull PlayerSelector parse(@NotNull String input, ConfigIssueHelper issueHelper) {
        if (Pattern.compile(CommandQuery.PLAYER_PLACEHOLDER).matcher(input).find())
            return PlayerSelector.ofAffectedPlayers();
        boolean placeholders = input.startsWith("[p]");
        if (placeholders) input = input.substring(2);

        try {
            if (!placeholders) PlayerSelector.select(input);
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WPS0.apply(e.getMessage()));
            return PlayerSelector.none();
        }
        return new PlayerSelector(input, placeholders, issueHelper.stackCopy());
    }

    private static @NotNull List<Entity> select(@NotNull String selector) throws IllegalArgumentException {
        try {
            return Bukkit.selectEntities(Bukkit.getConsoleSender(), selector);
        } catch (IllegalArgumentException e) {
            if (!(e.getCause() instanceof CommandSyntaxException ce))
                throw new RuntimeException("Unexpected internal error while parsing selector", Objects.requireNonNull(e.getCause()));
            throw new IllegalArgumentException(ce.getMessage());
        }
    }
}
