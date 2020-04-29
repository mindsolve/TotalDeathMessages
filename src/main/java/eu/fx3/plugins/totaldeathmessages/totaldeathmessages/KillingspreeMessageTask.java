package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static net.md_5.bungee.api.ChatColor.*;

// TODO: JavaDoc this file
// TODO: Rewrite KillSpree end message
// TODO: Make config run-dynamic (non-final)

public class KillingspreeMessageTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final int killSpreeTimeout;
    private List<Player> playerList;

    public KillingspreeMessageTask(JavaPlugin plugin) {
        this.plugin = plugin;
        // Note: This config might change.
        // As we reload the config as soon as something happends, maybe move to loop/run()?
        this.killSpreeTimeout = plugin.getConfig().getInt("killing-spree-timeout");
        playerList = getPlayersForReducedKillSpreeMessages();
    }

    @Override
    public void run() {
        playerList = getPlayersForReducedKillSpreeMessages();
        ComponentBuilder chatMessage = new ComponentBuilder().color(DARK_GRAY);

        for (PlayerKillStats killStats : ((TotalDeathMessages) plugin).playerKillList) {
            if (killStats.spreeKillCount > 2 && (Instant.now().getEpochSecond() - killStats.lastKillTime) > killSpreeTimeout) {
                // We are interested
                chatMessage.append("Player ").append(killStats.playerName).color(DARK_PURPLE);
                chatMessage.append(" has finished his killing spree with ").color(DARK_GRAY);
                chatMessage.append(killStats.spreeKillCount + " kills!").color(RED);
                sendMessage(chatMessage.create());
                killStats.spreeKillCount = 0;
            }
        }

    }

    private void sendMessage(BaseComponent[] message) {
        for (Player player : playerList) {
            player.spigot().sendMessage(message);
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
