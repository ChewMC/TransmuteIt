package pw.chew.transmuteit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TransmuteGUI implements InventoryHolder, Listener {
    private Inventory inv;

    public TransmuteGUI() {
        inv = Bukkit.createInventory(this, 27, "TransmuteIt - Home Page (WIP)");
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    // You can call this whenever you want to put the items in
    public void initializeItems(UUID uuid, String args[], Player player) {
        DataManager bob = new DataManager();
        int emc = bob.getEMC(uuid, player);
        int discoveries = bob.discoveries(uuid).size();
        int totalDiscoveries = TransmuteIt.json.length();
        inv.setItem(10, createGuiItem(Material.getMaterial("PLAYER_HEAD"), "You", ChatColor.YELLOW + "EMC: " + ChatColor.GREEN + NumberFormat.getInstance().format(emc), ChatColor.YELLOW + "Discoveries: " + ChatColor.GREEN + discoveries + " / " + totalDiscoveries));
        inv.setItem(12, createGuiItem(Material.getMaterial("PAPER"), "Help!", "Click to view help!"));
        inv.setItem(14, createGuiItem(Material.getMaterial("ENCHANTING_TABLE"), "Discoveries", "" + "Click to view your discoveries."));
    }

    // Nice little method to create a gui item with a custom name, and description
    private ItemStack createGuiItem(Material material, String name, String...lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + name);

        ArrayList<String> metaLore = new ArrayList<String>(Arrays.asList(lore));

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

        if(e.getRawSlot() > 26) {
            return;
        }

        if(e.getRawSlot() == 12) {
            helpResponse(player);
            List<HumanEntity> viewers = e.getInventory().getViewers();
            for(int i = 0; i < viewers.size(); i++) {
                if(viewers.get(0).getUniqueId() == player.getUniqueId()) {
                    viewers.get(0).closeInventory();
                }
            }
        }

        if(e.getRawSlot() == 14) {
            DiscoveriesGUI gui = new DiscoveriesGUI();
            gui.initializeItems(player.getUniqueId(), new String[0]);
            gui.openInventory(player);
        }


    }

    public void helpResponse(Player player) {
        player.sendMessage("§dWelcome to TransmuteIt!");
        player.sendMessage("§b/transmute take [amount] §d- Take [amount] of held item and convert to EMC.");
        player.sendMessage("§b/transmute get [item] [amount] §d- Get amount of item using EMC.");
        player.sendMessage("§b/transmute help §d- This command.");
        player.sendMessage("§b/transmute learn §d- Discover the item without transmuting it.");
        player.sendMessage("§b/getEMC §d- Get the EMC value of held item.");
        player.sendMessage("§b/emc §d- View your EMC.");
        player.sendMessage("§b/discoveries [search term] §d- View your Discoveries. Leave blank to see all.");
    }
}
