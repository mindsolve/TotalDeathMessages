package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.sections.FlatFileSection;
import eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting;

import java.util.HashMap;
import java.util.UUID;


public class MobdeathConfig {
    /**
     * This variable contains the current configuration version.
     * For each breaking change, this version is incremented
     */
    public static final int CONFIG_VERSION = 3;

    // Reference to main plugin class
    static final TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();
    // Reference to global config
    static final Yaml config = pluginInstance.getPluginConfig();

    /**
     * TODO: Remove hacky workaround for enum conversion
     */
    static PlayerMessageSetting getPlayerMessageSetting(UUID playerUUID) {
        Object messageSetting = getUserSection(playerUUID).getOrSetDefault("message-setting", PlayerMessageSetting.ALL_MESSAGES);
        //noinspection ConstantConditions
        if (messageSetting instanceof String) {
            pluginInstance.getLogger().warning("[config] returned string, not enum");
            messageSetting = PlayerMessageSetting.valueOf((String) messageSetting);
        }
        return (PlayerMessageSetting) messageSetting;
    }

    static void setPlayerMessageSetting(UUID playerUUID, PlayerMessageSetting value) {
        getUserSection(playerUUID).set("message-setting", value);
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
        while (oldVersion != CONFIG_VERSION) {
            switch (oldVersion) {
                case 2:
                    pluginInstance.getLogger().info("Migrating config from version 2...");

                    // Save all data in hashmap
                    HashMap<String, PlayerMessageSetting> stateMap = new HashMap<>();
                    for (String playerUUIDString : config.getSection("playerconfig").singleLayerKeySet()) {
                        FlatFileSection playerSection = config.getSection("playerconfig." + playerUUIDString);
                        boolean wantsAllMessages = playerSection.getOrDefault("allMessages", true);
                        boolean wantsKillSpreeMessages = playerSection.getOrDefault("allKillSpreeMessages", true);

                        PlayerMessageSetting state = PlayerMessageSetting.ALL_MESSAGES;
                        if (!wantsAllMessages) {
                            state = PlayerMessageSetting.NO_MESSAGES;
                        } else if (!wantsKillSpreeMessages) {
                            state = PlayerMessageSetting.FEWER_MESSAGES;
                        }

                        stateMap.put(playerUUIDString, state);
                    }

                    // Remove old config section
                    config.remove("playerconfig");

                    // Write new config section
                    for (String playerUUIDstr : stateMap.keySet()) {
                        config.set("playerconfig." + playerUUIDstr + ".message-setting", stateMap.get(playerUUIDstr));
                    }

                    config.set("config-version", 3);

                    pluginInstance.getLogger().info("Config migration from 2 -> 3 finished.");
                    oldVersion = config.getInt("config-version");
                    break;

                default:
                    throw new IllegalArgumentException("Unknown config version!");

            }
        }

    }

}

