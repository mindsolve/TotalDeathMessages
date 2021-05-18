package eu.fx3.plugins.totaldeathmessages.utils;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;


public class TextComponentHelper {

    /**
     * Helper method to convert a ItemStack to a chat representation, containing its name, item type
     * and additional formatting if it is enchanted.
     *
     * @param itemStack ItemStack to show as text with item-hover
     * @return BaseComponent representation of the item (name + item on hover)
     */
    public static Component itemToComponent(ItemStack itemStack) {
        HoverEvent<?> hoverEvent;
        @NonNull TextComponent message;
        TextColor messageColor = NamedTextColor.BLUE;

        if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
            hoverEvent = itemStack.asHoverEvent();
            ItemMeta meta = itemStack.getItemMeta();

            String itemName = WordUtils.capitalize(itemStack.getType().name().toLowerCase().replace("_", " "));

            if (itemStack.getType() == Material.TRIDENT) {
                itemName = "giant fork";
            }

            message = Component.text(itemName);

            if (meta.hasDisplayName()) {
                Component displayName = meta.displayName();
                assert displayName != null;

                message = message.append(Component.text(" \"" + PlainComponentSerializer.plain().serialize(displayName) + "\""));
            }

            boolean isEnchanted = meta.getEnchants().keySet().size() > 0;
            if (isEnchanted) {
                messageColor = NamedTextColor.AQUA;
            }

        } else {
            hoverEvent = Component.text("Yes, really, his bare hands!").asHoverEvent();
            message = Component.text("bare hands");
        }

        return message
                .color(messageColor)
                .hoverEvent(hoverEvent);
    }

    /**
     * Helper method to convert a ItemStack to a chat representation, containing its name, item type
     * and additional formatting if it is enchanted.
     * <p></p>
     * This method simply converts {@link TextComponentHelper#itemToComponent(ItemStack)}'s Adventure components
     * to Bungeecord chat components as old TextComponents are still used in the code base.
     *
     * @param itemStack ItemStack to show as text with item-hover
     * @return BaseComponent representation of the item (name + item on hover)
     * @see TextComponentHelper#itemToComponent(ItemStack)
     * @deprecated Bungeecord chat components are deprecated by PaperMC, use Adventure components
     */
    @Deprecated
    public static BaseComponent itemToTextComponent(ItemStack itemStack) {
        Component itemComponent = itemToComponent(itemStack);

        BaseComponent[] result = BungeeComponentSerializer.get().serialize(itemComponent);
        return result[0];
    }
}
