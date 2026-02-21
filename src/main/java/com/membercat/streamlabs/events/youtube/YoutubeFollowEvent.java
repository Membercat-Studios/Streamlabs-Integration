package com.membercat.streamlabs.events.youtube;

import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class YoutubeFollowEvent extends StreamlabsEvent {
    public YoutubeFollowEvent() {
        super("youtube_subscription", "follow", StreamlabsPlatform.YOUTUBE);
    }
}
