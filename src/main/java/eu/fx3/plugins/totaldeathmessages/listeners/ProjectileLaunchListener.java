package eu.fx3.plugins.totaldeathmessages.listeners;

import eu.fx3.plugins.totaldeathmessages.ProjectileLaunchHelper;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class ProjectileLaunchListener implements org.bukkit.event.Listener {
    private final ProjectileLaunchHelper projectileLaunchHelper;

    public ProjectileLaunchListener(ProjectileLaunchHelper projectileLaunchHelper) {
        this.projectileLaunchHelper = projectileLaunchHelper;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Filter for Tridents
        if (!(event.getEntity().getType() == EntityType.TRIDENT ||
                event.getEntity().getType() == EntityType.ARROW ||
                event.getEntity().getType() == EntityType.SPECTRAL_ARROW)) {
            return;
        }

        // Filter for Players
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        // Get Thrower
        Player projectileLauncher = (Player) event.getEntity().getShooter();
        // Get Throwers equipment
        EntityEquipment launcherEquipment = projectileLauncher.getEquipment();

        if (launcherEquipment == null) {
            return;
        }

        // TODO: Simplify if-statement
        boolean isInOffhand, isInMainhand;
        if (event.getEntity().getType() == EntityType.TRIDENT) {
            isInOffhand = launcherEquipment.getItemInOffHand().getType() == Material.TRIDENT;
            isInMainhand = launcherEquipment.getItemInMainHand().getType() == Material.TRIDENT;
        } else {
            // Could be "BOW" or "CROSSBOW"
            isInOffhand = launcherEquipment.getItemInOffHand().getType().name().contains("BOW");
            isInMainhand = launcherEquipment.getItemInMainHand().getType().name().contains("BOW");
        }

        ItemStack killerWeapon = null;

        if (isInOffhand && !isInMainhand) {
            killerWeapon = launcherEquipment.getItemInOffHand();
        } else if (isInMainhand) {
            killerWeapon = launcherEquipment.getItemInMainHand();
        }

        projectileLaunchHelper.registerProjectileLauch(projectileLauncher.getUniqueId(), killerWeapon);
    }
}
