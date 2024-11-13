package me.Domplanto.streamLabs.events;

import org.jetbrains.annotations.Nullable;

public enum StreamlabsPlatform {
    STREAMLABS("streamlabs"),
    YOUTUBE("youtube_account"),
    TWITCH("twitch_account");

    private final @Nullable String id;

    StreamlabsPlatform(@Nullable String id) {
        this.id = id;
    }

    public boolean compare(@Nullable String platformString) {
        return platformString != null && platformString.equals(this.id);
    }
}
