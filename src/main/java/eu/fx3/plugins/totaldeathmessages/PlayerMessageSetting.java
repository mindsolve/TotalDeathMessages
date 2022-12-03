package eu.fx3.plugins.totaldeathmessages;

/**
 * Enum for the three messaging levels players can choose.
 * Used in the config file ("playerconfig") section and internally.
 */
public enum PlayerMessageSetting {
    /**
     * Player wants all messages, including all killing spree messages.
     */
    ALL_MESSAGES("All messages", "all messages, including killing sprees", "green"),

    /**
     * Player wants no messages.
     */
    NO_MESSAGES("No messages", "no mob death or killing spree messages", "red"),

    /**
     * Player wants single mobdeath messages, but no "spammy" killing spree messages.
     * Instead, killing spree ending will result in a single summary message.
     */
    FEWER_MESSAGES("Fewer messages", "fewer messages and only summarized killing sprees", "yellow");

    public final String shortDescription;
    public final String longDescription;
    public final String color;

    private PlayerMessageSetting(String shortDescription, String longDescription, String color) {
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.color = color;
    }
}
