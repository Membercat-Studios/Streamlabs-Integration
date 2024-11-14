package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class YoutubeMembershipGiftEvent extends BasicDonationEvent {
    public YoutubeMembershipGiftEvent() {
        super("youtube_gift_memberships", "membershipGift", StreamlabsPlatform.YOUTUBE);
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Memberships";
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s has gifted %s youtube memberships!", ChatColor.DARK_AQUA, getRelatedUser(object), (int) calculateAmount(object));
    }
}
