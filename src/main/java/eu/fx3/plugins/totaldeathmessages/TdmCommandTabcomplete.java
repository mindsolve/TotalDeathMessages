package eu.fx3.plugins.totaldeathmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TdmCommandTabcomplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length <= 1) {
            completions.add("status");
            completions.add("all");
            completions.add("fewer");
            completions.add("off");
        }

        return completions;
    }
}