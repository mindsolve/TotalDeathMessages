package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static net.md_5.bungee.api.ChatColor.*;

public class MobdeathCommand implements CommandExecutor {
    static TotalDeathMessages pluginInstance = TotalDeathMessages.getInstance();

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        // Initialize preference if not yet set
        if (!pluginInstance.getConfig().isSet("playerconfig." + player.getUniqueId())) {
            setPlayerConfig(player.getUniqueId(), true);
        }

        if (args.length < 1) {
            return false;
        }

        ComponentBuilder commandResponse = new ComponentBuilder();
        switch (args[0]) {
            case "status":
                if (playerWantsMessages(player.getUniqueId())) {
                    commandResponse.append("You are currently receiving mob death messages.").color(GREEN);
                } else {
                    commandResponse.append("You are currently not receiving mob death messages.\n").color(YELLOW);
                    commandResponse.append("To enable, run ").color(WHITE).append("/" + label + " enable").color(AQUA).bold(true);
                }
                break;

            case "enable":
                setPlayerConfig(player.getUniqueId(), true);
                commandResponse.append("Enabled mob death messages!").color(GREEN);
                break;

            case "disable":
                setPlayerConfig(player.getUniqueId(), false);
                commandResponse.append("Disabled mob death messages.").color(YELLOW);
                break;

            default:
                commandResponse.append("Error:").color(RED).bold(true).append("Unknown subcommand \"" + args[0] + "\"!").bold(false);
                sender.spigot().sendMessage(commandResponse.create());
                return false;
        }

        sender.spigot().sendMessage(commandResponse.create());
        return true;
    }

    static boolean playerWantsMessages(UUID playerUUID) {
        if (!pluginInstance.getConfig().isSet("playerconfig." + playerUUID)) {
            setPlayerConfig(playerUUID, true);
        }
        return pluginInstance.getConfig().getBoolean("playerconfig." + playerUUID);
    }

    static void setPlayerConfig(UUID playerUUID, boolean value) {
        pluginInstance.getConfig().set("playerconfig." + playerUUID, value);
        pluginInstance.saveConfig();
    }
}
