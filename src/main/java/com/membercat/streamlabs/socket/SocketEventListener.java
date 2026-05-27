package com.membercat.streamlabs.socket;

import com.google.gson.JsonElement;
import com.membercat.streamlabs.config.PluginConfig;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SocketEventListener {
    void onEvent(@NotNull PluginConfig.StreamlabsAccount account, @NotNull JsonElement rawData);

    default void onConnectionOpening(@NotNull PluginConfig.StreamlabsAccount account, @NotNull ServerHandshake handshake) {
    }

    void onConnectionSuccess(@NotNull PluginConfig.StreamlabsAccount account);

    void onConnectionClosed(@NotNull PluginConfig.StreamlabsAccount account, @NotNull StreamlabsSocketClient.DisconnectReason reason, @Nullable String message);
}
