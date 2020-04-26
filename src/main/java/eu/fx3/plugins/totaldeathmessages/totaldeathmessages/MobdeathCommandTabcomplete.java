package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MobdeathCommandTabcomplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length > 1) {
            return completions;
        }

        completions.add("status");

        if (MobdeathConfig.playerWantsAllMessages(player.getUniqueId())) {
            completions.add("disable");
        } else {
            completions.add("enable");
        }

        return completions;
    }
}