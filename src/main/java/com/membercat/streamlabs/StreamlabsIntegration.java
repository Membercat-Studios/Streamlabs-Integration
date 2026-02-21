package com.membercat.streamlabs;

import com.google.gson.JsonElement;
import com.membercat.streamlabs.action.ActionExecutor;
import com.membercat.streamlabs.command.SubCommand;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
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
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamlabsIntegration extends JavaPlugin implements SocketEventListener {
    private static final String NAMESPACE = "streamlabs";
    private static final Locale[] SUPPORTED_LOCALES = {Locale.US, Locale.GERMANY};
    private static final String DEFAULT_BUNDLE_ID = "default";
    private static final Set<? extends StreamlabsEvent> STREAMLABS_EVENTS = StreamlabsEvent.findEventClasses();
    private final Set<? extends SubCommand> SUB_COMMANDS = SubCommand.findSubCommandClasses(this);
    public static Logger LOGGER;
    private static boolean DEBUG_MODE = false;
    private static boolean PAPI_INSTALLED = false;
    private StreamlabsSocketClient socketClient;
    private PluginConfig pluginConfig;
    private ActionExecutor executor;
    private File configFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

        DEBUG_MODE = pluginConfig.getOptions().debugMode;
        Translations.printAsciiArt(this);
        if (issueList != null) this.printIssues(issueList, null);
        else getComponentLogger().info(Component.text("Configuration loaded successfully, no issues found!", ColorScheme.SUCCESS));
        this.executor = new ActionExecutor(this.pluginConfig, this);
        this.setupPlaceholderExpansions();
        this.registerCommandLoadHandler();
        this.socketClient = new StreamlabsSocketClient(pluginConfig.getOptions().socketToken, getLogger())
                .registerListeners(this);
        // The StreamlabsSocketClient will not connect at all if a connection in onEnable is not attempted,
        // this is why we need to add it here, this issue should definitely be investigated further in the future!
        new Thread(() -> {
            try {
                this.socketClient.connectBlocking();
            } catch (InterruptedException ignore) {
            }
            if (!pluginConfig.getOptions().autoConnect && this.socketClient.isOpen()) {
                StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(socketClient);
                getLogger().info("Not connecting to Streamlabs at startup because auto_connect is disabled in the config!");
            }
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
    public void onEvent(@NotNull JsonElement rawData) {
        this.executor.parseAndExecute(rawData);
    }

    @Override
    public void onConnectionOpen(@NotNull ServerHandshake handshake) {
        if (this.showStatusMessages())
            Translations.sendPrefixedToPlayers("streamlabs.status.socket_open", ColorScheme.SUCCESS, getServer());
    }

    @Override
    public void onConnectionClosed(@NotNull StreamlabsSocketClient.DisconnectReason reason, @Nullable String message) {
        if (this.showStatusMessages())
            reason.sendToPlayers(getServer());
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.isOpen()) {
            StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(socketClient);
        }
        if (this.executor != null) this.executor.shutdown();
    }

    public void reloadPluginConfig() throws ConfigLoadedWithIssuesException {
        this.pluginConfig.load(this.configFile);
    }

    public StreamlabsSocketClient getSocketClient() {
        return socketClient;
    }

    public static Set<? extends StreamlabsEvent> getCachedEventObjects() {
        return STREAMLABS_EVENTS;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public ActionExecutor getExecutor() {
        return executor;
    }

    public boolean showStatusMessages() {
        return this.pluginConfig.getOptions().showStatusMessages;
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    public static boolean isPapiInstalled() {
        return PAPI_INSTALLED;
    }
}
