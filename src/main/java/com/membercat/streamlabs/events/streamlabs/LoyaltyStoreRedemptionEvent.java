package com.membercat.streamlabs.events.streamlabs;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class LoyaltyStoreRedemptionEvent extends StreamlabsEvent {
    public LoyaltyStoreRedemptionEvent() {
        super("streamlabs_loyalty_store_redemption", "loyalty_store_redemption", StreamlabsPlatform.STREAMLABS);
        this.addPlaceholder("item", object -> object.get("product").getAsString());
        this.addPlaceholder("item_type", object -> object.get("productType").getAsString());
    }

    @Override
    public @NotNull String getRelatedUser(JsonObject object) {
        return object.get("from").getAsString();
    }
}
