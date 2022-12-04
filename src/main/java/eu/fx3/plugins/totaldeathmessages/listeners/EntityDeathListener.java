package eu.fx3.plugins.totaldeathmessages.listeners;

import eu.fx3.plugins.totaldeathmessages.Configuration;
import eu.fx3.plugins.totaldeathmessages.PlayerKillStats;
import eu.fx3.plugins.totaldeathmessages.TotalDeathMessages;
import eu.fx3.plugins.totaldeathmessages.PlayerMessageSetting;
import eu.fx3.plugins.totaldeathmessages.ProjectileLaunchHelper;
import eu.fx3.plugins.totaldeathmessages.TextComponentHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.text.WordUtils;
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
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

import static eu.fx3.plugins.totaldeathmessages.PlayerMessageSetting.FEWER_MESSAGES;
import static eu.fx3.plugins.totaldeathmessages.PlayerMessageSetting.NO_MESSAGES;
import static net.md_5.bungee.api.ChatColor.*;


// TODO: JavaDoc file

public class EntityDeathListener implements org.bukkit.event.Listener {
    private final TotalDeathMessages instance;
    private final ProjectileLaunchHelper projectileLaunchHelper;

    public EntityDeathListener(TotalDeathMessages instance, ProjectileLaunchHelper projectileLaunchHelper) {
        this.instance = instance;
        this.projectileLaunchHelper = projectileLaunchHelper;
    }

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
                    instance.getLogger().warning("The world type \"" + worldType + "\" is invalid. Ignoring entry.");
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
                    instance.getLogger().warning("The entity type \"" + entityName + "\" is invalid. Ignoring entry.");
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
        if (deadEntity instanceof Tameable deadTamable) {
            deathMessage.append(getPetTextComponent(deadTamable));
        }

        deathMessage
                .append(" was killed by player ")
                .color(DARK_GRAY)
                .append(PlainTextComponentSerializer.plainText().serialize(killerPlayer.displayName()))
                .color(DARK_PURPLE)
                .append("")
                .color(DARK_GRAY);

