package eu.fx3.plugins.totaldeathmessages.listeners;

import eu.fx3.plugins.totaldeathmessages.ProjectileLaunchHelper;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ProjectileLaunchListener implements org.bukkit.event.Listener {
    private final ProjectileLaunchHelper projectileLaunchHelper;
    private final EntityType[] wantedEntityTypes = {EntityType.TRIDENT, EntityType.ARROW, EntityType.SPECTRAL_ARROW};

    public ProjectileLaunchListener(ProjectileLaunchHelper projectileLaunchHelper) {
        this.projectileLaunchHelper = projectileLaunchHelper;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Filter for Projectiles
        if (Arrays.stream(wantedEntityTypes).noneMatch(type -> type == event.getEntity().getType())) {
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

        ItemStack killerWeapon = getProjectileLaunchWeapon(event, launcherEquipment);

        projectileLaunchHelper.registerProjectileLaunch(projectileLauncher.getUniqueId(), killerWeapon);
    }

    /**
     * Helper function for determining the weapon that caused a projectile to be launched,
     * e.g. a bow or trident.
     *
     * @param event             The ProjectileLaunchEvent
     * @param launcherEquipment The causing player's equipment
     * @return ItemStack with the weapon that launched the projectile
     */
    @Nullable
    private static ItemStack getProjectileLaunchWeapon(@NotNull ProjectileLaunchEvent event, @NotNull EntityEquipment launcherEquipment) {
        ItemStack itemInOffHand = launcherEquipment.getItemInOffHand();
        ItemStack itemInMainHand = launcherEquipment.getItemInMainHand();

        // TODO: Simplify if-statement
        boolean isInOffhand;
        boolean isInMainhand;

        if (event.getEntity().getType() == EntityType.TRIDENT) {
            isInOffhand = itemInOffHand.getType() == Material.TRIDENT;
            isInMainhand = itemInMainHand.getType() == Material.TRIDENT;
        } else {
            // TODO: BOWL also contains BOW... Find a better (future-proof) way to detect the launcher
            // Could be "BOW" or "CROSSBOW"
            isInOffhand = itemInOffHand.getType().name().contains("BOW");
            isInMainhand = itemInMainHand.getType().name().contains("BOW");
        }

        ItemStack killerWeapon = null;

        if (isInOffhand && !isInMainhand) {
            killerWeapon = itemInOffHand;
        } else if (isInMainhand) {
            killerWeapon = itemInMainHand;
        }

        return killerWeapon;
    }
}
