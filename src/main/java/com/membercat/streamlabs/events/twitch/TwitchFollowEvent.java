package com.membercat.streamlabs.events.twitch;

import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class TwitchFollowEvent extends StreamlabsEvent {
    public TwitchFollowEvent() {
        super("twitch_follow", "follow", StreamlabsPlatform.TWITCH);
    }
}
