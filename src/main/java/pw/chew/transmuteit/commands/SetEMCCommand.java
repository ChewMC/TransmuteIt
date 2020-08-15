package pw.chew.transmuteit.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pw.chew.transmuteit.TransmuteIt;

import static pw.chew.transmuteit.utils.I18n.tl;

public class SetEMCCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {

            if(args.length == 0) {
                sender.sendMessage(tl("enter_value"));
                return true;
            }

            int input = Integer.parseInt(args[0]);

            // All this basically is just "Get the held item's name"
            Player player = (Player)sender;
            PlayerInventory inventory = player.getInventory();
            ItemStack item = inventory.getItemInMainHand();
            int amount = item.getAmount();
            Material type = item.getType();
            String name = type.toString();
            // If it's nothing
            if(name.equals("AIR")) {
                sender.sendMessage(tl("hold_item_set"));
            } else {
                // If it's something
                try {
                    if(input > 0) {
                        TransmuteIt.json.put(name, input);
                        sender.sendMessage(tl("item_emc_set").replace("%{name}", name).replace("%{value}", "" + input));
                    } else {
                        TransmuteIt.json.remove(name);
                        sender.sendMessage(tl("emc_removed").replace("%{name}", name));
                    }
                    TransmuteIt.data.writeToEMCFile();
                    // If there's no JSON file or it's not IN the JSON file
                } catch(org.json.JSONException e) {
                    sender.sendMessage(tl("that_item_is").replace("%{amount}", amount + "").replace("%{name}", name));
                }
            }

        } else {
            // Sorry Jimbo, Players only!
            sender.sendMessage("[TransmuteIt] " + tl("only_players"));
        }

        // If the player (or console) uses our command correctly, we can return true
        return true;
    }
}
