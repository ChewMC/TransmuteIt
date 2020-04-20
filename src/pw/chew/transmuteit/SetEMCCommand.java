package pw.chew.transmuteit;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class SetEMCCommand implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {

      if(args.length == 0) {
        sender.sendMessage("Please enter a value! (If you intend to clear the EMC, put 0)");
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
        sender.sendMessage("Please hold an item to set its EMC value!");
      } else {
        // If it's something
        try {
          if(input > 0) {
            TransmuteIt.json.put(name, input);
            sender.sendMessage("Item " + name + "'s EMC Value has been set to " + input);
          } else {
            TransmuteIt.json.remove(name);
            sender.sendMessage("Item " + name + "'s EMC Value has been removed");
          }
          TransmuteIt.data.writeToEMCFile();
          // If there's no JSON file or it's not IN the JSON file
        } catch(org.json.JSONException e) {
          sender.sendMessage("That item is " + amount + " of " + name + ". It has no set EMC value!");
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
