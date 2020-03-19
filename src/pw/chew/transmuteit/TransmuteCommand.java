package pw.chew.transmuteit;
import java.util.Arrays;
import org.bukkit.Bukkit;
import java.util.Collections;
import org.bukkit.util.StringUtil;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class TransmuteCommand implements CommandExecutor, TabCompleter {

  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      Player player = (Player)sender;
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
          UUID uuid = player.getUniqueId();
          String name = args[1].toUpperCase();
          int amount = Integer.parseInt(args[2]);

          if(new DataManager().discovered(uuid, name)) {
            int emc = new DataManager().getEMC(uuid, player);
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
            DataManager bob = new DataManager();
            bob.writeEMC(uuid, emc - (value * amount), player);
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
              DataManager bob = new DataManager();
              int emc = TransmuteIt.json.getInt(type.toString());
              item.setAmount(amount - takeAmount);
              UUID uuid = ((Player)sender).getUniqueId();
              int current = new DataManager().getEMC(uuid, player);
              int newEMC = current + (takeAmount * emc);
              bob.writeEMC(uuid, newEMC, player);
              if(bob.discovered(uuid, name) == false) {
                sender.sendMessage("You've discovered " + name + "!");
                if(bob.discoveries(uuid).size() == 0) {
                  sender.sendMessage("Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                }
                new DataManager().writeDiscovery(uuid, name);
              }
              sender.sendMessage("Successfully transmuted " + takeAmount + " " + name + " into EMC!");
              sender.sendMessage("You now have " + newEMC + " EMC (+" + (takeAmount * emc) + " EMC)");
              return true;
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

  @Override
  public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
    List<String> completions = new ArrayList<>();
    List<String> commands = new ArrayList<>();

    ((TransmuteIt)Bukkit.getPluginManager().getPlugin("TransmuteIt")).getLogger().info(args.length + ": " + Arrays.toString(args));

    if (args.length == 1) {
      commands.add("help");
      commands.add("get");
      commands.add("take");
      StringUtil.copyPartialMatches(args[0], commands, completions);
    } else if (args.length >= 2 && args[0].equals("help")) {
      StringUtil.copyPartialMatches(args[1], commands, completions);
    } else if (args[0].equals("get")) {
      if(args.length == 2) {
        List<Object> discoveries = new DataManager().discoveries(((Player)sender).getUniqueId());
        for(int i = 0; i < discoveries.size(); i++) {
          commands.add(discoveries.get(i).toString());
        }
      } else {
      }
      StringUtil.copyPartialMatches(args[1], commands, completions);
    }
    Collections.sort(completions);
    return completions;
  }

  public void helpResponse(CommandSender sender) {
    sender.sendMessage("§dWelcome to TransmuteIt!");
    sender.sendMessage("§b/transmute take [amount] §d- Take [amount] of held item and convert to EMC.");
    sender.sendMessage("§b/transmute get [item] [amount] §d- Get amount of item using EMC.");
    sender.sendMessage("§b/transmute help §d- This command.");
    sender.sendMessage("§b/getEMC §d- Get the EMC value of held item.");
  }
}
