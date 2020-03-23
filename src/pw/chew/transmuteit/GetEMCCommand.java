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

public class GetEMCCommand implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      // All this basically is just "Get the held item's name"
      Player player = (Player)sender;
      PlayerInventory inventory = player.getInventory();
      ItemStack item = inventory.getItemInMainHand();
      int amount = item.getAmount();
      Material type = item.getType();
      ItemMeta meta = item.getItemMeta();
      Damageable damage;
      int currentDurability = 0;
      if(meta instanceof Damageable) {
        damage = ((Damageable) meta);
        currentDurability = damage.getDamage();
      }
      short maxDurability = type.getMaxDurability();
      if(currentDurability > maxDurability) {
        currentDurability = maxDurability;
      }
      String name = type.toString();
      // If it's nothing
      if(name.equals("AIR")) {
        sender.sendMessage("Please hold an item to find its EMC value!");
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
          sender.sendMessage(ChatColor.YELLOW + "EMC Value: " + ChatColor.GREEN + emc);
          sender.sendMessage(ChatColor.YELLOW + "Stack EMC Value: " + ChatColor.GREEN + (emc * amount));
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
