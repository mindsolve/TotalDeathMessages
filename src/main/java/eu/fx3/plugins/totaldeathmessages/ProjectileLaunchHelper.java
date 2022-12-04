package eu.fx3.plugins.totaldeathmessages;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProjectileLaunchHelper {

    public final Map<UUID, ItemStack> projectileLaunches = new HashMap<>();

    public void registerProjectileLaunch(UUID playerUUID, ItemStack projectileLauncher) {
        // Put launch in HashMap, overwriting previous launch (if applicable)
        projectileLaunches.put(playerUUID, projectileLauncher);
    }

    public ItemStack getLastProjectileSource(UUID playerUUID) {
        return projectileLaunches.get(playerUUID);
    }

}
