package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting.*;

// TODO: Better descriptions for settings
// TODO: Better visual representation
// TODO: JavaDoc this file

/*
Three states:
- "ALL_MESSAGES": All messages [all mob deaths, all killing spree messages]
- "FEWER_MESSAGES": condensed killing spree messages [first two spree messages, killspree end summary]
- "NO_MESSAGES": No messages
 */

public class TdmCommand implements CommandExecutor {
    // This method is called when somebody uses our command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        // Display usage info
        if (args.length < 1 || args.length > 2) {
            return false;
        }

        TextComponent.Builder newCommandResponse = Component.text();
        switch (args[0]) {
            case "status":
                newCommandResponse
                        .append(Component.text("Total Death Messages:\n").decorate(TextDecoration.UNDERLINED))
                        .append(Component.text("Showing: "));

                if (MobdeathConfig.getPlayerMessageSetting(player.getUniqueId()) == ALL_MESSAGES) {
                    newCommandResponse
                            .append(Component.text("All messages").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                            .append(Component.text("\nShowing all messages, including killing sprees."));

                } else if (MobdeathConfig.getPlayerMessageSetting(player.getUniqueId()) == FEWER_MESSAGES) {
                    newCommandResponse
                            .append(Component.text("Fewer messages").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                            .append(Component.text("\nShowing fewer messages and only summarized killing sprees."));

                } else {
                    newCommandResponse
                            .append(Component.text("No messages").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                            .append(Component.text("\nShowing no mob death or killing spree messages."));

                }
                break;

            case "all":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), ALL_MESSAGES);
                newCommandResponse
                        .append(Component.text("Enabled all mob death messages!")
                                .color(NamedTextColor.GREEN));

                break;

            case "fewer":
            case "summarized":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), FEWER_MESSAGES);
                newCommandResponse
                        .append(Component.text("Enabled reduced mob death message count.")
                                .color(NamedTextColor.GREEN))
                        .append(Component.text("\nYou will still receive single mob death messages, but no killing spree spam anymore."));

                break;

            case "off":
            case "none":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), NO_MESSAGES);
                newCommandResponse
                        .append(Component.text("Disabled mob death messages.")
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text("\nYou will no longer receive mob death messages."));

                break;

            default:
                newCommandResponse
                        .append(Component.text("Error: ").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                        .append(Component.text("Unknown subcommand \"" + args[0] + "\"!").color(NamedTextColor.RED));

                sender.sendMessage(newCommandResponse);
                return false;
        }

        sender.sendMessage(newCommandResponse);
        return true;
    }

}
