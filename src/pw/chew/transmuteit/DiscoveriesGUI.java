package pw.chew.transmuteit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONException;

import java.text.NumberFormat;
import java.util.*;

public class DiscoveriesGUI implements InventoryHolder, Listener {
  private final Inventory inv;

  public DiscoveriesGUI() {
    inv = Bukkit.createInventory(this, 54, "Discoveries - Click to Transmute");
  }

  @Override
  public Inventory getInventory() {
    return inv;
  }

  // You can call this whenever you want to put the items in
  public void initializeItems(UUID uuid, String[] args) {
    boolean search = false;
    StringBuilder term = new StringBuilder();
    if(args.length > 0) {
      search = true;
      for (String arg : args) {
        term.append(arg.toUpperCase());
      }
    }
    List<Object> discoveries = new DataManager().discoveries(uuid);
    List<String> strings = new ArrayList<>(discoveries.size());
    for (Object object : discoveries) {
      strings.add(Objects.toString(object, null));
    }
    Collections.sort(strings);
    for (String string : strings) {
      String nameraw = string.toString();
      String nameformatted = nameraw.replace("_", " ");
      try {
        int emc = TransmuteIt.json.getInt(nameraw);
        if (search) {
          if (nameraw.contains(term.toString()) || nameformatted.contains(term.toString())) {
            inv.addItem(createGuiItem(Material.getMaterial(nameraw), nameraw, "Raw Name: " + nameraw, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)));
          }
        } else {
          inv.addItem(createGuiItem(Material.getMaterial(nameraw), nameraw, "Raw Name: " + nameraw, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)));
        }
      } catch (JSONException e) {
        new DataManager().removeDiscovery(uuid, nameraw);
      }

    }
  }

  // Nice little method to create a gui item with a custom name, and description
  private ItemStack createGuiItem(Material material, String name, String...lore) {
    ItemStack item = new ItemStack(material, 1);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(ChatColor.RESET + capitalize(name));

    ArrayList<String> metaLore = new ArrayList<String>(Arrays.asList(lore));

    meta.setLore(metaLore);
    item.setItemMeta(meta);
    return item;
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

  // You can open the inventory with this
  public void openInventory(Player p) {
    p.openInventory(inv);
  }

  // Check for clicks on items
  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    if (e.getInventory().getHolder().getClass() != this.getClass()) {
      return;
    }
    if (e.getClick().equals(ClickType.NUMBER_KEY)){
      e.setCancelled(true);
    }
    e.setCancelled(true);

    Player player = (Player) e.getWhoClicked();
    ItemStack clickedItem = e.getCurrentItem();

    // verify current item is not null
    if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

    if(e.getRawSlot() > 53) {
      return;
    }

    UUID uuid = player.getUniqueId();
    String name = clickedItem.getType().toString();

    if(new DataManager().discovered(uuid, name)) {
      int emc = new DataManager().getEMC(uuid, player);
      int amount = 1;
      if(e.isShiftClick()) {
        amount = 64;
      }
      int value;
      try {
        value = TransmuteIt.json.getInt(name);
      } catch(org.json.JSONException f) {
        player.sendMessage("This item no longer has an EMC value!");
        return;
      }
      if((value * amount) > emc) {
        player.sendMessage("You don't have enough EMC!");
        return;
      }

      PlayerInventory inventory = player.getInventory();
      ItemStack item = new ItemStack(Material.getMaterial(name), amount);
      inventory.addItem(item);
      DataManager bob = new DataManager();
      bob.writeEMC(uuid, emc - (value * amount), player);
      player.sendMessage("Successfully transmuted " + (value * amount) + " EMC into " + amount + " " + name);
    } else {
      try {
        DataManager bob = new DataManager();
        if(!bob.discovered(uuid, name)) {
          player.sendMessage("You've discovered " + name + "!");
          if(bob.discoveries(uuid).size() == 0) {
            player.sendMessage("Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
          }
          new DataManager().writeDiscovery(uuid, name);
        }
      // If there's no JSON file or it's not IN the JSON file
      } catch(org.json.JSONException f) {
        player.sendMessage("This item has no set EMC value!");
      }
    }
  }
}
