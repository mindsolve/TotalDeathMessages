package eu.fx3.plugins.totaldeathmessages.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eu.fx3.plugins.totaldeathmessages.utils.ReflectionUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NMSItem {
    public NMSItem() {
    }

    public String itemToJson(ItemStack itemStack) {
        Class<?> craftItemStackClass = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClass, "asNMSCopy", new Class[]{ItemStack.class});
        Class<?> nmsItemStackClass = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClass = ReflectionUtil.getNMSClass("NBTTagCompound");
        Method saveNMSItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClass, "save", new Class[]{nbtTagCompoundClass});

        Object itemAsJsonObject;
        try {
            Object nmsNBTTagCompoundObject = nbtTagCompoundClass.newInstance();
            Object nmsItemStackObject = asNMSCopyMethod.invoke((Object) null, itemStack);
            itemAsJsonObject = saveNMSItemStackMethod.invoke(nmsItemStackObject, nmsNBTTagCompoundObject);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException var11) {
            var11.printStackTrace();
            return null;
        }

        return itemAsJsonObject.toString();
    }

    public TextComponent itemToTextComponent(ItemStack itemStack) {
        HoverEvent event;
        String message;
        ChatColor messageColor = ChatColor.BLUE;
        TextComponent itemComponent = new TextComponent();


        if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
            String jsonItem = this.itemToJson(itemStack);
            BaseComponent[] hoverEventComponents = new BaseComponent[]{new TextComponent(jsonItem)};
            event = new HoverEvent(Action.SHOW_ITEM, hoverEventComponents);
            int amount = itemStack.getAmount();
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

            messageColor = (isEnchanted ? ChatColor.AQUA : ChatColor.BLUE);
            message = name;

        } else {
            event = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Yes, really, his bare hands!")});
            message = " bare hands";
        }

        itemComponent = new TextComponent(message);
        itemComponent.setColor(messageColor);

        itemComponent.setHoverEvent(event);
        return itemComponent;
    }
}
