package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.sections.FlatFileSection;

import eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MobdeathConfig {
    /**
     * This variable contains the current configuration version.
     * For each breaking change, this version is incremented
     */
    public static final int CONFIG_VERSION = 3;

    /** Reference to main plugin class */
    static final TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();
    /** Reference to plugin-global config */
    static final Yaml config = pluginInstance.getPluginConfig();
    /** Config section key with player configuration */
    public static final String PLAYERCONFIG_KEY = "playerconfig";
    /** Config key for single player message setting under playerconfig setting */
    public static final String PLAYER_MESSAGE_SETTING_KEY = "message-setting";

    /**
     * Private constuctor; static utility classes should not be instantiated.
     */
    private MobdeathConfig() {
        throw new IllegalStateException("This class should not be instantiated.");
    }

    static PlayerMessageSetting getPlayerMessageSetting(UUID playerUUID) {
        PlayerMessageSetting messageSetting = getUserSection(playerUUID).getEnum(PLAYER_MESSAGE_SETTING_KEY, PlayerMessageSetting.class);

        if (messageSetting == null) {
            messageSetting = PlayerMessageSetting.ALL_MESSAGES;
            getUserSection(playerUUID).set(PLAYER_MESSAGE_SETTING_KEY, messageSetting);
        }

        return messageSetting;
    }

    static void setPlayerMessageSetting(UUID playerUUID, PlayerMessageSetting value) {
        getUserSection(playerUUID).set(PLAYER_MESSAGE_SETTING_KEY, value);
    }

    static FlatFileSection getUserSection(UUID playerUUID) {
        String sectionPath = PLAYERCONFIG_KEY + "." + playerUUID;

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
                    for (String playerUUIDString : config.getSection(PLAYERCONFIG_KEY).singleLayerKeySet()) {
                        FlatFileSection playerSection = config.getSection(PLAYERCONFIG_KEY + "." + playerUUIDString);
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
                    config.remove(PLAYERCONFIG_KEY);

                    // Write new config section
                    for (Map.Entry<String, PlayerMessageSetting> entry : stateMap.entrySet()) {
                        config.set(PLAYERCONFIG_KEY + "." + entry.getKey() + ".message-setting", entry.getValue());
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

