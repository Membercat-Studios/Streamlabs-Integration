package com.membercat.streamlabs.socket;

import com.google.gson.JsonElement;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SocketEventListener {
    void onEvent(@NotNull JsonElement rawData);
    void onConnectionOpen(@NotNull ServerHandshake handshake);
    void onConnectionClosed(@NotNull StreamlabsSocketClient.DisconnectReason reason, @Nullable String message);
}
