package com.membercat.streamlabs;

import com.google.gson.JsonElement;
import com.membercat.streamlabs.action.ActionExecutor;
import com.membercat.streamlabs.command.SubCommand;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.database.DatabaseManager;
import com.membercat.streamlabs.database.provider.DatabaseProvider;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.papi.StreamlabsExpansion;
import com.membercat.streamlabs.socket.SocketEventListener;
import com.membercat.streamlabs.socket.StreamlabsSocketClient;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.*;

public class StreamlabsIntegration extends JavaPlugin implements SocketEventListener {
    private static final String NAMESPACE = "streamlabs";
    private static final Locale[] SUPPORTED_LOCALES = {Locale.US, Locale.GERMANY};
    private static final String DEFAULT_BUNDLE_ID = "default";
    private static Path DATA_PATH;
    private static final Set<? extends StreamlabsEvent> STREAMLABS_EVENTS = StreamlabsEvent.findEventClasses();
    private final Set<? extends SubCommand> SUB_COMMANDS = SubCommand.findSubCommandClasses(this);
    private final Map<PluginConfig.StreamlabsAccount, StreamlabsSocketClient> socketClients = new ConcurrentHashMap<>();
    public static Logger LOGGER;
    private static @Nullable Boolean DEBUG_MODE = null;
    private static boolean PAPI_INSTALLED = false;
    private PluginConfig pluginConfig;
    private ActionExecutor executor;
    private DatabaseManager databaseManager;
    private File configFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DATA_PATH = getDataPath();
        LOGGER = getLogger();
        this.initializeResourceBundles();
        this.pluginConfig = new PluginConfig(getComponentLogger());
        ConfigIssueHelper.IssueList issueList = null;
        try {
            this.configFile = new File(getDataFolder(), "config.yml");
            this.pluginConfig.load(configFile);
        } catch (ConfigLoadedWithIssuesException e) {
            issueList = e.getIssues();
        }

