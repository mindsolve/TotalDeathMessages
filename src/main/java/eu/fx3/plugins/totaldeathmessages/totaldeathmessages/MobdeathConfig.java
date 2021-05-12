package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.sections.FlatFileSection;

import java.util.UUID;

/*
 TODO: Find better upgrade method
    Currently, we upgrade from version x to the current version directly.
    This would force us to create a direct conversion between every version and the current one.
    Maybe we could instead upgrade to the next version? Then we only need a upgrade path between two versons
*/

public class MobdeathConfig {
    /**
     * This variable contains the current configuration version.
     * For each breaking change, this version is incremented
     */
    public static final int CONFIG_VERSION = 2;

    // Reference to main plugin class
    static TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();
    // Reference to global config
    static Yaml config = pluginInstance.getPluginConfig();

    static boolean playerWantsAllMessages(UUID playerUUID) {
        return getPlayerConfig(playerUUID, "allMessages");
    }

    static boolean getPlayerConfig(UUID playerUUID, String setting) {
        return getUserSection(playerUUID).getOrSetDefault(setting, true);
    }

    static void setPlayerConfig(UUID playerUUID, String setting, boolean value) {
        getUserSection(playerUUID).set(setting, value);
    }

    static FlatFileSection getUserSection(UUID playerUUID) {
        String sectionPath = "playerconfig." + playerUUID;

        return config.getSection(sectionPath);
    }

    /**
     * Updates the plugin config file from a previous version, if needed
     *
     * @param oldVersion The old plugin config version number
     */
    static void upgradeConfigVersion(int oldVersion) {
        switch (oldVersion) {
            case CONFIG_VERSION:
                pluginInstance.getLogger().info("No config update necessary (current version).");
                break;

            default:
                throw new IllegalArgumentException("Unknown config version!");

        }
    }

}

