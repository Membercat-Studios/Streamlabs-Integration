package me.Domplanto.streamLabs.events.twitch;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchSubscriptionEvent extends BasicDonationEvent {
    public TwitchSubscriptionEvent() {
        super("twitch_subscription", "subscription", StreamlabsPlatform.TWITCH);
        this.addPlaceholder("months", object -> String.valueOf(object.get("months").getAsInt()));
        this.addPlaceholder("months_streak", object -> String.valueOf(object.get("streak_months").getAsInt()));
        this.addPlaceholder("sub_type", object -> object.get("sub_type").getAsString());
        this.addPlaceholder("sub_plan", object -> object.get("sub_plan_name").getAsString());
    }

    public double calculateAmount(JsonObject object) {
        char tier = object.get("sub_plan").getAsString().charAt(0);
        return Integer.parseInt(String.valueOf(tier));
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Tier";
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s subscribed with tier %s!", ChatColor.BLUE, getRelatedUser(object), (int) calculateAmount(object));
    }
}
