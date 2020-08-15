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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.UUID;

import static pw.chew.transmuteit.utils.GuiHelper.createGuiItem;
import static pw.chew.transmuteit.utils.StringFormattingHelper.capitalize;

public class TransmuteTakeGUI implements InventoryHolder, Listener {
    private final Inventory inv;

    public TransmuteTakeGUI() {
        inv = Bukkit.createInventory(this, 9, "Click Items to Transmute");
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        for(int i=0; i < 8; i++) {
            inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ""));
        }
        // Setting return button
        inv.setItem(8, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Return", ChatColor.RED + "Click to return to the main menu."));
    }

    // You can open the inventory with this
    public void openInventory(Player p) {
        p.openInventory(inv);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() == null || e.getInventory().getHolder().getClass() != this.getClass()) {
            return;
        }
        if (e.getClick().equals(ClickType.NUMBER_KEY)){
            e.setCancelled(true);
        }
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        PlayerInventory inventory = player.getInventory();
        ItemStack returnButton = createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Return", ChatColor.RED + "Click to return to the main menu.");

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Verify if clicked item is the return button
        if (clickedItem.equals(returnButton)) {
            player.closeInventory();
            TransmuteGUI mainMenu = new TransmuteGUI();
            mainMenu.initializeItems(player.getUniqueId(), player);
            mainMenu.openInventory(player);
            return;
        }

        int selectedItem = e.getRawSlot();
        if(selectedItem > 8) {
            if(selectedItem >= 36) {
                selectedItem -= 36;
            }
            ItemStack item = inventory.getItem(selectedItem);
            int amount = item.getAmount();

            Material type = item.getType();
            String name = type.toString();

            boolean loreAllowed = TransmuteIt.config.getBoolean("lore");
            // If it's nothing
                // If it's something
            try {
                DataManager bob = new DataManager();
                int emc = TransmuteIt.json.getInt(type.toString());
                if(!loreAllowed && item.getItemMeta().hasLore()) {
                    player.sendMessage(ChatColor.RED + "This item has a custom lore set, and items with lore can't be transmuted as per the config.");
                    return;
                }
                int currentDurability = 0;
                ItemMeta meta = item.getItemMeta();
                Damageable damage;
                if(meta instanceof Damageable) {
                    damage = ((Damageable) meta);
                    currentDurability = damage.getDamage();
                }
                short maxDurability = type.getMaxDurability();
                if(currentDurability > maxDurability) {
                    currentDurability = maxDurability;
                }
                if(maxDurability > 0) {
                    emc = (int)((double)emc * (((double)maxDurability-(double)currentDurability)/(double)maxDurability));
                }
                item.setAmount(0);
                UUID uuid = player.getUniqueId();
                int current = new DataManager().getEMC(uuid, player);
                int newEMC = current + (amount * emc);
                bob.writeEMC(uuid, newEMC, player);
                player.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
                if(!bob.discovered(uuid, name)) {
                    player.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
                    if(bob.discoveries(uuid).size() == 0) {
                        player.sendMessage(ChatColor.ITALIC + "" + ChatColor.COLOR_CHAR + "7Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                    }
                    new DataManager().writeDiscovery(uuid, name);
                }
                player.sendMessage(ChatColor.GREEN + "+ " + NumberFormat.getInstance().format(amount * emc) + " EMC [Total: " + NumberFormat.getInstance().format(newEMC) + " EMC]");
                player.sendMessage(ChatColor.RED + "- " + amount + " " + capitalize(name));
                // If there's no JSON file or it's not IN the JSON file
            } catch(org.json.JSONException f) {
                player.sendMessage("This item has no set EMC value!");
            }
        }

    }
}
