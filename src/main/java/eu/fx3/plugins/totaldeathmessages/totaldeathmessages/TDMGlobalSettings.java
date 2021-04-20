package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

// TODO: Add JavaDoc

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class TDMGlobalSettings {

    public HashMap<UUID, ItemStack> tridentLaunchesNew = new HashMap<>();

    public void registerTridentLaunch(UUID playerUUID, ItemStack launchedTrident) {
        // Put launch in HashMap, overwriting previous launch (if applicable)
        tridentLaunchesNew.put(playerUUID, launchedTrident);
    }

    public ItemStack getLastThrownTrident(UUID playerUUID) {
        return tridentLaunchesNew.get(playerUUID);
    }
}

