package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TotalDeathMessages extends JavaPlugin {
    FileWatcher configWatcher;

    @Override
    public void onEnable() {
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

}
