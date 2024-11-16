package me.Domplanto.streamLabs.events.twitch;

import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class TwitchFollowEvent extends StreamlabsEvent {
    public TwitchFollowEvent() {
        super("twitch_follow", "follow", StreamlabsPlatform.TWITCH);
    }
}
