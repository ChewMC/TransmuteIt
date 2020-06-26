package pw.chew.transmuteit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.chew.transmuteit.DataManager;

import java.text.NumberFormat;
import java.util.UUID;

public class EMCCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            UUID uuid = player.getUniqueId();
            int emc = new DataManager().getEMC(uuid, player);
            sender.sendMessage("You have " + NumberFormat.getInstance().format(emc) + " EMC!");
        } else {
            // Sorry Jimbo, Players only!
            sender.sendMessage("[TransmuteIt] Only players may run this command.");
        }

        // If the player (or console) uses our command correctly, we can return true
        return true;
    }
}
