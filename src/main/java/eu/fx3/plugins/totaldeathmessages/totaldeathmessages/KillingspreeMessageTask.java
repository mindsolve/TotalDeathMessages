package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// TODO: JavaDoc this file
// TODO: Rewrite KillSpree end message
// TODO: Make config run-dynamic (non-final)

public class KillingspreeMessageTask extends BukkitRunnable {
    private final JavaPlugin plugin;

    public KillingspreeMessageTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int killSpreeTimeout = ((TotalDeathMessages)plugin).getPluginConfig().getInt("killing-spree-timeout");
        List<Player> playerList = getPlayersForReducedKillSpreeMessages();
        TextComponent.Builder chatMessage = Component.text();

        // TODO: What is the performance impact of running this with a huge userbase?

        for (Map.Entry<UUID, PlayerKillStats> entry : ((TotalDeathMessages) plugin).playerKillStats.entrySet()) {
            PlayerKillStats killStat = entry.getValue();
            UUID playerUUID = entry.getKey();

            if (killStat.spreeKillCount > 2 && (Instant.now().getEpochSecond() - killStat.lastKillTime) > killSpreeTimeout) {
                // We are interested

                Player player = plugin.getServer().getPlayer(playerUUID);
                if (player == null) {
                    continue;
                }

                chatMessage.append(Component.text("Player ", ChatStyles.BASE_STYLE))
                        .append(player.displayName().color(NamedTextColor.DARK_PURPLE))
                        .append(Component.text(" has finished his killing spree with ", ChatStyles.BASE_STYLE))
                        .append(Component.text(killStat.spreeKillCount + " kills!", NamedTextColor.RED));

                Audience.audience(playerList).sendMessage(chatMessage.build());

                killStat.spreeKillCount = 0;
            }
        }

    }

    private List<Player> getPlayersForReducedKillSpreeMessages() {
        List<Player> playerlist = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (MobdeathConfig.getPlayerConfig(player.getUniqueId(), "allKillSpreeMessages")) {
                continue;
            }
            playerlist.add(player);
        }
        return playerlist;
    }
}
