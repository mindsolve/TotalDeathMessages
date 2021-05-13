package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.fx3.plugins.totaldeathmessages.settingutils.PlayerMessageSetting.*;
import static net.md_5.bungee.api.ChatColor.*;

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
    static TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();

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

        ComponentBuilder commandResponse = new ComponentBuilder();
        switch (args[0]) {
            case "status":
                commandResponse.append("Total Death Messages:\n").underlined(true);
                commandResponse.append("Showing: ").underlined(false);

                if (MobdeathConfig.getPlayerMessageSetting(player.getUniqueId()) == ALL_MESSAGES) {
                    commandResponse.append("All messages").bold(true).color(GREEN);
                    commandResponse.append("\n").reset();
                    commandResponse.append("Showing all messages, including killing sprees.");
                } else if (MobdeathConfig.getPlayerMessageSetting(player.getUniqueId()) == FEWER_MESSAGES) {
                    commandResponse.append("Fewer messages").bold(true).color(YELLOW);
                    commandResponse.append("\n").reset();
                    commandResponse.append("Showing fewer messages and only summarized killing sprees.");
                } else {
                    commandResponse.append("No messages").bold(true).color(RED);
                    commandResponse.append("\n").reset();
                    commandResponse.append("Showing no mob death or killing spree messages.");
                }
                break;

            case "all":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), ALL_MESSAGES);
                commandResponse.append("Enabled all mob death messages!").color(GREEN);
                break;

            case "fewer":
            case "summarized":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), FEWER_MESSAGES);
                commandResponse.append("Enabled reduced mob death message count.").color(GREEN)
                        .append("\n").reset()
                        .append("You will still receive single mob death messages, but no killing spree spam anymore.");
                break;

            case "off":
            case "none":
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), NO_MESSAGES);
                commandResponse.append("Disabled mob death messages.").color(YELLOW)
                        .append("\n").reset()
                        .append("You will no longer receive mob death messages.");
                break;

            default:
                commandResponse.append("Error:").color(RED).bold(true).append("Unknown subcommand \"" + args[0] + "\"!").bold(false);
                sender.spigot().sendMessage(commandResponse.create());
                return false;
        }

        sender.spigot().sendMessage(commandResponse.create());
        return true;
    }

}
