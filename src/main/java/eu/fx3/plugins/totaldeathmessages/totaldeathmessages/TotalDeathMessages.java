package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import eu.fx3.plugins.totaldeathmessages.utils.NMSItem;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class TotalDeathMessages extends JavaPlugin {
    FileWatcher configWatcher;
    private static TotalDeathMessages instance;
    private NMSItem nmsItem;
    public List<PlayerKillStats> playerKillList = new ArrayList<PlayerKillStats>();

    @Override
    public void onEnable() {
        // Save object reference
        instance = this;

        // Create NSMItem object
        nmsItem = new NMSItem();

        // Register EventListener
        getServer().getPluginManager().registerEvents(new EntityDeathListener(), this);

        // Copy default config if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdir();
            saveDefaultConfig();
            getLogger().info("Standard-Konfiguration erstellt!");
        } else {
            MobdeathConfig.upgradeConfigVersion(getConfig().getInt("config-version", 1));
            reloadConfig();
        }

        // Register file watcher for config file
        configWatcher = new FileWatcher(configFile, this::updateConfig);
        configWatcher.start();


        PluginCommand mobdeathmsgs = this.getCommand("mobdeathmsgs");
        assert mobdeathmsgs != null;

        // Register command
        mobdeathmsgs.setExecutor(new MobdeathCommand());
        // Register Tab-Complete for command
        mobdeathmsgs.setTabCompleter(new MobdeathCommandTabcomplete());

        // Start KillingSpree Timer
        BukkitTask task = new KillingspreeMessageTask(this).runTaskTimer(this, 25*5, 25*5);

        // Log success
        getLogger().info(ChatColor.GREEN + "Plugin erfolgreich initialisiert!");

    }

    @Override
    public void onDisable() {
        // Stop file watcher thread
        configWatcher.stopThread();
    }

    protected void updateConfig() {
        getLogger().info("Reloaded config.");
        reloadConfig();
    }

    public static TotalDeathMessages getInstance() {
        return instance;
    }

    public NMSItem getNmsItem() {
        return this.nmsItem;
    }

}
