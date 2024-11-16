package me.Domplanto.streamLabs.socket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamlabsSocketClient extends WebSocketClient {
    private static final String KEEP_ALIVE_MESSAGE = "2";
    private static final long KEEP_ALIVE_INTERVAL = 15000;
    private final Logger logger;
    @Nullable
    private Timer keepAliveTimer;
    @NotNull
    private final Consumer<JsonElement> dataReceivedListener;
    @Nullable
    private Consumer<ServerHandshake> connectionOpenListener;
    @Nullable
    private Consumer<String> connectionCloseListener;
    @Nullable
    private Runnable invalidTokenListener;

    public StreamlabsSocketClient(@NotNull String socketToken, Logger logger, @NotNull Consumer<JsonElement> onDataReceived) {
        super(createURI(socketToken));
        this.logger = logger;
        this.dataReceivedListener = onDataReceived;
    }

    private static URI createURI(String socketToken) {
        return URI.create(String.format("wss://sockets.streamlabs.com/socket.io/?token=%s&transport=websocket", socketToken));
    }

    private boolean processStatusCode(int statusCode) {
        return switch (statusCode) {
            case 41, 44 -> {
                this.logger.warning("Disconnecting due to invalid access token");
                if (invalidTokenListener != null)
                    invalidTokenListener.run();
                this.close();
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
        if (connectionOpenListener != null)
            connectionOpenListener.accept(serverHandshake);
    }

    @Override
    public void onMessage(String message) {
        try {
            int statusCodeEndIdx = calculateStatusCodeEndIdx(message);
            int statusCode = Integer.parseInt(message.substring(0, statusCodeEndIdx));
            if (processStatusCode(statusCode)) {
                String json = message.substring(statusCodeEndIdx);
                JsonElement jsonElement = new Gson().fromJson(json, JsonElement.class);
                dataReceivedListener.accept(jsonElement);
            }
        } catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to process Streamlabs message", e);
        }
    }

    private int calculateStatusCodeEndIdx(String message) {
        if (!message.contains("{") && !message.contains("["))
            return message.contains("\"") ? message.indexOf('"') : message.length();
        if (message.contains("{") && message.contains("["))
            return Math.min(message.indexOf('{'), message.indexOf('['));

        return Math.max(message.indexOf('{'), message.indexOf('['));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (this.keepAliveTimer != null)
            this.keepAliveTimer.cancel();
        this.logger.warning(String.format("Lost connection to Streamlabs: %s", reason));
        if (connectionCloseListener != null)
            connectionCloseListener.accept(reason);
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

    public StreamlabsSocketClient setConnectionOpenListener(@NotNull Consumer<ServerHandshake> connectionOpenListener) {
        this.connectionOpenListener = connectionOpenListener;
        return this;
    }

    public StreamlabsSocketClient setConnectionCloseListener(@NotNull Consumer<String> connectionCloseListener) {
        this.connectionCloseListener = connectionCloseListener;
        return this;
    }

    public StreamlabsSocketClient setInvalidTokenListener(@Nullable Runnable invalidTokenListener) {
        this.invalidTokenListener = invalidTokenListener;
        return this;
    }
}
