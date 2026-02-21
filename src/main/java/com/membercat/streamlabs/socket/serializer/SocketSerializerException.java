package com.membercat.streamlabs.socket.serializer;

import org.jetbrains.annotations.Nullable;

public class SocketSerializerException extends RuntimeException {
    public SocketSerializerException() {
        this(null);
    }

    public SocketSerializerException(@Nullable Throwable throwable) {
        super("Failed to serialize data received from the socket", throwable);
    }
}