        if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) deadEntity.getLastDamageCause()).getDamager();

            if (damager instanceof Projectile) {
                deathMessage.append(" ").color(DARK_GRAY);

                if (damager instanceof Arrow || damager instanceof SpectralArrow) {
                    AbstractArrow abstractArrow = (AbstractArrow) damager;
                    ItemStack arrowItemStack = abstractArrow.getItemStack();

                    // Prepare article for arrow name
                    String article = "a" + (arrowItemStack.getType().toString().matches("^[AEIOU].*") ? "n " : " ");

                    deathMessage.append("by shooting " + article);

                    deathMessage.append(TextComponentHelper.itemToTextComponent(arrowItemStack));

                    // TODO:
                    //  Nothing gets displayed ("by shooting ") if:
                    //  - The killer has no bow (ricocheted arrow)

                    ItemStack killerWeapon = projectileLaunchHelper.getLastProjectileSource(killerPlayer.getUniqueId());

                    if (killerWeapon != null) {
                        deathMessage.append(" with his ").color(DARK_GRAY);
                        deathMessage.append(TextComponentHelper.itemToTextComponent(killerWeapon));
                    }

                } else if (damager instanceof ThrownPotion) {
                    ThrownPotion potionDamager = (ThrownPotion) damager;
                    ItemMeta potionItemMeta = potionDamager.getItem().getItemMeta();

                    deathMessage.append("by throwing a ");

                    PotionMeta potionMeta = (PotionMeta) potionItemMeta;
                    ItemStack item = potionDamager.getItem();

                    assert potionMeta != null;
                    if (potionMeta.getBasePotionData().getType().equals(PotionType.INSTANT_DAMAGE)) {
                        deathMessage
                                .append("Potion of Harming " + (potionMeta.getBasePotionData().isUpgraded() ? "II" : "I"))
                                .color(AQUA);
                    } else if (potionMeta.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL)) {
                        deathMessage
                                .append("Potion of Healing " + (potionMeta.getBasePotionData().isUpgraded() ? "II" : "I"))
                                .color(AQUA);
                    } else {
                        // This should not be possible, except (maybe) for cheating a whithering potion
                        deathMessage.append(TextComponentHelper.itemToTextComponent(item));
                    }
                } else if (damager instanceof ThrowableProjectile) {
                    ThrowableProjectile throwableDamager = (ThrowableProjectile) damager;
                    deathMessage.append("by throwing his ");
                    BaseComponent killerWeaponComponent = TextComponentHelper.itemToTextComponent(throwableDamager.getItem());
                    deathMessage.append(killerWeaponComponent);

                } else if (damager instanceof ShulkerBullet) {
                    deathMessage.append("by redirecting a Shulker Bullet");

                } else if (damager instanceof Fireball) {
                    deathMessage.append("by redirecting a Ghast Fireball");

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

                getProjectileMessage(killerPlayer, deathMessage);

            } else if (damager instanceof AreaEffectCloud) {
                AreaEffectCloud areaEffectCloudDamager = (AreaEffectCloud) damager;

                if (areaEffectCloudDamager.getBasePotionData().getType().equals(PotionType.INSTANT_DAMAGE)) {
                    deathMessage
                            .append(" by throwing a ")
                            .append("Lingering Potion of ").color(DARK_GRAY)
                            .append("Harming "
                                    + (areaEffectCloudDamager.getBasePotionData().isUpgraded() ? "II" : "I")).color(AQUA);
                } else if (areaEffectCloudDamager.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL)) {
                    deathMessage
                            .append(" by throwing a ")
                            .append("Lingering Potion of ").color(DARK_GRAY)
                            .append("Healing "
                                    + (areaEffectCloudDamager.getBasePotionData().isUpgraded() ? "II" : "I")).color(AQUA);
                } else {
                    // This should not be possible with Vanialla game

                    // Get the potion effect type
                    PotionType areaEffectCloudDamagerType = areaEffectCloudDamager.getBasePotionData().getType();
                    // Make the effect type human-readable
                    String areaEffectCloudDamagerTypeName = WordUtils.capitalizeFully(
                            areaEffectCloudDamagerType.name().replace("_", " "));

                    // Add effect level, if upgradeable
                    if (areaEffectCloudDamagerType.isUpgradeable()) {
                        areaEffectCloudDamagerTypeName += areaEffectCloudDamager.getBasePotionData().isUpgraded() ? " II" : " I";
                    }

                    deathMessage.append(" by creating an ")
                            .append("Area Effect Cloud with ").color(DARK_GRAY)
                            .append(areaEffectCloudDamagerTypeName).color(AQUA);
                }

            } else if (damager instanceof Tameable && ((Tameable) damager).isTamed()) {
                String damagerType = WordUtils.capitalizeFully(damager.getType().toString().replace("_", " "));

                deathMessage.append(" by letting his " + damagerType + " loose");

            } else if (damager instanceof Creeper) {
                deathMessage.append(" by letting a creeper blow up");

            } else {
                deathMessage.append(" Other damager! Name: " + damager.getName() + "; " + damager.getType()).color(RED);

            }

        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByBlockEvent) {
            EntityDamageByBlockEvent cause = ((EntityDamageByBlockEvent) deadEntity.getLastDamageCause());
            if (cause.getCause() == EntityDamageEvent.DamageCause.VOID) {
                deathMessage
                        .append(" by pushing it into")
                        .append(" the VOID").color(BLACK).bold(true)
                        .append("").reset().color(DARK_GRAY);
            } else if (cause.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                deathMessage
                        .append(" by pushing it into")
                        .append(" Lava").color(RED).bold(true)
                        .append("").reset().color(DARK_GRAY);
            } else if (cause.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                deathMessage
                        .append(" by pushing it onto")
                        .append(" a very hot floor").color(RED).bold(true)
                        .append("").reset().color(DARK_GRAY);
            } else {
                deathMessage.append(" (How the heck did you do this? Event (block): " +
                                    "Cause:" + cause.getCause() + "; Damager:" + cause.getDamager() + ")").color(DARK_RED);
            }

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
        if (currentKillStat.getSpreeKillCount() > 1) {
            deathMessage.append(" (killing spree x" + currentKillStat.getSpreeKillCount() + ")!!").color(RED);
        }

        deathMessage
                .append("").reset().color(DARK_GRAY)
                .append("!");

        instance.getComponentLogger().info(BungeeComponentSerializer.legacy().deserialize(deathMessage.create()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerMessageSetting playerMessageSetting = Configuration.getPlayerMessageSetting(player.getUniqueId());
            // Only send messages to players that want them
            if (playerMessageSetting == NO_MESSAGES) {
                continue;
            }

            // Skip players not wanting killing spree messages
            if ((playerMessageSetting == FEWER_MESSAGES) && (currentKillStat.getSpreeKillCount() > 2)) {
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

        if (killTimestamp - currentKillStat.getLastKillTime() <= killSpreeTimeout) {
            // Killing spree
            currentKillStat.setSpreeKillCount(currentKillStat.getSpreeKillCount() + 1);
        } else {
            currentKillStat.setSpreeKillCount(1);
        }

        currentKillStat.setLastKillTime(killTimestamp);
        currentKillStat.setTotalKillCount(currentKillStat.getTotalKillCount() + 1);
        return currentKillStat;
    }

    private void getProjectileMessage(Player killerPlayer, ComponentBuilder deathMessage) {
        ItemStack killerTrident = projectileLaunchHelper.getLastProjectileSource(killerPlayer.getUniqueId());

        if (killerTrident != null) {
            deathMessage.append(" with his ");
            deathMessage.append(TextComponentHelper.itemToTextComponent(killerTrident));
        }
    }


    /**
     * Checks whether the specified world is ignored or not.
     *
     * @param eventWorldName Name of the world the event occurred in
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
     * Generates an Adventure TextComponent with details about a tamed entity, e.g. a tamed dog.
     *
     * @param deadEntity The killed tamable entity
     * @return An Adventure TextComponent with a pet specific message, if the entity is tamed & owned
     */
    @NotNull
    private TextComponent getModernPetTextComponent(Tameable deadEntity) {
        @NotNull TextComponent newResult = Component.text("");

        Player killerPlayer = deadEntity.getKiller();

        if (deadEntity.isTamed() && deadEntity.getOwnerUniqueId() != null && killerPlayer != null) {
            newResult = Component.text(", ", NamedTextColor.DARK_GRAY);

            if (killerPlayer.getUniqueId().equals(deadEntity.getOwnerUniqueId())) {
                newResult = newResult
                        .append(Component.text("the killer's pet,", NamedTextColor.DARK_GRAY));
            } else {
                AnimalTamer deadEntityOwner = deadEntity.getOwner();
                assert deadEntityOwner != null && deadEntityOwner.getName() != null;

                newResult = newResult
                        .append(Component.text(deadEntityOwner.getName(), NamedTextColor.DARK_PURPLE))
                        .append(Component.text("'s pet,", NamedTextColor.DARK_GRAY));
            }
        }

        return newResult;
    }

    /**
     * Generates a BaseComponent[] with details about a tamed entity, e.g. a tamed dog.
     *
     * @param deadEntity The killed tamable entity
     * @return A BaseComponent[] with a pet specific message, if the entity is tamed & owned
     * @deprecated Spigot text components are deprecated and their use should be removed
     */
    @NotNull
    @Deprecated(since = "v1.7.2", forRemoval = true)
    private BaseComponent[] getPetTextComponent(Tameable deadEntity) {
        TextComponent modernComponentResult = getModernPetTextComponent(deadEntity);
        return BungeeComponentSerializer.get().serialize(modernComponentResult);
    }

}
