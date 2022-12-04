package eu.fx3.plugins.totaldeathmessages.listeners;

import eu.fx3.plugins.totaldeathmessages.ProjectileLaunchHelper;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ProjectileLaunchListener implements org.bukkit.event.Listener {
    private final ProjectileLaunchHelper projectileLaunchHelper;
    private final EntityType[] wantedType = {EntityType.TRIDENT, EntityType.ARROW, EntityType.SPECTRAL_ARROW};

    public ProjectileLaunchListener(ProjectileLaunchHelper projectileLaunchHelper) {
        this.projectileLaunchHelper = projectileLaunchHelper;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Filter for Projectiles
        if (Arrays.stream(wantedType).noneMatch(type -> type == event.getEntity().getType())) {
            return;
        }

        // Filter for Players
        if (!(event.getEntity().getShooter() instanceof Player projectileLauncher)) {
            return;
        }

        // Get Throwers equipment
        EntityEquipment launcherEquipment = projectileLauncher.getEquipment();

        if (launcherEquipment == null) {
            return;
        }

        // TODO: Simplify if-statement
        boolean isInOffhand;
        boolean isInMainhand;
        if (event.getEntity().getType() == EntityType.TRIDENT) {
            isInOffhand = launcherEquipment.getItemInOffHand().getType() == Material.TRIDENT;
            isInMainhand = launcherEquipment.getItemInMainHand().getType() == Material.TRIDENT;
        } else {
            // TODO: BOWL also contains BOW... Find a better (future-proof) way to detect the launcher
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
