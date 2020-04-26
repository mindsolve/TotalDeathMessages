package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;

public class MobdeathConfig {
    /**
     * This variable contains the current configuration version.
     * For each breaking change, this version is incremented
     */
    public static final int CONFIG_VERSION = 2;

    // Reference to main plugin class
    static TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();

    static boolean playerWantsAllMessages(UUID playerUUID) {
        return getPlayerConfig(playerUUID, "allMessages");
    }

    static boolean getPlayerConfig(UUID playerUUID, String setting) {
        if (!getUserSection(playerUUID).isSet(setting)) {
            setPlayerConfig(playerUUID, setting,true);
        }
        return getUserSection(playerUUID).getBoolean(setting);
    }

    static void setPlayerConfig(UUID playerUUID, String setting, boolean value) {
        getUserSection(playerUUID).set(setting, value);
        pluginInstance.saveConfig();
    }

    static ConfigurationSection getUserSection(UUID playerUUID) {
        String sectionPath = "playerconfig." + playerUUID;

        // Create Section, if it doesn't exist
        if (!pluginInstance.getConfig().isConfigurationSection(sectionPath)) {
            pluginInstance.getConfig().createSection(sectionPath);
        }

        return pluginInstance.getConfig().getConfigurationSection(sectionPath);
    }

    static void upgradeConfigVersion(int oldVersion) {
        switch (oldVersion) {
            case CONFIG_VERSION:
                pluginInstance.getLogger().info("No config update necessary (current version).");
                break;

            case 1:
                ConfigurationSection playerSection = pluginInstance.getConfig().getConfigurationSection("playerconfig");
                if (playerSection == null) {
                    pluginInstance.getConfig().createSection("playerconfig");
                    pluginInstance.getConfig().set("config-version", 2);
                    pluginInstance.saveConfig();
                    return;
                }

                Set<String> playerUUIDs = playerSection.getKeys(false);
                for (String section : playerUUIDs) {
                    setPlayerConfig(UUID.fromString(section), "allMessages", playerSection.getBoolean(section));
                }
                pluginInstance.getConfig().set("config-version", 2);
                pluginInstance.saveConfig();
                return;

            default:
                throw new IllegalArgumentException("Unknown config version!");

        }
    }

}

