package me.Domplanto.streamLabs.socket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Server;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamlabsSocketClient extends WebSocketClient {
    private static final String KEEP_ALIVE_MESSAGE = "2";
    private static final long KEEP_ALIVE_INTERVAL = 15000;
    private final Logger logger;
    private final Set<SocketEventListener> eventListeners;
    @Nullable
    private Timer keepAliveTimer;

    public StreamlabsSocketClient(@NotNull String socketToken, Logger logger) {
        super(createURI(socketToken));
        this.logger = logger;
        this.eventListeners = new HashSet<>();
    }

    private static URI createURI(String socketToken) {
        return URI.create(String.format("wss://sockets.streamlabs.com/socket.io/?token=%s&transport=websocket", socketToken));
    }

    private boolean processStatusCode(int statusCode) {
        return switch (statusCode) {
            case 41, 44 -> {
                this.logger.warning("Disconnecting due to invalid access token");
                DisconnectReason.INVALID_TOKEN.close(this);
                yield false;
            }
            case 42 -> true;
            default -> false;
        };
    }

    public void startKeepAliveTimer() {
        if (this.keepAliveTimer != null)
            this.keepAliveTimer.cancel();
        this.keepAliveTimer = new Timer("Websocket keep-alive-timer");
        this.keepAliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendKeepAliveMessage();
            }
        }, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL);
    }

    public void sendKeepAliveMessage() {
        if (this.isOpen()) {
            this.send(KEEP_ALIVE_MESSAGE);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.startKeepAliveTimer();
        this.logger.info("Successfully connected to Streamlabs!");
        this.eventListeners.forEach(listener -> listener.onConnectionOpen(serverHandshake));
    }

    @Override
    public void onMessage(String message) {
        try {
            int statusCodeEndIdx = calculateStatusCodeEndIdx(message);
            int statusCode = Integer.parseInt(message.substring(0, statusCodeEndIdx));
            if (processStatusCode(statusCode)) {
                String json = message.substring(statusCodeEndIdx);
                JsonElement jsonElement = new Gson().fromJson(json, JsonElement.class);
                this.eventListeners.forEach(listener -> listener.onEvent(jsonElement));
            }
        } catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to process Streamlabs message", e);
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
        if (this.keepAliveTimer != null)
            this.keepAliveTimer.cancel();
        DisconnectReason reason = DisconnectReason.fromStatusCode(code);
        this.logger.warning(String.format("Lost connection to Streamlabs: %s (%s)", message, reason));
        this.eventListeners.forEach(listener -> listener.onConnectionClosed(reason, !message.isBlank() ? message : null));
    }

    @Override
    public void onError(Exception e) {
        this.logger.log(Level.SEVERE, "Unexpected socket error", e);
    }

    public void reconnectAsync() {
        new Thread(this::reconnect, "Streamlabs socket reconnection thread").start();
    }

    public void updateToken(String socketToken) {
        this.uri = createURI(socketToken);
    }

    public StreamlabsSocketClient registerListeners(@NotNull SocketEventListener... connectionOpenListener) {
        this.eventListeners.addAll(List.of(connectionOpenListener));
        return this;
    }

    public enum DisconnectReason {
        PLUGIN_CLOSED_CONNECTION(4000, "Connection was intentionally closed by the plugin.", "streamlabs.status.socket_closed", ColorScheme.DISABLE),
        PLUGIN_RECONNECTING(4001, "Connection was intentionally closed by the plugin with the intention of reconnecting shortly after.", "streamlabs.status.socket_reconnecting", ColorScheme.DISABLE),
        INVALID_TOKEN(4002, "The streamlabs server refused the access token.", "streamlabs.status.invalid_token", ColorScheme.INVALID),
        CONNECTION_FAILURE(-1, "A previous attempt at initializing a connection failed.", "streamlabs.status.connection_failure", ColorScheme.ERROR),
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

        public void sendToPlayers(Server server) {
            Translations.sendPrefixedToPlayers(this.translationKey, this.color, server);
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
