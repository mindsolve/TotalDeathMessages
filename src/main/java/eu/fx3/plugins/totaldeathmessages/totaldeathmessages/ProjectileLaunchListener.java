package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import eu.fx3.plugins.totaldeathmessages.utils.TridentLaunchHelper;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class ProjectileLaunchListener implements org.bukkit.event.Listener {
    final TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();
    final TridentLaunchHelper tridentLaunchHelper = pluginInstance.getTridentLaunchHelper();

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Filter for Tridents
        if (event.getEntity().getType() != EntityType.TRIDENT) {
            return;
        }

        // Filter for Players
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        // Get Thrower
        Player tridentThrower = (Player) event.getEntity().getShooter();
        // Get Throwers equipment
        EntityEquipment throwerEquipment = tridentThrower.getEquipment();

        if (throwerEquipment == null) {
            return;
        }

        boolean isInOffhand = throwerEquipment.getItemInOffHand().getType() == Material.TRIDENT;
        boolean isInMainhand = throwerEquipment.getItemInMainHand().getType() == Material.TRIDENT;

        ItemStack killerWeapon = null;

        if (isInOffhand && !isInMainhand) {
            killerWeapon = throwerEquipment.getItemInOffHand();
        } else if (isInMainhand) {
            killerWeapon = throwerEquipment.getItemInMainHand();
        }

        tridentLaunchHelper.registerTridentLaunch(tridentThrower.getUniqueId(), killerWeapon);
    }
}
