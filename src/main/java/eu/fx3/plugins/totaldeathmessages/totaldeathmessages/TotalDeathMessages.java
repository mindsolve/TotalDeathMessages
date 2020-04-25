package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import eu.fx3.plugins.totaldeathmessages.utils.NMSItem;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TotalDeathMessages extends JavaPlugin {
    FileWatcher configWatcher;
    private static TotalDeathMessages instance;
    private NMSItem nmsItem;

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
            reloadConfig();
        }

        // Register file watcher for config file
        configWatcher = new FileWatcher(configFile, this::updateConfig);
        configWatcher.start();

        // Register command
        this.getCommand("mobdeathmsgs").setExecutor(new MobdeathCommand());
        this.getCommand("mobdeathmsgs").setTabCompleter(new MobdeathCommandTabcomplete());

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
