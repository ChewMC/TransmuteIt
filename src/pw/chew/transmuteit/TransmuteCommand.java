package pw.chew.transmuteit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.StringUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TransmuteCommand implements CommandExecutor, TabCompleter {

  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      Player player = (Player)sender;
      if(args.length == 0) {
        TransmuteGUI gui = new TransmuteGUI();
        gui.initializeItems(player.getUniqueId(), args, player);
        gui.openInventory(player);
      } else {
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
          int amount = 0;
          try {
            amount = Integer.parseInt(args[2]);
          } catch(NumberFormatException e) {
            sender.sendMessage("Invalid number input! Please enter a number!");
            return true;
          }

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
            sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
            sender.sendMessage(ChatColor.GREEN + "+ " + amount + " " + capitalize(name));
            sender.sendMessage(ChatColor.RED + "- " + NumberFormat.getInstance().format(amount * value) + " EMC [Total: " + NumberFormat.getInstance().format(emc - (value * amount)) + " EMC]");
          } else {
            sender.sendMessage("Uh oh! You don't appear to have discovered " + name + ". Type /getemc to find the exact name.");
          }
          return true;
        } else if(arg0.equals("take")) {
          PlayerInventory inventory = ((Player)sender).getInventory();
          ItemStack item = inventory.getItemInMainHand();
          boolean enchantments = item.getEnchantments().size() > 0;
          boolean confirm = false;
          int amount = item.getAmount();
          int takeAmount = 0;
          if(args.length >= 2) {
            if(args[1].toLowerCase().equals("confirm")) {
              takeAmount = 1;
              confirm = true;
            } else {
              try {
                takeAmount = Integer.parseInt(args[1]);
              } catch(NumberFormatException e) {
                sender.sendMessage("Invalid number input! Please enter a number!");
                return true;
              }
            }

          } else {
            takeAmount = amount;
          }
          if(args.length >= 3) {
            if (args[2].toLowerCase().equals("confirm")) {
              confirm = true;
            }
          }
          if(!confirm && enchantments) {
            sender.sendMessage(ChatColor.YELLOW + "WARNING: " + ChatColor.RED + "This item has enchantments! They will NOT be calculated into the EMC, are you sure you want to transmute this? Add \"confirm\" to the command if so!");
            return true;
          }
          if(takeAmount <= 0) {
            sender.sendMessage("Please select a value greater than 0!");
            return true;
          }
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
              short currentDurability = item.getDurability();
              short maxDurability = type.getMaxDurability();
              if(maxDurability > 0) {
                emc = (int)((double)emc * (((double)maxDurability-(double)currentDurability)/(double)maxDurability));
              }
              item.setAmount(amount - takeAmount);
              UUID uuid = ((Player)sender).getUniqueId();
              int current = new DataManager().getEMC(uuid, player);
              int newEMC = current + (takeAmount * emc);
              bob.writeEMC(uuid, newEMC, player);
              sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
              if(!bob.discovered(uuid, name)) {
                sender.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
                if(bob.discoveries(uuid).size() == 0) {
                  sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.COLOR_CHAR + "7Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                }
                new DataManager().writeDiscovery(uuid, name);
              }
              sender.sendMessage(ChatColor.GREEN + "+ " + NumberFormat.getInstance().format(takeAmount * emc) + " EMC [Total: " + NumberFormat.getInstance().format(newEMC) + " EMC]");
              sender.sendMessage(ChatColor.RED + "- " + takeAmount + " " + capitalize(name));
              return true;
            // If there's no JSON file or it's not IN the JSON file
            } catch(org.json.JSONException e) {
              sender.sendMessage("This item has no set EMC value!");
            }
          }
        } else if(arg0.equals("learn")) {
          PlayerInventory inventory = ((Player)sender).getInventory();
          ItemStack item = inventory.getItemInMainHand();
          Material type = item.getType();
          String name = type.toString();
          // If it's nothing
          if(name.equals("AIR")) {
            sender.sendMessage("Please hold an item to learn it!");
          } else {
            // If it's something
            try {
              TransmuteIt.json.getInt(type.toString());
              DataManager bob = new DataManager();
              UUID uuid = ((Player)sender).getUniqueId();
              sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
              if(!bob.discovered(uuid, name)) {
                sender.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
                if(bob.discoveries(uuid).size() == 0) {
                  sender.sendMessage(ChatColor.COLOR_CHAR + "7" + ChatColor.ITALIC + "Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                }
                new DataManager().writeDiscovery(uuid, name);
              } else {
                sender.sendMessage(ChatColor.COLOR_CHAR + "cYou've already discovered " + name + "!");
              }
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

    if (args.length == 1) {
      commands.add("help");
      commands.add("get");
      commands.add("take");
      commands.add("learn");
      StringUtil.copyPartialMatches(args[0], commands, completions);
    } else if (args.length >= 2 && args[0].equals("help")) {
      StringUtil.copyPartialMatches(args[1], commands, completions);
    } else if (args[0].equals("get")) {
      if(args.length == 2) {
        List<Object> discoveries = new DataManager().discoveries(((Player)sender).getUniqueId());
        for (Object discovery : discoveries) {
          commands.add(discovery.toString());
        }
      }
      StringUtil.copyPartialMatches(args[1], commands, completions);
    }
    Collections.sort(completions);
    return completions;
  }

  public String capitalize(String to) {
    String[] words = to.split("_");
    String newword = "";
    for (String word : words) {
      String rest = word.substring(1).toLowerCase();
      String first = word.substring(0, 1).toUpperCase();
      newword = newword + first + rest + " ";
    }
    return newword;
  }

  public void helpResponse(CommandSender sender) {
    sender.sendMessage("§dWelcome to TransmuteIt!");
    sender.sendMessage("§b/transmute take [amount] §d- Take [amount] of held item and convert to EMC.");
    sender.sendMessage("§b/transmute get [item] [amount] §d- Get amount of item using EMC.");
    sender.sendMessage("§b/transmute help §d- This command.");
    sender.sendMessage("§b/transmute learn §d- Discover the item without transmuting it.");
    sender.sendMessage("§b/getEMC §d- Get the EMC value of held item.");
    sender.sendMessage("§b/emc §d- View your EMC.");
    sender.sendMessage("§b/discoveries [search term] §d- View your Discoveries. Leave blank to see all.");
  }
}