        DEBUG_MODE = pluginConfig.debugMode;
        Translations.printAsciiArt(this);
        if (issueList != null) this.printIssues(issueList, null);
        else
            getComponentLogger().info(text("Configuration loaded successfully, no issues found!", ColorScheme.SUCCESS));
        this.recreateDatabaseManager();
        this.databaseManager.init();
        this.executor = new ActionExecutor(this.pluginConfig, this);
        this.setupPlaceholderExpansions();
        this.registerCommandLoadHandler();
        this.synchronizeSocketClients();
        // The StreamlabsSocketClient will not connect at all if a connection in onEnable is not attempted,
        // this is why we need to add it here, this issue should definitely be investigated further in the future!
        new Thread(() -> {
            this.socketClients.values().forEach(c -> {
                try {
                    c.connectBlocking();
                } catch (InterruptedException ignored) {
                }
            });
            this.socketClients.entrySet()
                    .stream().filter(e -> !e.getKey().autoConnect)
                    .forEach(e -> {
                        StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(e.getValue());
                        getLogger().info("Not connecting account \"%s\" to Streamlabs at startup because auto_connect is disabled in the config!".formatted(e.getKey().id));
                    });
        }, "Socket startup Thread").start();
    }

    private void setupPlaceholderExpansions() {
        PAPI_INSTALLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!isPapiInstalled()) return;

        StreamlabsExpansion expansion = new StreamlabsExpansion(this);
        expansion.register();
        getLogger().info("Successfully hooked into PlaceholderAPI!");
    }

    private void initializeResourceBundles() {
        TranslationStore.StringBased<MessageFormat> store = TranslationStore.messageFormat(Key.key(NAMESPACE, DEFAULT_BUNDLE_ID));
        for (Locale locale : SUPPORTED_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle("%s.%s".formatted(NAMESPACE, DEFAULT_BUNDLE_ID), locale, UTF8ResourceBundleControl.get());
            store.registerAll(locale, bundle, true);
        }

        GlobalTranslator.translator().addSource(store);
    }

    private void registerCommandLoadHandler() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(NAMESPACE)
                .requires(source -> source.getSender().hasPermission(NAMESPACE + ".admin"));
        SUB_COMMANDS.forEach(command -> builder.then(command.buildCommand()));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            try {
                event.registrar().register(builder.build());
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to register brigadier commands, please report this error to the developers at %s".formatted(Translations.ISSUES_URL), e);
            }
        });
    }

    public void printIssues(ConfigIssueHelper.IssueList issues, @Nullable CommandSender sender) {
        boolean isConsole = sender == null;
        Component message = issues.getListMessage(isConsole ? -1 : 7, !isConsole);
        Translations.sendComponentsSplit(message, isConsole ? getComponentLogger()::info : sender::sendMessage);
    }

    @Override
    public void onEvent(@NotNull PluginConfig.StreamlabsAccount account, @NotNull JsonElement rawData) {
        this.executor.parseAndExecute(rawData, account);
    }

    @Override
    public void onConnectionSuccess(@NotNull PluginConfig.StreamlabsAccount account) {
        this.sendStatusMessage(account, translatable("streamlabs.status.socket_open", ColorScheme.SUCCESS));
    }

    @Override
    public void onConnectionClosed(@NotNull PluginConfig.StreamlabsAccount account, @NotNull StreamlabsSocketClient.DisconnectReason reason, @Nullable String message) {
        this.sendStatusMessage(account, reason.asComponent());
    }

    private void sendStatusMessage(@NotNull PluginConfig.StreamlabsAccount account, @NotNull Component message) {
        if (!account.showStatusMessages) return;
        Component prefix = translatable("streamlabs.status.prefix", ColorScheme.COMMENT, text(account.id, ColorScheme.DISABLE));
        Translations.sendPrefixedToPlayers(empty().append(prefix).appendSpace().append(message), getServer(), false);
    }

    @Override
    public void onDisable() {
        this.socketClients.values().forEach(StreamlabsSocketClient::intentionallyCloseIfOpen);
        if (this.executor != null) this.executor.shutdown();
        if (this.databaseManager != null) this.databaseManager.close();
    }

    public void reloadPluginConfig() throws ConfigLoadedWithIssuesException {
        this.pluginConfig.load(this.configFile);
    }

    public @Nullable StreamlabsSocketClient getSocketClient(@NotNull PluginConfig.StreamlabsAccount account) {
        return this.socketClients.get(account);
    }

    public static Set<? extends StreamlabsEvent> getCachedEventObjects() {
        return STREAMLABS_EVENTS;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public DatabaseProvider dbProvider() {
        return pluginConfig().getDatabaseOptions().provider;
    }

    public void recreateDatabaseManager() {
        this.databaseManager = pluginConfig().getDatabaseOptions().createManager();
    }

    public void synchronizeSocketClients() {
        new HashMap<>(this.socketClients).entrySet().stream()
                .filter(e -> !pluginConfig().getAccounts().contains(e.getKey()))
                .forEach(e -> {
                    e.getValue().intentionallyCloseIfOpen();
                    this.socketClients.remove(e.getKey());
                });
        pluginConfig().getAccounts().stream()
                .filter(a -> !this.socketClients.containsKey(a))
                .forEach(a -> this.socketClients.put(a, a.createClient(this).registerListeners(this)));
    }

    public DatabaseManager dbManager() {
        return databaseManager;
    }

    public ActionExecutor getExecutor() {
        return executor;
    }

    public static boolean isDebugMode() {
        return Boolean.TRUE.equals(DEBUG_MODE);
    }

    public static boolean isDebugModeDefined() {
        return DEBUG_MODE != null;
    }

    public static @NotNull Path dataPath() {
        return DATA_PATH;
    }

    public static boolean isPapiInstalled() {
        return PAPI_INSTALLED;
    }
}
