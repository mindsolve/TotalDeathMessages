package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
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

import java.time.Instant;

import static net.md_5.bungee.api.ChatColor.*;


// TODO: JavaDoc file
// TODO: Add player UUID to PlayerKillStats
// TODO: Move player id from name to UUID for PlayerKillStats


public class EntityDeathListener implements org.bukkit.event.Listener {
    TotalDeathMessages instance = TotalDeathMessages.getInstance();
    JavaPlugin plugin = (JavaPlugin) instance;
    TDMGlobalSettings globalSettings = instance.getGlobalSettings();

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

        // Ignore player deaths
        if (deadEntity instanceof Player) {
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

        if (isWorldIgnored(deadEntity.getWorld().getName())) {
            return;
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
        int killSpreeTimeout = plugin.getConfig().getInt("killing-spree-timeout");
        boolean isInList = false;
        for (PlayerKillStats item : ((TotalDeathMessages) plugin).playerKillList) {
            if (item.playerName.equals(killerPlayer.getName())) {

                if (killTimestamp - item.lastKillTime <= killSpreeTimeout) {
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

            ((TotalDeathMessages) plugin).playerKillList.add(killStat);
            currentKillStat = killStat;
        }

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
            deathMessage.append(getPetTextComponent(deadEntity).create());
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
                        deathMessage.append(TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(killerArrow));
                    }

                    // TODO:
                    //  Wenn killer zwischen Bogen und anderem Slot wechselt (Schwert), dann wird nichts angezeigt ("by shooting ")
                    //  Außerdem, wenn killer keinen Bogen hat (abgeprallter Pfeil), dann ebenfalls nicht

                    if (killerPlayer.getEquipment() != null) {
                        ItemStack killerWeapon = null;

                        //Könnte sowohl "BOW" also auch "CROSSBOW" sein
                        boolean bowInOffhand = killerPlayer.getEquipment().getItemInOffHand().getType().name().contains("BOW");
                        boolean bowInMainhand = killerPlayer.getEquipment().getItemInMainHand().getType().name().contains("BOW");

                        if (bowInOffhand && !bowInMainhand) {
                            killerWeapon = killerPlayer.getEquipment().getItemInOffHand();
                        } else if (bowInOffhand || bowInMainhand) {
                            killerWeapon = killerPlayer.getEquipment().getItemInMainHand();
                        }

                        if (killerWeapon != null) {
                            deathMessage.append(" with his ").color(DARK_GRAY);
                            deathMessage.append(TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(killerWeapon));
                        }

                        // Removed last part from ComponentBuilder
                        //  deathMessage.removeComponent(deathMessage.getCursor());
                    }


                } else if (damager instanceof ThrowableProjectile) {
                    ThrowableProjectile throwableDamager = (ThrowableProjectile) damager;
                    deathMessage.append("by throwing his ");
                    TextComponent killerWeaponComponent = TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(throwableDamager.getItem());
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
                        deathMessage.append(TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(item));
                    }
                } else if (damager instanceof Trident) {

                    getTridentMessage(killerPlayer, deathMessage);
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
                // Keine Todeswaffe kann in der linken (offhand) getragen werden und töten, außer Bogen.
                // Bogen wird allerdings durch Projectile -> Arrow abgedeckt
                EntityEquipment killerEquipment = killerPlayer.getEquipment();
                ItemStack killerWeapon = null;

                // Evtl. könnte dies auftreten, wenn der Spieler keine Items im Inventar hat
                // itemToTextComponent kann mit "null" umgehen (bare hands)
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

                TextComponent killerWeaponComponent = TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(killerWeapon);
                deathMessage.append(killerWeaponComponent);

            } else if (damager instanceof LightningStrike) {
                deathMessage.append(" by summoning a ").color(DARK_GRAY).append("Lightning Bolt").color(AQUA).append("").color(DARK_GRAY);

                getTridentMessage(killerPlayer, deathMessage);


            } else {
                deathMessage.append("Other damager! Name: " + damager.getName() + "; " + damager.getType()).color(RED);

            }

        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByBlockEvent) {
            deathMessage.append(" (How the heck did you do this? Event (block): " + ((EntityDamageByBlockEvent) deadEntity.getLastDamageCause()).getDamager().getType() + ")").color(DARK_RED);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            // Might be an Arrow of Harming (I / II). Either we assume that here, or we ignore it (because we cant check: time of check vs time of arrow firing)

            deathMessage.append(" using ").color(DARK_GRAY)
                    .append("Magic").color(AQUA)
                    .append(" (").color(DARK_GRAY)
                    .append("maybe Arrow of Harming?").italic(true)
                    .append(")").reset().color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            // If cause == FIRE_TICK && getKiller == Player, this is cased by Fire* on Playerweapon
            // Fire_Tick = "Indirekter Schaden" (Brennen)
            deathMessage.append(" using ").color(DARK_GRAY).append("Fire").color(AQUA).append("").color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FIRE) {
            // Fire = "Direkter Schaden" (Blitz)
            deathMessage.append(" using ").color(DARK_GRAY).append("Fire").color(AQUA).append("").color(DARK_GRAY);

        } else if (deadEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            deathMessage.append(" by letting him ").color(DARK_GRAY).append("fall to death.").color(BLUE);

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
            // Only send messages to players that want them
            if (!MobdeathConfig.playerWantsAllMessages(player.getUniqueId())) {
                continue;
            }

            if (!MobdeathConfig.getPlayerConfig(player.getUniqueId(), "allKillSpreeMessages") &&
                    currentKillStat.spreeKillCount > 2) {

                continue;
            }

            player.spigot().sendMessage(deathMessage.create());

        }

    }

    private void getTridentMessage(Player killerPlayer, ComponentBuilder deathMessage) {
        ItemStack killerTrident = globalSettings.getLastThrownTrident(killerPlayer.getUniqueId());

        if (killerTrident != null) {
            deathMessage.append(" with his ");
            deathMessage.append(TotalDeathMessages.getInstance().getNmsItem().itemToTextComponent(killerTrident));
        }
    }


    private boolean isWorldIgnored(String eventWorldName) {
        if (plugin.getConfig().contains("ignore-worlds")) {
            for (String worldName : plugin.getConfig().getStringList("ignore-worlds")) {
                if (eventWorldName.equals(worldName)) {
                    return true;
                }
            }
        }
        return false;
    }


    private ComponentBuilder getPetTextComponent(LivingEntity deadEntity) {
        Player killerPlayer = deadEntity.getKiller();
        ComponentBuilder thisPart = new ComponentBuilder("").color(DARK_GRAY);

        Tameable deadTameableEntity = (Tameable) deadEntity;
        if (deadTameableEntity.isTamed() && deadTameableEntity.getOwner() != null) {
            assert killerPlayer != null;
            if (!killerPlayer.getName().equals(deadTameableEntity.getOwner().getName())) {
                thisPart.append(", ").append(deadTameableEntity.getOwner().getName()).color(DARK_PURPLE).append("s pet,").color(DARK_GRAY);
            } else {
                thisPart.append(", the killers pet,").color(DARK_GRAY);
            }
        }

        return thisPart;
    }

}
