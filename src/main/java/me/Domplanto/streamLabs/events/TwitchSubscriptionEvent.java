package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchSubscriptionEvent extends BasicDonationEvent {
    public TwitchSubscriptionEvent() {
        super("twitch_subscription", "subscription", StreamlabsPlatform.TWITCH);
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
