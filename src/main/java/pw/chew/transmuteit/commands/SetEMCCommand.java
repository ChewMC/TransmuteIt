package pw.chew.transmuteit.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import pw.chew.transmuteit.utils.ChatHelper;
import pw.chew.transmuteit.utils.DataManager;

public class SetEMCCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {

            if (args.length == 0) {
                return ChatHelper.sendError(sender, "Please enter a value! (If you intend to clear the EMC, put 0)");
            }

            int input = 0;
            try {
                input = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                return ChatHelper.sendError(sender, "Please enter a valid number!");
            }

            // All this basically is just "Get the held item's name"
            PlayerInventory inventory = player.getInventory();
            ItemStack item = inventory.getItemInMainHand();
            int amount = item.getAmount();
            Material type = item.getType();
            String name = type.toString();
            // If it's nothing
            if(type.isAir()) {
                sender.sendMessage("Please hold an item to set its EMC value!");
            } else {
                // If it's something
                try {
                    DataManager.setEMCValue(name, input);
                    if(input > 0) {
                        sender.sendMessage("Item " + name + "'s EMC Value has been set to " + input);
                    } else {
                        sender.sendMessage("Item " + name + "'s EMC Value has been removed");
                    }
                    // If there's no JSON file or it's not IN the JSON file
                } catch(JSONException e) {
                    ChatHelper.sendError(sender, "That item is %s of %s. It has no set EMC value!", amount, name);
                }
            }

        } else {
            // Sorry Jimbo, Players only!
            sender.sendMessage("[TransmuteIt] Only players may run this command.");
        }

        // If the player (or console) uses our command correctly, we can return true
        return true;
    }
}
