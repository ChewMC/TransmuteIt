package pw.chew.transmuteit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class GetEMCCommand implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      // All this basically is just "Get the held item's name"
      Player player = (Player)sender;
      PlayerInventory inventory = player.getInventory();
      ItemStack item = inventory.getItemInMainHand();
      int amount = item.getAmount();
      Material type = item.getType();
      String name = type.toString();
      // If it's nothing
      if(name.equals("AIR")) {
        sender.sendMessage("Please hold an item to find its EMC value!");
      } else {
        // If it's something
        try {
          int emc = TransmuteIt.json.getInt(type.toString());
          sender.sendMessage("That item is " + amount + " of " + name + ". Stack EMC Value: " + emc * amount + " (" + amount + " @ " + emc + " EMC each)");
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
