package eu.fx3.plugins.totaldeathmessages.settingutils;

/**
 * Enum for the three messaging levels players can choose.
 * Used in the config file ("playerconfig") section and internally.
 */
public enum PlayerMessageSetting {
    /**
     * Player wants all messages, including all killing spree messages.
     */
    ALL_MESSAGES,

    /**
     * Player wants no messages.
     */
    NO_MESSAGES,

    /**
     * Player wants single mobdeath messages, but no "spammy" killing spree messages.
     * Instead, killing spree ending will result in a single summary message.
     */
    FEWER_MESSAGES
}
