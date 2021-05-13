package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting;
import eu.fx3.plugins.totaldeathmessages.utils.TextComponentHelper;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

import static eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting.*;
import static net.md_5.bungee.api.ChatColor.*;


// TODO: JavaDoc file
// TODO: Add player UUID to PlayerKillStats
// TODO: Move player id from name to UUID for PlayerKillStats


public class EntityDeathListener implements org.bukkit.event.Listener {
    TotalDeathMessages instance = TotalDeathMessages.getInstance();
    JavaPlugin plugin = instance;
    TDMGlobalSettings globalSettings = instance.getGlobalSettings();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killerPlayer = deadEntity.getKiller();

        long killTimestamp = Instant.now().getEpochSecond();

        // Ignore deaths not caused by Players
        if (killerPlayer == null) {
            return;
        }

        // Ignore player deaths
        if (deadEntity instanceof Player) {
            return;
        }

        PlayerKillStats currentKillStat = instance.playerKillStats.get(killerPlayer.getUniqueId());

        if (instance.getPluginConfig().contains("ignore-world-types")) {
            for (String worldType : instance.getPluginConfig().getStringList("ignore-world-types")) {
                try {
                    if (deadEntity.getWorld().getEnvironment() == World.Environment.valueOf(worldType)) {
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("The world type \"" + worldType + "\" is invalid. Ignoring entry.");
                }
            }
        }

        if (isWorldIgnored(deadEntity.getWorld().getName())) {
            return;
        }

        if (instance.getPluginConfig().contains("ignore-entities")) {
            for (String entityName : instance.getPluginConfig().getStringList("ignore-entities")) {
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

        // 2. try: Set statistics, if in HashMap
        currentKillStat = setPlayerKillStat(killTimestamp, currentKillStat);
        instance.playerKillStats.put(killerPlayer.getUniqueId(), currentKillStat);

        // Check if entity has a custom name
        boolean hasCustomName = deadEntity.getCustomName() != null;

        // Get name for killed entity (or custom name if mob is named)
        String killedEntityName = hasCustomName ? deadEntity.getCustomName().trim() : deadEntity.getName().trim();
        killedEntityName = WordUtils.capitalize(killedEntityName);

        /*
        TODO:
            New:
            $Player killed $Entity [$extra]
            Old:
            $Entity was killed by $Player [using $extra]
         */


        ComponentBuilder deathMessage = new ComponentBuilder().color(DARK_GRAY);
        if (hasCustomName) {
            // Get killed entity type (e.g. "Ender Dragon", "Bee")
            String killedEntityTypeName = deadEntity.getType().toString().replace("_", " ");
            killedEntityTypeName = WordUtils.capitalizeFully(killedEntityTypeName);

            deathMessage.append(killedEntityName).color(GOLD);

            // Append entity type if the entity had a custom name (nametag)
            deathMessage.append(" (that poor ").color(DARK_GRAY).append(killedEntityTypeName).color(BLUE).append(")").color(DARK_GRAY);
        } else {
            // Attach correct article as the entity name is the entity type ("Ghast" -> "A Ghast"; "Enderman" -> "An Enderman")
            // Beware: This "algorithm" doesn't respect special cases ("a unit"/"an unit")
            String article = "A" + (killedEntityName.matches("^[AEIOU].*") ? "n " : " ");
            deathMessage.append(article).color(DARK_GRAY).append(killedEntityName).color(BLUE);
        }

        // Add info for pets (owner), if applicable
        if (deadEntity instanceof Tameable) {
            deathMessage.append(getPetTextComponent((Tameable) deadEntity).create());
        }

        deathMessage.append(" was killed by player ").color(DARK_GRAY).append(killerPlayer.getDisplayName()).color(DARK_PURPLE);

        if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) deadEntity.getLastDamageCause()).getDamager();

            if (damager instanceof Projectile) {
                deathMessage.append(" ").color(DARK_GRAY);

                if (damager instanceof Arrow) {
                    Arrow arrowDamager = (Arrow) damager;

                    deathMessage.append("by shooting ");

                    if (arrowDamager.getBasePotionData().getType() != PotionType.UNCRAFTABLE && arrowDamager.getCustomEffects().isEmpty()) {
                        ItemStack killerArrow = new ItemStack(Material.TIPPED_ARROW);
                        PotionMeta meta = (PotionMeta) killerArrow.getItemMeta();
                        meta.setBasePotionData(arrowDamager.getBasePotionData());
                        killerArrow.setItemMeta(meta);
                        deathMessage.append("a ");
                        deathMessage.append(TextComponentHelper.itemToTextComponent(killerArrow));
                    }

                    // TODO:
                    //  Nothing gets displayed ("by shooting ") if:
                    //  - The killer switches between Bow and other slot (e.g. Sword)
                    //  - The killer has no bow (ricocheted arrow)

                    if (killerPlayer.getEquipment() != null) {
                        ItemStack killerWeapon = null;

                        // Could be "BOW" or "CROSSBOW"
                        boolean bowInOffhand = killerPlayer.getEquipment().getItemInOffHand().getType().name().contains("BOW");
                        boolean bowInMainhand = killerPlayer.getEquipment().getItemInMainHand().getType().name().contains("BOW");

                        if (bowInOffhand && !bowInMainhand) {
                            killerWeapon = killerPlayer.getEquipment().getItemInOffHand();
                        } else if (bowInOffhand || bowInMainhand) {
                            killerWeapon = killerPlayer.getEquipment().getItemInMainHand();
                        }

                        if (killerWeapon != null) {
                            deathMessage.append(" with his ").color(DARK_GRAY);
                            deathMessage.append(TextComponentHelper.itemToTextComponent(killerWeapon));
                        }
                    }

                } else if (damager instanceof ThrowableProjectile) {
                    ThrowableProjectile throwableDamager = (ThrowableProjectile) damager;
                    deathMessage.append("by throwing his ");
                    BaseComponent killerWeaponComponent = TextComponentHelper.itemToTextComponent(throwableDamager.getItem());
                    deathMessage.append(killerWeaponComponent);

                } else if (damager instanceof ThrownPotion) {
                    ThrownPotion potionDamager = (ThrownPotion) damager;
                    ItemMeta potionItemMeta = potionDamager.getItem().getItemMeta();

                    deathMessage.append("by throwing a ");

                    PotionMeta potionMeta = (PotionMeta) potionItemMeta;
                    ItemStack item = potionDamager.getItem();

                    assert potionMeta != null;
                    if (potionMeta.getBasePotionData().getType().equals(PotionType.INSTANT_DAMAGE)) {
                        deathMessage.append("Potion of ").color(DARK_GRAY).append("Harming " +
                                (potionMeta.getBasePotionData().isUpgraded() ? "II" : "I")).color(AQUA);
                    } else {
                        // This should not be possible, except (maybe) for cheating a whithering potion
                        deathMessage.append(TextComponentHelper.itemToTextComponent(item));
                    }

                } else {
                    Projectile projectileDamager = (Projectile) damager;

                    deathMessage.append("Projectile ");
                    deathMessage.append(projectileDamager.getName());
                    if (projectileDamager.getCustomName() != null) {
                        deathMessage.append(" \"" + projectileDamager.getCustomName() + "\"");
                    }
                }

            } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.THORNS) {
                // Thorns
                //TODO: Find better message
                deathMessage.append(" by letting his armor do the job for him");

            } else if (damager instanceof Player) {
                // No killer weapon can be whielded in the offhand (left), except for bows.
                // Bows should already be covered by Projectile -> Arrow
                EntityEquipment killerEquipment = killerPlayer.getEquipment();
                ItemStack killerWeapon = null;

                // This could happen if the player has no items in his inventory
                // itemToTextComponent is capable of handling "null" (bare hands)
                if (killerEquipment != null) {
                    if (killerEquipment.getItemInMainHand().getType().name().contains("SWORD")) {
                        deathMessage.append(" by slashing it with his ").color(DARK_GRAY);
                    } else if (killerEquipment.getItemInMainHand().getType() == Material.TRIDENT) {
                        deathMessage.append(" by stabbing it with his ").color(DARK_GRAY);
                    } else {
                        deathMessage.append(" by hitting it repeatedly with his ").color(DARK_GRAY);
                    }

                    killerWeapon = killerEquipment.getItemInMainHand();
                }

                BaseComponent killerWeaponComponent = TextComponentHelper.itemToTextComponent(killerWeapon);
                deathMessage.append(killerWeaponComponent);

            } else if (damager instanceof LightningStrike) {
                deathMessage.append(" by summoning a ").color(DARK_GRAY).append("Lightning Bolt").color(AQUA).append("").color(DARK_GRAY);

                getTridentMessage(killerPlayer, deathMessage);

            } else if (damager instanceof AreaEffectCloud) {
                AreaEffectCloud areaEffectCloudDamager = (AreaEffectCloud) damager;

                if (areaEffectCloudDamager.getBasePotionData().getType().equals(PotionType.INSTANT_DAMAGE)) {
                    deathMessage
                            .append(" by throwing a ")
                            .append("Lingering Potion of ").color(DARK_GRAY)
                            .append("Harming "
                                    + (areaEffectCloudDamager.getBasePotionData().isUpgraded() ? "II" : "I")).color(AQUA);
                } else {
                    // This should not be possible with Vanialla game

                    // Get the potion effect type
                    PotionType areaEffectCloudDamagerType = areaEffectCloudDamager.getBasePotionData().getType();
                    // Make the effect type human-readable
                    String areaEffectCloudDamagerTypeName = WordUtils.capitalizeFully(
                            areaEffectCloudDamagerType.name().replaceAll("_", " "));

                    // Add effect level, if upgradeable
                    if (areaEffectCloudDamagerType.isUpgradeable()) {
                        areaEffectCloudDamagerTypeName += areaEffectCloudDamager.getBasePotionData().isUpgraded() ? " II" : " I";
                    }

                    deathMessage.append(" by creating an ")
                            .append("Area Effect Cloud with ").color(DARK_GRAY)
                            .append(areaEffectCloudDamagerTypeName).color(AQUA);
                }

            } else {
                deathMessage.append(" Other damager! Name: " + damager.getName() + "; " + damager.getType()).color(RED);

            }

        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByBlockEvent) {
            deathMessage.append(" (How the heck did you do this? Event (block): " + ((EntityDamageByBlockEvent) deadEntity.getLastDamageCause()).getDamager().getType() + ")").color(DARK_RED);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            // Might be an Arrow of Harming (I / II). Either we assume that here, or we ignore it
            // (because we can't check: time of check vs time of arrow firing)

            deathMessage.append(" using ").color(DARK_GRAY)
                    .append("Magic").color(AQUA)
                    .append(" (").color(DARK_GRAY)
                    .append("maybe Arrow of Harming?").italic(true)
                    .append(")").reset().color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            // If cause == FIRE_TICK && getKiller == Player, this is cased by Fire* on Playerweapon
            // Fire_Tick = "Indirect Damage" (Burning)
            deathMessage.append(" using ").color(DARK_GRAY).append("Fire").color(AQUA).append("").color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FIRE) {
            // Fire = "Direct Damage" (Lightning bolt)
            deathMessage.append(" using ").color(DARK_GRAY).append("Fire").color(AQUA).append("").color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            deathMessage.append(" by letting him ").color(DARK_GRAY).append("fall to death").color(BLUE).append("").color(DARK_GRAY);

        } else {
            deathMessage.append(" (How the heck did you do this? Event (other): " + deadEntity.getLastDamageCause().getCause() + ")").color(DARK_RED);
        }


        // Add "Killing Spree" info
        if (currentKillStat.spreeKillCount > 1) {
            deathMessage.append(" (killing spree x" + currentKillStat.spreeKillCount + ")!!").color(RED);
        }

        deathMessage.append("!");

        TotalDeathMessages.getInstance().getLogger().info(BaseComponent.toLegacyText(deathMessage.create()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerMessageSetting playerMessageSetting = MobdeathConfig.getPlayerMessageSetting(player.getUniqueId());
            // Only send messages to players that want them
            if (playerMessageSetting == NO_MESSAGES) {
                continue;
            }

            // Skip players not wanting killing spree messages
            if ((playerMessageSetting == FEWER_MESSAGES) && (currentKillStat.spreeKillCount > 2)) {
                continue;
            }

            player.spigot().sendMessage(deathMessage.create());

        }

    }

    /**
     * Updates or creates the Players {@link PlayerKillStats} object
     *
     * @param killTimestamp   Epoch kill timestamp (e.g. from {@link Instant#getEpochSecond()})
     * @param currentKillStat Existing {@link PlayerKillStats} object or null
     * @return Updated {@link PlayerKillStats} object
     */
    @NotNull
    private PlayerKillStats setPlayerKillStat(long killTimestamp, @Nullable PlayerKillStats currentKillStat) {
        int killSpreeTimeout = instance.getPluginConfig().getInt("killing-spree-timeout");

        if (currentKillStat == null) {
            currentKillStat = new PlayerKillStats();
        }

        if (killTimestamp - currentKillStat.lastKillTime <= killSpreeTimeout) {
            // Killing spree
            currentKillStat.spreeKillCount++;
        } else {
            currentKillStat.spreeKillCount = 1;
        }

        currentKillStat.lastKillTime = killTimestamp;
        currentKillStat.totalKillCount++;
        return currentKillStat;
    }

    private void getTridentMessage(Player killerPlayer, ComponentBuilder deathMessage) {
        ItemStack killerTrident = globalSettings.getLastThrownTrident(killerPlayer.getUniqueId());

        if (killerTrident != null) {
            deathMessage.append(" with his ");
            deathMessage.append(TextComponentHelper.itemToTextComponent(killerTrident));
        }
    }


    /**
     * Checks whether the specified world is ignored or not.
     *
     * @param eventWorldName Name of the world the event occured in
     * @return True if the world is ignored; false otherwise.
     */
    private boolean isWorldIgnored(String eventWorldName) {
        if (!instance.getPluginConfig().contains("ignore-worlds")) {
            return false;
        }

        @NotNull List<String> ignoredWorldNames = instance.getPluginConfig().getStringList("ignore-worlds");
        return ignoredWorldNames.contains(eventWorldName);
    }


    /**
     * Generates a ComponentBuilder with details about a tamed entity, e.g. a tamed dog.
     *
     * @param deadEntity The killed tameable entity
     * @return A ComponentBuilder with a pet specific message, if the entitiy is tamed & owned
     */
    @NotNull
    private ComponentBuilder getPetTextComponent(Tameable deadEntity) {
        @NotNull ComponentBuilder result = new ComponentBuilder("").color(DARK_GRAY);

        Player killerPlayer = deadEntity.getKiller();

        if (deadEntity.isTamed() && deadEntity.getOwner() != null && killerPlayer != null) {
            AnimalTamer deadEntityOwner = deadEntity.getOwner();
            if (killerPlayer.getName().equals(deadEntityOwner.getName())) {
                result.append(", the killer's pet,").color(DARK_GRAY);
            } else {
                result.append(", ").append(deadEntityOwner.getName()).color(DARK_PURPLE).append("'s pet,").color(DARK_GRAY);
            }
        }

        return result;
    }

}
