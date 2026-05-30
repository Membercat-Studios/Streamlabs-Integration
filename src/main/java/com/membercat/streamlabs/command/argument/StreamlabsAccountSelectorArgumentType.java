package com.membercat.streamlabs.command.argument;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.command.exception.ComponentCommandExceptionType;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StreamlabsAccountSelectorArgumentType implements CustomArgumentType<Collection<PluginConfig.StreamlabsAccount>, String> {
    private static final ComponentCommandExceptionType ACCOUNT_NOT_FOUND = new ComponentCommandExceptionType("streamlabs.commands.error.unknown_account", ColorScheme.INVALID);
    private final @NotNull StreamlabsIntegration plugin;

    private StreamlabsAccountSelectorArgumentType(@NotNull StreamlabsIntegration plugin) {
        this.plugin = plugin;
    }

    public static StreamlabsAccountSelectorArgumentType accountSelector(@NotNull StreamlabsIntegration plugin) {
        return new StreamlabsAccountSelectorArgumentType(plugin);
    }

    @Override
    public @NotNull Collection<PluginConfig.StreamlabsAccount> parse(@NotNull StringReader reader) throws CommandSyntaxException {
        String val = reader.readString();
        if (val.equals("all")) return Collections.unmodifiableCollection(getAccounts());
        List<PluginConfig.StreamlabsAccount> accounts = getAccounts().stream()
                .filter(a -> a.id.equals(val))
                .limit(1).toList();
        if (accounts.isEmpty()) throw ACCOUNT_NOT_FOUND.create();
        return accounts;
    }

    private @NotNull Collection<PluginConfig.StreamlabsAccount> getAccounts() {
        return this.plugin.pluginConfig().getAccounts();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        getAccounts().stream().map(a -> a.id).forEach(builder::suggest);
        builder.suggest("all");
        return builder.buildFuture();
    }
}
