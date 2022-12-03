package eu.fx3.plugins.totaldeathmessages;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

/**
 * Static utility class for Adventure chat styles
 */
public class ChatStyles {
    private ChatStyles() {
        throw new IllegalStateException("This class should not be instantiated.");
    }

    public static final Style BASE_STYLE = Style.style(NamedTextColor.DARK_GRAY);
}
