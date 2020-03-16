package pw.chew.transmuteit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class TransmuteCommand implements CommandExecutor {

  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {

      if(args.length == 0) {
        helpResponse(sender);
      } else if(args.length >= 1) {
        String arg0 = args[0].toLowerCase();
        if(arg0 == "help") {
          helpResponse(sender);
        } else if(arg0 == "get") {
          sender.sendMessage("This sub-command hasn't been implemented yet!");
        } else if(arg0 == "take") {
          sender.sendMessage("This sub-command hasn't been implemented yet!");
        } else {
          sender.sendMessage("Invalid subcommand! Need help? Try \"/transmute help\"");
        }
      }


    } else {
      sender.sendMessage("[TransmuteIt] Only players may run this command.");
    }

    // If the player (or console) uses our command correct, we can return true
    return true;
  }

  public void helpResponse(CommandSender sender) {
    sender.sendMessage("&cWelcome to TransmuteIt!");
    sender.sendMessage("&b/transmute take [amount] &c- Take [amount] of held item and convert to EMC.");
    sender.sendMessage("&b/transmute get [item] [amount] &c- Get amount of item using EMC.");
    sender.sendMessage("&b/transmute help &c- This command.");
    sender.sendMessage("&b/getEMC &c- Get the EMC value of held item.");
  }
}
