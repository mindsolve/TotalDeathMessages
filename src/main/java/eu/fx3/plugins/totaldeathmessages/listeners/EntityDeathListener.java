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
import net.kyori.adventure.text.format.TextDecoration;
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
        String customName = "";
        Component customNameComponent = deadEntity.customName();
        if (customNameComponent != null) {
            customName = PlainTextComponentSerializer.plainText().serialize(customNameComponent);
        }

        // Get name for killed entity (or custom name if mob is named)
        String killedEntityName = !customName.isEmpty() ? customName : deadEntity.getName().trim();
        killedEntityName = WordUtils.capitalize(killedEntityName);

        /*
        TODO:
            New:
            $Player killed $Entity [$extra]
            Old:
            $Entity was killed by $Player [using $extra]
         */

        final TextComponent.Builder textComponentBuilder = Component.text()
                .color(NamedTextColor.DARK_GRAY);

        if (!customName.isEmpty()) {
            textComponentBuilder.append(customNameComponent.color(NamedTextColor.GOLD));

            // Get killed entity type (e.g. "Ender Dragon", "Bee")
            String killedEntityTypeName = deadEntity.getType().toString().replace("_", " ");
            killedEntityTypeName = WordUtils.capitalizeFully(killedEntityTypeName);

            // Append entity type if the entity had a custom name (nametag)
            textComponentBuilder
                    .append(Component.text(" (that poor ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(killedEntityTypeName).color(NamedTextColor.BLUE))
                    .append(Component.text(")").color(NamedTextColor.DARK_GRAY));

        } else {
            // Attach correct article as the entity name is the entity type ("Ghast" -> "A Ghast"; "Enderman" -> "An Enderman")
            // Beware: This "algorithm" doesn't respect special cases ("a unit"/"an unit")
            String article = "A" + (killedEntityName.matches("^[AEIOU].*") ? "n " : " ");
            textComponentBuilder
                    .append(Component.text(article).color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(killedEntityName).color(NamedTextColor.BLUE));
        }

        // Add info for pets (owner), if applicable
        if (deadEntity instanceof Tameable deadTamable) {
            textComponentBuilder.append(getModernPetTextComponent(deadTamable));
        }

        textComponentBuilder
                .append(Component.text(" was killed by player ").color(NamedTextColor.DARK_GRAY))
                .append(killerPlayer.displayName().color(NamedTextColor.DARK_PURPLE))
                .append(Component.text().color(NamedTextColor.DARK_GRAY));

        EntityDamageEvent lastDamageEvent = deadEntity.getLastDamageCause();
        if (lastDamageEvent instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) lastDamageEvent).getDamager();

            if (damager instanceof Projectile) {
                textComponentBuilder.append(Component.text(" "));

                if (damager instanceof Arrow || damager instanceof SpectralArrow) {
                    AbstractArrow abstractArrow = (AbstractArrow) damager;
                    ItemStack arrowItemStack = abstractArrow.getItemStack();

                    // Prepare article for arrow name
                    String article = "a" + (arrowItemStack.getType().toString().matches("^[AEIOU].*") ? "n " : " ");

                    textComponentBuilder
                            .append(Component.text("by shooting " + article))
                            .append(TextComponentHelper.itemToComponent(arrowItemStack));

                    // TODO:
                    //  Nothing gets displayed ("by shooting ") if:
                    //  - The killer has no bow (ricocheted arrow)

                    ItemStack killerWeapon = projectileLaunchHelper.getLastProjectileSource(killerPlayer.getUniqueId());

                    if (killerWeapon != null) {
                        textComponentBuilder
                                .append(Component.text(" with his ").color(NamedTextColor.DARK_GRAY))
                                .append(TextComponentHelper.itemToComponent(killerWeapon));
                    }

                } else if (damager instanceof ThrownPotion potionDamager) {
                    ItemMeta potionItemMeta = potionDamager.getItem().getItemMeta();

                    textComponentBuilder.append(Component.text("by throwing a "));

                    PotionMeta potionMeta = (PotionMeta) potionItemMeta;
                    ItemStack item = potionDamager.getItem();

                    assert potionMeta != null;
                    if (potionMeta.getBasePotionData().getType().equals(PotionType.INSTANT_DAMAGE)) {
                        textComponentBuilder
                                .append(Component.text("Potion of Harming " + (potionMeta.getBasePotionData().isUpgraded() ? "II" : "I")).color(NamedTextColor.AQUA));
                    } else if (potionMeta.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL)) {
                        textComponentBuilder
                                .append(Component.text("Potion of Healing " + (potionMeta.getBasePotionData().isUpgraded() ? "II" : "I")).color(NamedTextColor.AQUA));
                    } else {
                        // This should not be possible, except (maybe) for cheating a whithering potion
                        textComponentBuilder.append(TextComponentHelper.itemToComponent(item));
                    }
                } else if (damager instanceof ThrowableProjectile throwableDamager) {
                    textComponentBuilder
                            .append(Component.text("by throwing his "))
                            .append(TextComponentHelper.itemToComponent(throwableDamager.getItem()));

                } else if (damager instanceof ShulkerBullet) {
                    textComponentBuilder
                            .append(Component.text("by redirecting a Shulker Bullet"));

                } else if (damager instanceof Fireball) {
                    textComponentBuilder
                            .append(Component.text("by redirecting a Ghast Fireball"));

                } else {
                    Projectile projectileDamager = (Projectile) damager;

                    textComponentBuilder
                            .append(Component.text("using Projectile "))
                            .append(projectileDamager.name());

                    Component projectileDamagerCustomName = projectileDamager.customName();
                    if (projectileDamagerCustomName != null) {
                        textComponentBuilder
                                .append(Component.text(" \""))
                                .append(projectileDamagerCustomName)
                                .append(Component.text("\""));
                    }
                }

            } else if (lastDamageEvent.getCause() == EntityDamageEvent.DamageCause.THORNS) {
                // Thorns
                //TODO: Find better message
                textComponentBuilder
                        .append(Component.text(" by letting his armor do the job for him"));

            } else if (damager instanceof Player) {
                // No killer weapon can be wielded in the offhand (left), except for bows.
                // Bows should already be covered by Projectile -> Arrow
                EntityEquipment killerEquipment = killerPlayer.getEquipment();
                ItemStack killerWeapon = null;

                // This could happen if the player has no items in his inventory
                // itemToTextComponent is capable of handling "null" (bare hands)
                if (killerEquipment != null) {
                    if (killerEquipment.getItemInMainHand().getType().name().contains("SWORD")) {
                        textComponentBuilder.append(Component.text(" by slashing it with his ").color(NamedTextColor.DARK_GRAY));
                    } else if (killerEquipment.getItemInMainHand().getType() == Material.TRIDENT) {
                        textComponentBuilder.append(Component.text(" by stabbing it with his ").color(NamedTextColor.DARK_GRAY));
                    } else {
                        textComponentBuilder.append(Component.text(" by hitting it repeatedly with his ").color(NamedTextColor.DARK_GRAY));
                    }

                    killerWeapon = killerEquipment.getItemInMainHand();
                }

                textComponentBuilder.append(TextComponentHelper.itemToComponent(killerWeapon));

            } else if (damager instanceof LightningStrike) {
                textComponentBuilder
                        .append(Component.text(" by summoning a ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Lightning Bolt").color(NamedTextColor.AQUA))
                        .append(Component.text().color(NamedTextColor.DARK_GRAY))
                        .append(getProjectileMessage(killerPlayer));


            } else if (damager instanceof AreaEffectCloud areaEffectCloudDamager) {

                PotionType potionType = areaEffectCloudDamager.getBasePotionData().getType();
                textComponentBuilder
                        .append(Component.text(" by throwing a "))
                        .append(Component.text("Lingering Potion of ").color(NamedTextColor.DARK_GRAY));


                if (potionType.equals(PotionType.INSTANT_DAMAGE)) {
                    textComponentBuilder
                            .append(Component.text("Harming ").color(NamedTextColor.AQUA));

                } else if (potionType.equals(PotionType.INSTANT_HEAL)) {
                    textComponentBuilder
                            .append(Component.text("Healing ").color(NamedTextColor.AQUA));


                } else {
                    // This should not be possible with Vanilla game

                    // Make the effect type human-readable
                    String areaEffectCloudDamagerTypeName = WordUtils.capitalizeFully(
                            potionType.name().replace("_", " "));

                    textComponentBuilder
                            .append(Component.text(areaEffectCloudDamagerTypeName).color(NamedTextColor.AQUA));
                }

                // Add effect level, if upgradeable
                if (potionType.isUpgradeable()) {
                    textComponentBuilder.append(Component.text(areaEffectCloudDamager.getBasePotionData().isUpgraded() ? " II" : " I").color(NamedTextColor.AQUA));
                }

            } else if (damager instanceof Tameable tameable && tameable.isTamed()) {
                String damagerType = WordUtils.capitalizeFully(damager.getType().toString().replace("_", " "));

                textComponentBuilder
                        .append(Component.text(" by letting his " + damagerType + " loose"));

            } else if (damager instanceof Creeper) {
                textComponentBuilder
                        .append(Component.text(" by letting a creeper blow up"));

            } else {
                textComponentBuilder
                        .append(Component.text(" by unknown means with an Entity (name: " + damager.getName() + "; " + damager.getType() + ")").color(NamedTextColor.RED));

            }

        } else if (lastDamageEvent instanceof EntityDamageByBlockEvent cause) {
            if (cause.getCause() == EntityDamageEvent.DamageCause.VOID) {
                textComponentBuilder
                        .append(Component.text(" by pushing it into"))
                        .append(Component.text(" the VOID").color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD));

            } else if (cause.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                textComponentBuilder
                        .append(Component.text(" by pushing it into"))
                        .append(Component.text(" Lava").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));


            } else if (cause.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                textComponentBuilder
                        .append(Component.text(" by pushing it onto"))
                        .append(Component.text(" a very hot floor").color(NamedTextColor.RED));

            } else {

                textComponentBuilder
                        .append(Component.text(" by unknown means with a block (cause: " + cause.getCause() + "; damager: " + cause.getDamager() + ")").color(NamedTextColor.RED));
            }

        } else if (lastDamageEvent.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            // Might be an Arrow of Harming (I / II) or Arrow of Healing (I/II) for undead.
            // Either we assume that here, or we ignore it
            // (because we can't check: time of check vs time of arrow firing)
            // TODO: Can this be covered by our ProjectileLaunchListener?

            textComponentBuilder
                    .append(Component.text(" using "))
                    .append(Component.text("Magic").color(NamedTextColor.AQUA))
                    .append(Component.text(" ("))
                    .append(Component.text("maybe Arrow of Harming?").decorate(TextDecoration.ITALIC))
                    .append(Component.text(")"));

        } else if (lastDamageEvent.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                lastDamageEvent.getCause() == EntityDamageEvent.DamageCause.FIRE) {
            // If cause == FIRE_TICK && getKiller == Player, this is caused by Fire* on Playerweapon
            // Fire_Tick = "Indirect Damage" (Burning)
            // Fire = "Direct Damage" (Lightning bolt)

            textComponentBuilder
                    .append(Component.text(" using "))
                    .append(Component.text("Fire").color(NamedTextColor.AQUA));

        } else if (lastDamageEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {
            textComponentBuilder
                    .append(Component.text(" by letting him "))
                    .append(Component.text("fall to death").color(NamedTextColor.BLUE));


        } else {
            textComponentBuilder
                    .append(Component.text(" by unknown means (cause: " + lastDamageEvent.getCause() + "; damage type class: " + lastDamageEvent.getClass().getName() + ")").color(NamedTextColor.RED));

        }


        // Add "Killing Spree" info
        if (currentKillStat.getSpreeKillCount() > 1) {
            textComponentBuilder
                    .append(Component.text(" (killing spree x" + currentKillStat.getSpreeKillCount() + ")!!").color(NamedTextColor.RED));
        }

        textComponentBuilder
                .append(Component.text("!").color(NamedTextColor.DARK_GRAY));

        instance.getComponentLogger().info(textComponentBuilder.build());
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

            player.sendMessage(textComponentBuilder.build());
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

    @Deprecated(forRemoval = true)
    private void addLegacyProjectileMessage(Player killerPlayer, ComponentBuilder deathMessage) {
        deathMessage.append(BungeeComponentSerializer.get().serialize(getProjectileMessage(killerPlayer)));
    }

    private @NotNull TextComponent getProjectileMessage(Player killerPlayer) {
        TextComponent.Builder projectileMessage = Component.text();
        ItemStack killerProjectile = projectileLaunchHelper.getLastProjectileSource(killerPlayer.getUniqueId());

        if (killerProjectile != null) {
            projectileMessage
                    .append(Component.text(" with his "))
                    .append(TextComponentHelper.itemToComponent(killerProjectile));
        }

        return projectileMessage.build();
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
