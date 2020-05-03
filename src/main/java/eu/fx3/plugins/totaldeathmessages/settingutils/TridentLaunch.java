package eu.fx3.plugins.totaldeathmessages.settingutils;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TridentLaunch {
    public final UUID launchPlayerUUID;
    public final ItemStack launchedTridentItem;

    public TridentLaunch(UUID playerUUID, ItemStack launchedTrident) {
        launchPlayerUUID = playerUUID;
        launchedTridentItem = launchedTrident;
    }
}
