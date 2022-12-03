package eu.fx3.plugins.totaldeathmessages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.fx3.plugins.totaldeathmessages.PlayerMessageSetting.*;

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
        if (!(sender instanceof Player player)) {
            return false;
        }

        // Display usage info
        if (args.length < 1 || args.length > 2) {
            return false;
        }

        MiniMessage miniMessageBuilder = MiniMessage.miniMessage();
        StringBuilder commandResponse = new StringBuilder();

        switch (args[0]) {
            case "status" -> {
                PlayerMessageSetting setting = MobdeathConfig.getPlayerMessageSetting(player.getUniqueId());

                commandResponse
                        .append("<u>Total Death Messages:</u>\n")
                        .append("Showing: <b><%s>%s<reset>\n".formatted(setting.color, setting.shortDescription))
                        .append("Showing %s.".formatted(setting.longDescription));
            }
            case "all" -> {
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), ALL_MESSAGES);
                commandResponse.append("<green>Enabled all mob death messages!</green>");
            }
            case "fewer", "summarized" -> {
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), FEWER_MESSAGES);
                commandResponse.append("<green>Enabled reduced mob death message count.</green>\n")
                        .append("You will still receive single mob death messages, but no killing spree spam anymore.");
            }
            case "off", "none" -> {
                MobdeathConfig.setPlayerMessageSetting(player.getUniqueId(), NO_MESSAGES);
                commandResponse.append("<yellow>Disabled mob death messages.</yellow>\n")
                        .append("You will no longer receive mob death messages.\n" +
                                "You can re-enable them anytime with <click:suggest_command:'/tdm all'><green>/tdm all</green></click> <i>(for all messages)</i> or <click:suggest_command:'/tdm fewer'><green>/tdm fewer</green></click> <i>(for summarized messages)</i>.");
            }
            default -> {
                commandResponse.append("<b><red>Error:</b> Unknown subcommand \"%s\"!</red>".formatted(args[0]));

                sender.sendMessage(miniMessageBuilder.deserialize(commandResponse.toString()));
                return false;
            }
        }

        sender.sendMessage(miniMessageBuilder.deserialize(commandResponse.toString()));
        return true;
    }

}
