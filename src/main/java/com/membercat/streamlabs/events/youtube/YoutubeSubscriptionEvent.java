package com.membercat.streamlabs.events.youtube;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class YoutubeSubscriptionEvent extends BasicDonationEvent {
    public YoutubeSubscriptionEvent() {
        super("youtube_membership", "subscription", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("tier_name", object -> object.get("membershipLevelName").getAsString());
        this.addPlaceholder("months", object -> String.valueOf(object.get("months").getAsInt()));
        this.addPlaceholder("first_membership_date", object -> object.get("sponsorSince").getAsString());
    }

    @Override
    public double calculateAmount(JsonObject object) {
        return object.get("membershipLevel").getAsInt();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Tier";
    }
}
