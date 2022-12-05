package eu.fx3.plugins.totaldeathmessages;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

// TODO: Check if Projectile launches also require cleanup

/**
 * Task for cleaning up all plugin-owned lists, such as the {@link TotalDeathMessages#playerKillStats}.
 */
class CleanupTask extends BukkitRunnable {
    private final JavaPlugin plugin;

    public CleanupTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Iterator<Map.Entry<UUID, PlayerKillStats>> entryIterator = ((TotalDeathMessages) plugin).playerKillStats.entrySet().iterator();

        int removedEntryCount = 0;
        while (entryIterator.hasNext()) {
            Map.Entry<UUID, PlayerKillStats> entry = entryIterator.next();
            UUID playerUUID = entry.getKey();

            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                removedEntryCount++;
                entryIterator.remove();
            }
        }

        if (removedEntryCount > 0 && plugin.getLogger().isLoggable(Level.FINE)) {
            plugin.getLogger().fine(MessageFormat.format("Removed {0} offline players from playerKillStats.", removedEntryCount));
        }
    }

}
