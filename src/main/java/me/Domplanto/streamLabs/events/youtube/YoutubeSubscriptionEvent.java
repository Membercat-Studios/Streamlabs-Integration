package me.Domplanto.streamLabs.events.youtube;

import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class YoutubeSubscriptionEvent extends StreamlabsEvent {
    public YoutubeSubscriptionEvent() {
        super("youtube_membership", "subscription", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("tier", object -> String.valueOf(object.get("level").getAsInt()));
        this.addPlaceholder("tier_name", object -> object.get("levelName").getAsString());
        this.addPlaceholder("months", object -> String.valueOf(object.get("months").getAsInt()));
        this.addPlaceholder("first_membership_date", object -> object.get("sponsorSince").getAsString());
    }
}
