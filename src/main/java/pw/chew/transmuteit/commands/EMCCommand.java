package pw.chew.transmuteit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pw.chew.transmuteit.utils.DataManager;

import java.text.NumberFormat;

public class EMCCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            long emc = DataManager.getEMC(player);
            sender.sendMessage("You have " + NumberFormat.getInstance().format(emc) + " EMC!");
        } else {
            // Sorry Jimbo, Players only!
            sender.sendMessage("[TransmuteIt] Only players may run this command.");
        }

        // If the player (or console) uses our command correctly, we can return true
        return true;
    }
}
