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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.StringUtil;

import java.text.NumberFormat;
import java.util.*;

public class TransmuteCommand implements CommandExecutor, TabCompleter {

  // This method is called, when somebody uses our command
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    // If is not a player
    if (!(sender instanceof Player)) {
      sender.sendMessage("[TransmuteIt] Only players may run this command.");
      return true;
    }

    Player player = (Player)sender;
    // Show GUI
    if(args.length == 0) {
      TransmuteGUI gui = new TransmuteGUI();
      gui.initializeItems(player.getUniqueId(), args, player);
      gui.openInventory(player);
      return true;
    }

    String arg0 = args[0].toLowerCase();
    switch (arg0) {
      case "help":
        return helpResponse(sender);
      case "get":
        return this.handleGet(sender, player, args);
      case "take":
        return this.handleTake(sender, player, args);
      case "learn":
        return this.handleLearn(sender);
      case "analyze":
        return this.handleAnalyze(sender);
      default:
        sender.sendMessage("Invalid subcommand! Need help? Try \"/transmute help\"");
        return true;
    }
  }


  private boolean handleGet(CommandSender sender, Player player, String[] args) {
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
      sender.sendMessage(ChatColor.RED + "Uh oh! You don't appear to have discovered " + name + ". Type \"/discoveries\" to view your discoveries.");
    }
    return true;
  }

  private boolean handleTake(CommandSender sender, Player player, String[] args) {
    PlayerInventory inventory = ((Player)sender).getInventory();
    ItemStack item = inventory.getItemInMainHand();
    boolean enchantments = item.getEnchantments().size() > 0;
    boolean confirm = false;
    ItemStack[] items = inventory.all(item.getType()).values().toArray(new ItemStack[0]);
    int amount = 0;
    for (ItemStack itemStack : items) {
      amount += itemStack.getAmount();
    }
    int takeAmount;
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
      sender.sendMessage("You don't have enough of this item! (You only have " + amount + ")");
      return true;
    }
    Material type = item.getType();
    String name = type.toString();
    // If it's nothing
    if(name.equals("AIR")) {
      sender.sendMessage("Please hold an item to transmute it!");
      return true;
    }

    // If it's something
    try {
      DataManager bob = new DataManager();
      int emc = TransmuteIt.json.getInt(type.toString());
      short currentDurability = item.getDurability();
      short maxDurability = type.getMaxDurability();
      if(maxDurability > 0) {
        emc = (int)((double)emc * (((double)maxDurability-(double)currentDurability)/(double)maxDurability));
      }
      int taken = 0;
      for (ItemStack itemStack : items) {
        if (taken != takeAmount) {
          int inStack = itemStack.getAmount();
          if (inStack + taken <= takeAmount) {
            itemStack.setAmount(0);
            taken += inStack;
          } else {
            itemStack.setAmount(Math.abs(takeAmount - taken - inStack));
            taken = takeAmount;
          }
        }
      }
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
      return true;
    }
  }

  private boolean handleLearn(CommandSender sender) {
    PlayerInventory inventory = ((Player) sender).getInventory();
    ItemStack item = inventory.getItemInMainHand();
    Material type = item.getType();
    String name = type.toString();
    // If it's nothing
    if (name.equals("AIR")) {
      sender.sendMessage("Please hold an item to learn it!");
      return true;
    }
    // If it's something
    try {
      TransmuteIt.json.getInt(type.toString());
      DataManager bob = new DataManager();
      UUID uuid = ((Player) sender).getUniqueId();
      sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
      if (!bob.discovered(uuid, name)) {
        sender.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
        if (bob.discoveries(uuid).size() == 0) {
          sender.sendMessage(ChatColor.COLOR_CHAR + "7" + ChatColor.ITALIC + "Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
        }
        new DataManager().writeDiscovery(uuid, name);
      } else {
        sender.sendMessage(ChatColor.COLOR_CHAR + "cYou've already discovered " + name + "!");
      }
      return true;
      // If there's no JSON file or it's not IN the JSON file
    } catch (org.json.JSONException e) {
      sender.sendMessage("This item has no set EMC value!");
      return true;
    }
  }

  private boolean handleAnalyze(CommandSender sender) {
    PlayerInventory inventory = ((Player) sender).getInventory();
    HashMap<String, Integer> amountMap = new HashMap<>();
    HashMap<String, Integer> emcValueMap = new HashMap<>();
    for (int i = 0; i < inventory.getSize(); i++) {
      try {
        ItemStack item = inventory.getItem(i);
        String name = item.getType().toString();
        if (amountMap.containsKey(name)) {
          int current = amountMap.get(name);
          amountMap.replace(name, current + item.getAmount());
        } else {
          amountMap.put(name, item.getAmount());
        }
        int emc = -1;
        try {
          emc = TransmuteIt.json.getInt(name);
        } catch(org.json.JSONException ignored) {

        }
        if(item.getItemMeta() instanceof Damageable) {
          Damageable damage = ((Damageable) item.getItemMeta());
          emcValueMap.put(name, damage.getDamage() * emc);
        } else {
          emcValueMap.put(name, emc);
        }
      } catch(NullPointerException ignored) {

      }
    }
    sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bInventory Analysis" + ChatColor.COLOR_CHAR + "d ]--------");
    Object[] keys = amountMap.keySet().toArray();
    Arrays.sort(keys);
    int total = 0;
    for(int i = 0; i < keys.length; i++) {
      String name = (String) keys[i];
      int amount = amountMap.get(name);
      try {
        int emc = TransmuteIt.json.getInt(name);
        sender.sendMessage(ChatColor.YELLOW + capitalize(name) + ": " + ChatColor.GREEN + NumberFormat.getInstance().format(emc * amount) + " EMC (" + NumberFormat.getInstance().format(emc) + " EMC each for " + amount + " items)");
        total += emc * amount;
      } catch(org.json.JSONException e) {
        sender.sendMessage(ChatColor.YELLOW + capitalize(name) + ": " + ChatColor.GREEN + "No EMC Value!");
      }
    }
    sender.sendMessage(ChatColor.YELLOW + "TOTAL" + ": " + ChatColor.GREEN + NumberFormat.getInstance().format(total) + " EMC");
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
      commands.add("analyze");
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
    return newword.substring(0, newword.length()-1);
  }

  public boolean helpResponse(CommandSender sender) {
    sender.sendMessage("§dWelcome to TransmuteIt!");
    sender.sendMessage("§b/transmute take [amount] §d- Take [amount] of held item and convert to EMC.");
    sender.sendMessage("§b/transmute get [item] [amount] §d- Get amount of item using EMC.");
    sender.sendMessage("§b/transmute help §d- This command.");
    sender.sendMessage("§b/transmute learn §d- Discover the item without transmuting it.");
    sender.sendMessage("§b/getEMC §d- Get the EMC value of held item.");
    sender.sendMessage("§b/emc §d- View your EMC.");
    sender.sendMessage("§b/discoveries [search term] §d- View your Discoveries. Leave blank to see all.");
    return true;
  }
}
