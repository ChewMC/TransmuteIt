package pw.chew.transmuteit;
import java.util.UUID;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.inventory.PlayerInventory;
import java.util.ArrayList;
import java.util.List;

public class DiscoveriesGUI implements InventoryHolder, Listener {
  private Inventory inv;

  public DiscoveriesGUI() {
    inv = Bukkit.createInventory(this, 54, "Discoveries - Click to Transmute");
  }

  @Override
  public Inventory getInventory() {
    return inv;
  }

  // You can call this whenever you want to put the items in
  public void initializeItems(UUID uuid) {
    List<Object> discoveries = new DataManager().discoveries(uuid);
    for(int i = 0; i < discoveries.size() && i < 54; i++) {
      inv.addItem(createGuiItem(Material.getMaterial(discoveries.get(i).toString()), discoveries.get(i).toString(), "EMC Value: Not implemented"));
    }
  }

  // Nice little method to create a gui item with a custom name, and description
  private ItemStack createGuiItem(Material material, String name, String...lore) {
    ItemStack item = new ItemStack(material, 1);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(ChatColor.RESET + name);
    ArrayList<String> metaLore = new ArrayList<String>();

    for(String loreComments : lore) {
      metaLore.add(loreComments);
    }

    meta.setLore(metaLore);
    item.setItemMeta(meta);
    return item;
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

    UUID uuid = player.getUniqueId();
    String name = clickedItem.getType().toString();
    int amount = 1;

    int emc = new DataManager().getEMC(uuid, player);
    int value;
    try {
      value = TransmuteIt.json.getInt(name);
    } catch(org.json.JSONException f) {
      player.sendMessage("This item no longer has an EMC value!");
      return;
    }
    if((value * amount) > emc) {
      player.sendMessage("You don't have enough EMC!");
    }

    PlayerInventory inventory = player.getInventory();
    ItemStack item = new ItemStack(Material.getMaterial(name), amount);
    inventory.addItem(item);
    DataManager bob = new DataManager();
    bob.writeEMC(uuid, emc - (value * amount), player);
    player.sendMessage("Successfully transmuted " + (value * amount) + " EMC into " + amount + " " + name);
  }
}