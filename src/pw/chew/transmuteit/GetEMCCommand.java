package pw.chew.transmuteit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.HashMap;

public class GetEMCCommand implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      // All this basically is just "Get the held item's name"
      Player player = (Player)sender;
      int maxDurability = 0;
      int currentDurability = 0;
      int amount = 0;
      int inventoryAmount = 0;
      boolean arg = false;
      Material type;
      PlayerInventory inventory = player.getInventory();
      ItemStack item = inventory.getItemInMainHand();
      if(args.length == 0 || (item.getType().toString().equals(args[0].toUpperCase()))) {
        amount = item.getAmount();
        inventoryAmount = 0;
        type = item.getType();
        ItemMeta meta = item.getItemMeta();
        Damageable damage;
        currentDurability = 0;
        if(meta instanceof Damageable) {
          damage = ((Damageable) meta);
          currentDurability = damage.getDamage();
        }
      } else {
        arg = true;
        try {
          type = Material.getMaterial(args[0].toUpperCase());
        } catch(IllegalArgumentException e) {
          sender.sendMessage(ChatColor.RED + "An item with the name \"" + args[0].toUpperCase() + "\" does not exist!");
          return true;
        }
      }

      HashMap<Integer, ? extends ItemStack> inventoryItems;
      try {
        inventoryItems = inventory.all(type);
      } catch(IllegalArgumentException e) {
        sender.sendMessage(ChatColor.RED + "An item with the name \"" + args[0].toUpperCase() + "\" does not exist!");
        return true;
      }
      ItemStack[] inventoryItemsThanks = inventoryItems.values().toArray(new ItemStack[0]);
      for(int i = 0; i < inventoryItems.size(); i++) {
        inventoryAmount += inventoryItemsThanks[i].getAmount();
      }
      maxDurability = type.getMaxDurability();
      if(currentDurability > maxDurability) {
        currentDurability = maxDurability;
      }
      if(arg) {
        currentDurability = maxDurability;
      }
      String name = type.toString();
      // If it's nothing
      if(name.equals("AIR")) {
        sender.sendMessage(ChatColor.RED + "Please hold an item to find its EMC value!");
      } else {
        // If it's something
        sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bItem Information" + ChatColor.COLOR_CHAR + "d ]--------");
        sender.sendMessage(ChatColor.YELLOW + "Friendly Name: " + ChatColor.GREEN + new TransmuteCommand().capitalize(name));
        sender.sendMessage(ChatColor.YELLOW + "Raw Name: " + ChatColor.GREEN + name);
        try {
          int emc = TransmuteIt.json.getInt(type.toString());
          int normal_emc = emc;
          if(maxDurability > 0) {
            emc = (int)((double)emc * (((double)maxDurability-(double)currentDurability)/(double)maxDurability));
          }
          sender.sendMessage(ChatColor.YELLOW + "Single EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(emc));
          if(!arg) {
            sender.sendMessage(ChatColor.YELLOW + "Hand EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(emc * amount) + " (for " + amount + " items)");
          }
          if(inventoryAmount > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Inventory EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(emc * inventoryAmount) + " (for " + inventoryAmount + " items)");
          }
          // If there's no JSON file or it's not IN the JSON file
        } catch(org.json.JSONException e) {
          sender.sendMessage(ChatColor.YELLOW + "EMC Value: " + ChatColor.GREEN + "None!");
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
