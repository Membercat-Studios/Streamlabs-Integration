package me.Domplanto.streamLabs.events;

public enum StreamlabsEventType {
    DONATION("donation"),
    TWITCH_FOLLOW("follow", "twitch_account"),
    TWITCH_SUBSCRIPTION("subscription", "twitch_account"),
    TWITCH_HOST("host", "twitch_account"),
    TWITCH_BITS("bits", "twitch_account"),
    TWITCH_RAID("raids", "twitch_account"),
    YOUTUBE_FOLLOW("follow", "youtube_account"),
    YOUTUBE_SUBSCRIPTION("subscription", "youtube_account"),
    YOUTUBE_SUPERCHAT("superchat", "youtube_account");

    private final String eventName;
    private final String platform;

    StreamlabsEventType(String eventName) {
        this(eventName, null);
    }

    StreamlabsEventType(String eventName, String platform) {
        this.eventName = eventName;
        this.platform = platform;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPlatform() {
        return platform;
    }
}