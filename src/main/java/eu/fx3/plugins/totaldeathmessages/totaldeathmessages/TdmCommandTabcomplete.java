package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TdmCommandTabcomplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length <= 1) {
            completions.add("status");
            completions.add("all");
            completions.add("killspree");
        } else if (args.length == 2) {

            switch (args[0]) {
                case "status":
                    return completions;

                case "all":
                    if (MobdeathConfig.playerWantsAllMessages(player.getUniqueId())) {
                        completions.add("disable");
                    } else {
                        completions.add("enable");
                    }

                    break;
                case "killspree":
                case "killingspree":
                    if (MobdeathConfig.getPlayerConfig(player.getUniqueId(), "allKillSpreeMessages")) {
                        completions.add("disable");
                    } else {
                        completions.add("enable");
                    }

                    break;
            }

        }


        return completions;
    }
}