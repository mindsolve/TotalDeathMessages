package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EntityDeathListener implements org.bukkit.event.Listener {
    JavaPlugin plugin = JavaPlugin.getPlugin(TotalDeathMessages.class);

    List<PlayerKillStats> playerKillList = new ArrayList<PlayerKillStats>();


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killerPlayer = deadEntity.getKiller();
        PlayerKillStats currentKillStat = null;

        long killTimestamp = Instant.now().getEpochSecond();

        // Ignore deaths not caused by Players
        if (killerPlayer == null) {
            return;
        }

        if (plugin.getConfig().contains("ignore-world-types")) {
            for (String worldType : plugin.getConfig().getStringList("ignore-world-types")) {
                try {
                    if (deadEntity.getWorld().getEnvironment() == World.Environment.valueOf(worldType)) {
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("The world type \"" + worldType + "\" is invalid. Ignoring entry.");
                }
            }
        }

        if (plugin.getConfig().contains("ignore-worlds")) {
            for (String worldName : plugin.getConfig().getStringList("ignore-worlds")) {
                if (deadEntity.getWorld().getName().equals(worldName)) {
                    return;
                }
            }
        }

        if (plugin.getConfig().contains("ignore-entities")) {
            for (String entityName : plugin.getConfig().getStringList("ignore-entities")) {
                try {
                    EntityType entityTypeToIgnore = EntityType.valueOf(entityName.toUpperCase());
                    if (deadEntity.getType() == entityTypeToIgnore) {
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("The entity type \"" + entityName + "\" is invalid. Ignoring entry.");
                }
            }
        }

        // Ignore Enderman deaths in the End
        if (deadEntity instanceof Enderman && deadEntity.getWorld().getEnvironment() == World.Environment.THE_END) {
            return;
        }

        // 2. try: Set statistics, if in List
        boolean isInList = false;
        for (PlayerKillStats item : playerKillList) {
            if (item.playerName.equals(killerPlayer.getName())) {

                if (killTimestamp - item.lastKillTime <= plugin.getConfig().getInt("killing-spree-timeout")) {
                    // Killing spree
                    item.spreeKillCount++;
                } else {
                    item.spreeKillCount = 1;
                }

                item.lastKillTime = killTimestamp;
                item.totalKillCount++;

                currentKillStat = item;
                isInList = true;
                break;
            }
        }

        // Add player to list
        if (!isInList) {
            PlayerKillStats killStat = new PlayerKillStats();
            killStat.spreeKillCount = 1;
            killStat.totalKillCount = 1;
            killStat.playerName = killerPlayer.getName();

            playerKillList.add(killStat);
            currentKillStat = killStat;
        }

        // Check if entity has a custom name
        boolean hasCustomName = deadEntity.getCustomName() != null;

        // Get name for killed entity (or custom name if mob is named)
        String killedEntityName = hasCustomName ? deadEntity.getCustomName().trim() : deadEntity.getName().trim();
        killedEntityName = WordUtils.capitalize(killedEntityName);
        String originalEntityName = deadEntity.getType().getName().replace("_", " ");
        originalEntityName = WordUtils.capitalize(originalEntityName);


        String deathMessage = "";
        if (hasCustomName) {
            deathMessage = ChatColor.GOLD + killedEntityName + ChatColor.DARK_GRAY +
                    " (that poor " + ChatColor.BLUE + originalEntityName + ChatColor.DARK_GRAY + ")";
        } else {
            // Attach correct article
            String article = "A" + (killedEntityName.matches("^[AEIOU].*") ? "n " : " ");
            deathMessage = ChatColor.DARK_GRAY + article + ChatColor.BLUE + killedEntityName + ChatColor.DARK_GRAY;
        }

        if (deadEntity instanceof Tameable) {
            Tameable deadTameableEntity = (Tameable) deadEntity;
            if (deadTameableEntity.isTamed()) {
                if (!killerPlayer.getDisplayName().equals(deadTameableEntity.getOwner().getName())) {
                    deathMessage += ", " + ChatColor.DARK_PURPLE + deadTameableEntity.getOwner().getName() + ChatColor.DARK_GRAY + "s pet,";
                } else {
                    deathMessage += ", the killers pet,";
                }
            }
        }

        deathMessage += " was killed by player " + ChatColor.DARK_PURPLE + killerPlayer.getDisplayName() + ChatColor.RESET;

        // Add "Killing Spree" info
        if (currentKillStat.spreeKillCount > 1) {
            deathMessage += ChatColor.RED + " (killing spree x" + currentKillStat.spreeKillCount + ")!!";
        }

        deathMessage += "!";

        Bukkit.broadcastMessage(deathMessage);

    }
}
