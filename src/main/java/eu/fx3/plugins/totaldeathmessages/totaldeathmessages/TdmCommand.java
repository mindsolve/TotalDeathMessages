package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.md_5.bungee.api.ChatColor.*;


/*
TODO:
    Switch/config for displaying a "condensed" message: only first two messages,
    then after the killing spree ended a summary (whoa, player has killed 200000 mobs in his killing spree!)
 */

// TODO: Better descriptions for settings
// TODO: Better visual representation
// TODO: JavaDoc this file


public class TdmCommand implements CommandExecutor {
    static TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();

    // This method is called when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        // Initialize preference if not yet set
        if (!pluginInstance.getConfig().isSet("playerconfig." + player.getUniqueId())) {
            MobdeathConfig.setPlayerConfig(player.getUniqueId(), "allMessages",true);
        }

        if (args.length < 1) {
            return false;
        }

        ComponentBuilder commandResponse = new ComponentBuilder();
        switch (args[0]) {
            case "status":
                commandResponse.append("Mob Death Message Status:\n").underlined(true);
                commandResponse.append("All messages: ").underlined(false);

                if (MobdeathConfig.playerWantsAllMessages(player.getUniqueId())) {
                    commandResponse.append("Enabled").bold(true).color(GREEN);
                } else {
                    commandResponse.append("Disabled").bold(true).color(YELLOW);
                    commandResponse.append("\nTo enable, run ").reset().italic(true).color(WHITE).append("/" + label + " all enable").color(AQUA).bold(true);
                }

                commandResponse.append("\n").reset();
                commandResponse.append("Don't hide killing spree messages: ");

                if (MobdeathConfig.getPlayerConfig(player.getUniqueId(), "allKillSpreeMessages")) {
                    commandResponse.append("Enabled").bold(true).color(GREEN);
                } else {
                    commandResponse.append("Disabled").bold(true).color(YELLOW);
                    commandResponse.append("\nTo enable, run ").reset().italic(true).color(WHITE).append("/" + label + " killspree enable").color(AQUA).bold(true);
                }
                break;

            case "all":
                if (args.length >= 2 && args[1].equals("enable")) {
                    MobdeathConfig.setPlayerConfig(player.getUniqueId(), "allMessages",true);
                    commandResponse.append("Enabled mob death messages!").color(GREEN);
                    break;
                } else if (args.length >= 2 && args[1].equals("disable")) {
                    MobdeathConfig.setPlayerConfig(player.getUniqueId(), "allMessages", false);
                    commandResponse.append("Disabled mob death messages.").color(YELLOW);
                    break;
                } else {
                    return false;
                }

            case "killingspree":
            case "killspree":
                if (args.length >= 2 && args[1].equals("enable")) {
                    MobdeathConfig.setPlayerConfig(player.getUniqueId(), "allKillSpreeMessages",true);
                    commandResponse.append("Showing all Killing Spree messages!").color(GREEN);
                    break;
                } else if (args.length >= 2 && args[1].equals("disable")) {
                    MobdeathConfig.setPlayerConfig(player.getUniqueId(), "allKillSpreeMessages", false);
                    commandResponse.append("Only showing summarized Killing Spree messages.").color(YELLOW);
                    break;
                } else {
                    return false;
                }

            default:
                commandResponse.append("Error:").color(RED).bold(true).append("Unknown subcommand \"" + args[0] + "\"!").bold(false);
                sender.spigot().sendMessage(commandResponse.create());
                return false;
        }

        sender.spigot().sendMessage(commandResponse.create());
        return true;
    }

}
