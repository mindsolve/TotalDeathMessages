package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.settings.ConfigSettings;
import eu.fx3.plugins.totaldeathmessages.utils.NMSItem;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public final class TotalDeathMessages extends JavaPlugin {
    private Yaml config;
    private static TotalDeathMessages instance;
    private NMSItem nmsItem;
    private TDMGlobalSettings globalSettings;

    // TODO: Cleanup list (offline players)
    public HashMap<UUID, PlayerKillStats> playerKillStats = new HashMap<>();

    @Override
    public void onEnable() {
        // Save object reference
        instance = this;

        // Create NSMItem object
        nmsItem = new NMSItem();

        // Create TDMGlobalSettings object
        globalSettings = new TDMGlobalSettings();

        // Register EventListener for EntityDeathEvent
        getServer().getPluginManager().registerEvents(new EntityDeathListener(), this);
        // Register EventListener for ProjectileLaunchEvent
        getServer().getPluginManager().registerEvents(new ProjectileLaunchListener(), this);

        // Copy default config if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            if (!getDataFolder().isDirectory() && !getDataFolder().mkdir()) {
                getLogger().warning("The configuration directory could not be created!");
            }
            saveDefaultConfig();
            getLogger().info("Default configuration created!");
        }

        // Initialise config
        config = new Yaml(configFile);
        // Set config mode
        config.setConfigSettings(ConfigSettings.PRESERVE_COMMENTS);

        MobdeathConfig.upgradeConfigVersion(config.getOrDefault("config-version", 1));

        PluginCommand tdmCommand = this.getCommand("tdm");
        assert tdmCommand != null;

        // Register command
        tdmCommand.setExecutor(new TdmCommand());
        // Register Tab-Complete for command
        tdmCommand.setTabCompleter(new TdmCommandTabcomplete());

        // Start KillingSpree Timer to fire every 5 seconds
        new KillingspreeMessageTask(this).runTaskTimer(this, 25 * 5, 25 * 5);

        // Log success
        getLogger().info(ChatColor.GREEN + "Plugin successfully initialized!");

    }

    @Override
    public void onDisable() {

    }

    protected void updateConfig() {
        getLogger().info("Configuration reloaded.");
        reloadConfig();
    }

    public static TotalDeathMessages getInstance() {
        return instance;
    }

    public NMSItem getNmsItem() {
        return this.nmsItem;
    }

    public TDMGlobalSettings getGlobalSettings() {
        return this.globalSettings;
    }

    @NotNull
    public Yaml getPluginConfig() {
        return config;
    }
}
