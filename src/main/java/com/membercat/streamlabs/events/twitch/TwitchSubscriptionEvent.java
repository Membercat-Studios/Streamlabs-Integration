package com.membercat.streamlabs.events.twitch;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class TwitchSubscriptionEvent extends BasicDonationEvent {
    public TwitchSubscriptionEvent() {
        super("twitch_subscription", "subscription", StreamlabsPlatform.TWITCH);
        this.addPlaceholder("months", object -> String.valueOf(object.get("months").getAsInt()));
        this.addPlaceholder("months_streak", object -> String.valueOf(object.get("streak_months").getAsInt()));
        this.addPlaceholder("sub_type", object -> object.get("sub_type").getAsString());
        this.addPlaceholder("sub_plan", object -> object.get("sub_plan_name").getAsString());
    }

    @Override
    public double calculateAmount(JsonObject object) {
        char tier = object.get("sub_plan").getAsString().charAt(0);
        return Integer.parseInt(String.valueOf(tier));
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Tier";
    }
}
