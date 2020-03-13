package pw.chew.transmuteit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class GetEMCCommand implements CommandExecutor {


  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      Player player = (Player)sender;
      PlayerInventory inventory = player.getInventory();
      ItemStack item = inventory.getItemInMainHand​();
      int amount = item.getAmount();
      Material type = item.getType();
      try {
        int emc = TransmuteIt.json.getInt(type.toString());
        sender.sendMessage("That item is " + amount + " of " + type.toString() + ". EMC Value: " + emc * amount);
      } catch(org.json.JSONException e) {
        sender.sendMessage("That item is " + amount + " of " + type.toString() + ". It has no set EMC value!");
      }




    } else {
      sender.sendMessage("[TransmuteIt] Only players may run this command.");
    }

    // If the player (or console) uses our command correct, we can return true
    return true;
  }
}
