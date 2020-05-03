package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

// TODO: Add JavaDoc

import eu.fx3.plugins.totaldeathmessages.settingutils.TridentLaunch;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TDMGlobalSettings {

    public List<TridentLaunch> tridentLaunches = new ArrayList<TridentLaunch>();

    public void registerTridentLaunch(UUID playerUUID, ItemStack launchedTrident) {
        // Remove all previous launches
        tridentLaunches.removeIf(launch -> playerUUID.equals(launch.launchPlayerUUID));

        // Add current launch
        tridentLaunches.add(new TridentLaunch(playerUUID, launchedTrident));
    }

    public ItemStack getLastThrownTrident(UUID playerUUID) {
        for (TridentLaunch launch : tridentLaunches) {
            if (launch.launchPlayerUUID.equals(playerUUID)) {
                return launch.launchedTridentItem;
            }
        }
        return null;
    }
}

