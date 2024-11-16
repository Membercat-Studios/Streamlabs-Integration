package me.Domplanto.streamLabs.events.youtube;

import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class YoutubeFollowEvent extends StreamlabsEvent {
    public YoutubeFollowEvent() {
        super("youtube_subscription", "follow", StreamlabsPlatform.YOUTUBE);
    }
}
