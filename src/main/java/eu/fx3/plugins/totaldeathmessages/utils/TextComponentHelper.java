package eu.fx3.plugins.totaldeathmessages.utils;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;

import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class TextComponentHelper {

    public static BaseComponent itemToTextComponent(ItemStack itemStack) {
        HoverEvent<?> event;
        String message;
        TextColor messageColor = NamedTextColor.BLUE;


        if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
            event = itemStack.asHoverEvent();
            ItemMeta meta = itemStack.getItemMeta();
            String name;

            String itemName = WordUtils.capitalize(itemStack.getType().name().toLowerCase().replaceAll("_", " "));

            if (itemStack.getType() == Material.TRIDENT) {
                itemName = "giant derpy fork";
            }

            if (meta.hasDisplayName()) {
                name = itemName + " \"" + meta.getDisplayName() + "\"";
            } else {
                name = itemName;
            }

            boolean isEnchanted = meta.getEnchants().keySet().size() > 0;

            if (isEnchanted) messageColor = NamedTextColor.AQUA;
            message = name;

        } else {
            event = Component.text("Yes, really, his bare hands!").asHoverEvent();
            message = " bare hands";
        }

        Component itemComponent = Component
                .text(message)
                .color(messageColor)
                .hoverEvent(event);

        BaseComponent[] result = BungeeComponentSerializer.get().serialize(itemComponent);
        return result[0];
    }
}
