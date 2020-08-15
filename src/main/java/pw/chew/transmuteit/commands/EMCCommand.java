package pw.chew.transmuteit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.chew.transmuteit.DataManager;

import java.text.NumberFormat;
import java.util.UUID;

import static pw.chew.transmuteit.utils.I18n.tl;

public class EMCCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            UUID uuid = player.getUniqueId();
            int emc = new DataManager().getEMC(uuid, player);
            sender.sendMessage(tl("you_have").replace("%{emc}", NumberFormat.getInstance().format(emc)));
        } else {
            // Sorry Jimbo, Players only!
            sender.sendMessage("[TransmuteIt] " + tl("only_players"));
        }

        // If the player (or console) uses our command correctly, we can return true
        return true;
    }
}
