package com.membercat.streamlabs.socket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.util.components.ColorScheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamlabsSocketClient extends WebSocketClient {
    private static final String PING_MESSAGE = "2";
    private static final long DEFAULT_PING_INTERVAL = 15000;
    private static final long SESSION_INFO_TIMEOUT = 5000;
    private final Logger logger;
    private final String logPrefix;
    private final Supplier<PluginConfig.StreamlabsAccount> accountSupplier;
    private final Set<SocketEventListener> eventListeners;
    private final AtomicReference<Timer> keepAliveTimer = new AtomicReference<>();
    private final AtomicBoolean sessionInfoPresent = new AtomicBoolean();

    public StreamlabsSocketClient(@NotNull Supplier<PluginConfig.StreamlabsAccount> accountSupplier, Logger logger) {
        PluginConfig.StreamlabsAccount account = accountSupplier.get();
        super(createURI(account.socketToken));
        this.accountSupplier = accountSupplier;
        this.logPrefix = "[%s] ".formatted(account.id);
        this.logger = logger;
        this.eventListeners = new HashSet<>();
    }

    public static String getURIString(String socketToken) {
        return String.format("wss://sockets.streamlabs.com/socket.io/?token=%s&transport=websocket", socketToken);
    }

    public static URI createURI(String socketToken) throws IllegalArgumentException {
        return URI.create(getURIString(socketToken));
    }

    private boolean processStatusCode(int statusCode, @Nullable JsonElement element) {
        return switch (statusCode) {
            case 0 -> {
                if (element == null) {
                    this.logger.warning(logPrefix + "Received session info message, but contents are empty or corrupted");
                    yield false;
                }

                long interval;
                try {
                    interval = element.getAsJsonObject().get("pingInterval").getAsLong();
                    this.sessionInfoPresent.set(true);
                } catch (Throwable e) {
                    this.logger.log(Level.WARNING, logPrefix + "Received session info message, but failed to get the ping interval. Did the structure change?", e);
                    yield false;
                }

                if (StreamlabsIntegration.isDebugMode())
                    this.logger.info(logPrefix + "Session info retrieved, starting keep-alive pings using given interval...");
                this.startKeepAliveTimer(interval);
                yield false;
            }
            case 40 -> {
                this.logger.info(logPrefix + "Successfully connected to Streamlabs!");
                this.eventListeners.forEach(l -> l.onConnectionSuccess(accountSupplier.get()));
                yield false;
            }
            case 41, 44 -> {
                this.logger.warning(logPrefix + "Your access token appears to be invalid, cancelling connection...");
                DisconnectReason.INVALID_TOKEN.close(this);
                yield false;
            }
            case 42 -> true;
            default -> false;
        };
    }

    public synchronized void startKeepAliveTimer(long interval) {
        if (StreamlabsIntegration.isDebugMode())
            this.logger.info(logPrefix + "Starting keep-alive timer with an interval of %dms".formatted(interval));
        if (this.keepAliveTimer.get() != null) this.keepAliveTimer.get().cancel();
        this.keepAliveTimer.set(new Timer("Websocket keep-alive-timer"));
        this.keepAliveTimer.get().schedule(new TimerTask() {
            @Override
            public void run() {
                sendKeepAliveMessage();
            }
        }, interval, interval);
    }

    public void sendKeepAliveMessage() {
        if (this.isOpen()) this.send(PING_MESSAGE);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.logger.info(logPrefix + "Connecting to Streamlabs...");
        this.sessionInfoPresent.set(false);
        if (StreamlabsIntegration.isDebugMode())
            this.logger.info(logPrefix + "Established websocket connection, waiting for session info...");
        this.eventListeners.forEach(listener -> listener.onConnectionOpening(accountSupplier.get(), serverHandshake));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isOpen() || sessionInfoPresent.get()) return;
                logger.warning(logPrefix + "Timed out waiting for session info, now using default ping interval");
                startKeepAliveTimer(DEFAULT_PING_INTERVAL);
            }
        }, SESSION_INFO_TIMEOUT);
    }

    @Override
    public void onMessage(String message) {
        try {
            int statusCodeEndIdx = calculateStatusCodeEndIdx(message);
            int statusCode = Integer.parseInt(message.substring(0, statusCodeEndIdx));
            String json = message.substring(statusCodeEndIdx);
            JsonElement jsonElement = null;
            try {
                jsonElement = new Gson().fromJson(json, JsonElement.class);
            } catch (JsonSyntaxException ignored) {
            }
            if (processStatusCode(statusCode, jsonElement)) {
                JsonElement finalJsonElement = Objects.requireNonNull(jsonElement, "Got event message without valid JSON");
                this.eventListeners.forEach(listener -> listener.onEvent(accountSupplier.get(), finalJsonElement));
            }
        } catch (Exception e) {
            this.logger.log(Level.WARNING, logPrefix + "Failed to process Streamlabs message", e);
        }
    }

    private int calculateStatusCodeEndIdx(String message) {
        int bracket1 = message.indexOf('{');
        int bracket2 = message.indexOf('[');
        if (bracket1 == -1 && bracket2 == -1)
            return message.contains("\"") ? message.indexOf('"') : message.length();
        if (bracket1 != -1 && bracket2 != -1)
            return Math.min(bracket1, bracket2);

        return Math.max(bracket1, bracket2);
    }

    @Override
    public void onClose(int code, String message, boolean remote) {
        this.sessionInfoPresent.set(false);
        if (this.keepAliveTimer.get() != null) this.keepAliveTimer.get().cancel();
        DisconnectReason reason = DisconnectReason.fromStatusCode(code);
        if (message == null || message.isBlank()) message = reason.getCloseMessage();
        this.logger.warning(logPrefix + String.format("Lost connection to Streamlabs: %s (%s)", message, reason));
        final String finalMessage = message;
        this.eventListeners.forEach(listener -> listener.onConnectionClosed(accountSupplier.get(), reason, !finalMessage.isBlank() ? finalMessage : null));
    }

    @Override
    public void onError(Exception e) {
        this.logger.log(Level.SEVERE, logPrefix + "Unexpected socket error", e);
    }

    public void reconnectAsync() {
        new Thread(this::reconnect, "Streamlabs socket reconnection thread").start();
    }

    public void updateToken(String socketToken) {
        this.uri = createURI(socketToken);
    }

    public StreamlabsSocketClient registerListeners(@NotNull SocketEventListener... listeners) {
        this.eventListeners.addAll(List.of(listeners));
        return this;
    }

    public void intentionallyCloseIfOpen() {
        if (isOpen()) StreamlabsSocketClient.DisconnectReason.PLUGIN_CLOSED_CONNECTION.close(this);
    }

    public enum DisconnectReason {
        PLUGIN_CLOSED_CONNECTION(4000, "Connection was intentionally closed by the plugin.", "streamlabs.status.socket_closed", ColorScheme.DISABLE),
        PLUGIN_RECONNECTING(4001, "Connection was intentionally closed by the plugin with the intention of reconnecting shortly after.", "streamlabs.status.socket_reconnecting", ColorScheme.DISABLE),
        INVALID_TOKEN(4002, "The Streamlabs server refused the access token.", "streamlabs.status.invalid_token", ColorScheme.INVALID),
        CONNECTION_FAILURE(CloseFrame.NEVER_CONNECTED, "A previous attempt at initializing a connection failed.", "streamlabs.status.connection_failure", ColorScheme.ERROR),
        SERVER_CLOSED(CloseFrame.NORMAL, "Connection has been forcibly closed by the server, possibly due to a ping timeout.", "streamlabs.status.lost_connection", ColorScheme.ERROR),
        LOST_CONNECTION(4003, "Connection to the server lost.", "streamlabs.status.lost_connection", ColorScheme.ERROR);
        private final int statusCode;
        private final String closeMessage;
        private final String translationKey;
        private final TextColor color;

        DisconnectReason(int statusCode, String closeMessage, String translationKey, TextColor color) {
            this.statusCode = statusCode;
            this.closeMessage = closeMessage;
            this.translationKey = translationKey;
            this.color = color;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getCloseMessage() {
            return closeMessage;
        }

        public @NotNull Component asComponent() {
            return Component.translatable(this.translationKey, this.color);
        }

        public void close(WebSocketClient client) {
            client.close(getStatusCode(), this.closeMessage);
        }

        public static @NotNull DisconnectReason fromStatusCode(int code) {
            return Arrays.stream(values())
                    .filter(reason -> reason.getStatusCode() == code)
                    .findAny().orElse(LOST_CONNECTION);
        }
    }
}
