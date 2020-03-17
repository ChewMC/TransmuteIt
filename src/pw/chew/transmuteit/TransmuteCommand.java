package pw.chew.transmuteit;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;
import java.util.ArrayList;

public class TransmuteCommand implements CommandExecutor {

  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {

      if(args.length == 0) {
        helpResponse(sender);
      } else if(args.length >= 1) {
        String arg0 = args[0].toLowerCase();
        if(arg0.equals("help")) {
          helpResponse(sender);
        } else if(arg0.equals("get")) {
          if (args.length < 3) {
            sender.sendMessage("This sub-command requires more arguments! Check \"/transmute help\" for more info.");
            return true;
          }
          Player player = (Player)sender;
          UUID uuid = player.getUniqueId();
          String name = args[1].toUpperCase();
          int amount = Integer.parseInt(args[2]);

          ArrayList<String> empty = new ArrayList<String>();
          TransmuteIt.discoveries.putIfAbsent(uuid, empty);

          if(TransmuteIt.discoveries.get(uuid).contains(name)) {
            int emc = TransmuteIt.emc.getOrDefault(uuid, 0);
            int value;
            try {
              value = TransmuteIt.json.getInt(name);
            } catch(org.json.JSONException e) {
              sender.sendMessage("This item no longer has an EMC value!");
              return true;
            }
            if((value * amount) > emc) {
              sender.sendMessage("You don't have enough EMC!");
              return true;
            }

            PlayerInventory inventory = player.getInventory();
            ItemStack item = new ItemStack(Material.getMaterial(name), amount);
            inventory.addItem(item);
            TransmuteIt.emc.replace(uuid, emc - (value * amount));
            sender.sendMessage("Successfully transmuted " + (value * amount) + " EMC into " + amount + " " + name);
            return true;
          } else {
            sender.sendMessage("Uh oh! You don't appear to have discovered " + name + ". Type /getemc to find the exact name.");
            return true;
          }
        } else if(arg0.equals("take")) {
          int takeAmount = Integer.parseInt(args[1]);
          if(takeAmount <= 0) {
            sender.sendMessage("Please select a value greater than 0!");
            return true;
          }
          ItemStack item = ((Player)sender).getInventory().getItemInMainHand();
          int amount = item.getAmount();
          if(amount - takeAmount < 0) {
            sender.sendMessage("You don't have enough of this item!");
            return true;
          }
          Material type = item.getType();
          String name = type.toString();
          // If it's nothing
          if(name.equals("AIR")) {
            sender.sendMessage("Please hold an item to transmute it!");
          } else {
            // If it's something
            try {
              int emc = TransmuteIt.json.getInt(type.toString());
              item.setAmount(amount - takeAmount);
              UUID uuid = ((Player)sender).getUniqueId();
              int current = TransmuteIt.emc.getOrDefault(uuid, 0);
              int newEMC = current + (takeAmount * emc);
              TransmuteIt.emc.putIfAbsent(uuid, 0);
              TransmuteIt.emc.replace(uuid, newEMC);
              ArrayList<String> empty = new ArrayList<String>();
              TransmuteIt.discoveries.putIfAbsent(uuid, empty);
              if(TransmuteIt.discoveries.get(uuid).contains(name)) {
                sender.sendMessage("Successfully transmuted " + takeAmount + " " + name + "! You now have " + newEMC + " EMC");
              } else {
                sender.sendMessage("You've discovered " + name + "! Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                sender.sendMessage("Successfully transmuted " + takeAmount + " " + name + "! You now have " + newEMC + " EMC");
                TransmuteIt.discoveries.get(uuid).add(name);
              }

              // If there's no JSON file or it's not IN the JSON file
            } catch(org.json.JSONException e) {
              sender.sendMessage("This item has no set EMC value!");
            }
          }
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
    sender.sendMessage("§dWelcome to TransmuteIt!");
    sender.sendMessage("§b/transmute take [amount] §d- Take [amount] of held item and convert to EMC.");
    sender.sendMessage("§b/transmute get [item] [amount] §d- Get amount of item using EMC.");
    sender.sendMessage("§b/transmute help §d- This command.");
    sender.sendMessage("§b/getEMC §d- Get the EMC value of held item.");
  }
}
