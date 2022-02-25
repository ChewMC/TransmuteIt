package pw.chew.transmuteit.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pw.chew.transmuteit.TransmuteIt;
import pw.chew.transmuteit.objects.TransmutableItem;
import pw.chew.transmuteit.utils.ChatHelper;
import pw.chew.transmuteit.utils.DataManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pw.chew.transmuteit.utils.GuiHelper.createGuiItem;

public class TransmuteTakeGUI implements InventoryHolder, Listener {
    private final Inventory inv;
    private final TransmuteIt plugin;

    public TransmuteTakeGUI(TransmuteIt plugin) {
        inv = Bukkit.createInventory(this, 9 * 5, "Click Items to Transmute");
        this.plugin = plugin;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        // Set the bottom row
        for(int i=9*4; i < 9*5; i++) {
            inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ""));
        }
        // Setting "total emc" item
        inv.setItem(9*4, createTotalEMCItem(0));
        // Set Paper with lore
        inv.setItem(9*4+4, createGuiItem(Material.PAPER, "Help!", "Put items in here to transmute them!"));
        // Setting return button
        inv.setItem(9*5-1, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Return", ChatColor.RED + "Click to return to the main menu."));
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

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Verify if clicked item is the return button
        if (e.getRawSlot() == 9*5-1) {
            player.closeInventory();
            TransmuteGUI mainMenu = new TransmuteGUI(plugin);
            mainMenu.initializeItems(player.getUniqueId(), player);
            mainMenu.openInventory(player);
            return;
        }

        if (e.getRawSlot() == 9*4) {
            // Set the total emc item
            e.getInventory().setItem(9*4, createTotalEMCItem(getEMC(e.getInventory())));
        }

        // Ignore the action row
        if (e.getRawSlot() >= 9*4 && e.getRawSlot() < 9*5) {
            e.setCancelled(true);
            return;
        }

        TransmutableItem item = new TransmutableItem(clickedItem);

        // Don't let it work if there's no EMC
        if (!item.hasEMC()) {
            e.setCancelled(true);
            ChatHelper.sendError(player, "This item cannot be transmuted!");
            return;
        }

        // Ensure items with lore are not transmuted
        if (item.hasLore() && plugin.getConfig().getBoolean("lore", true)) {
            e.setCancelled(true);
            ChatHelper.sendError(player, "This item has a custom lore set, and items with lore can't be transmuted as per the config.");
            return;
        }

        // Update the total EMC
        e.getInventory().setItem(9*4, createTotalEMCItem(getEMC(e.getInventory())));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (inventory.getHolder() == null || inventory.getHolder().getClass() != this.getClass()) {
            return;
        }

        Player player = (Player) e.getPlayer();

        long emc = DataManager.getEMC(player);
        long toAdd = 0;
        List<String> discoveries = new ArrayList<>();

        for (int i = 0; i < 9*4; i++) {ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            TransmutableItem item = new TransmutableItem(stack);

            if (item.hasEMC()) {
                toAdd += item.getEMC();
                discoveries.add(item.getName());
            }
        }

        // Make sure discoveries is unique
        discoveries = discoveries.stream().distinct().collect(Collectors.toList());

        // Write the data
        DataManager.writeEMC(player, emc + toAdd);
        DataManager.writeDiscoveries(player.getUniqueId(), discoveries);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "--------[ " + ChatColor.AQUA + "Transmuting Stats" + ChatColor.LIGHT_PURPLE + " ]--------");
        player.sendMessage(ChatColor.GREEN + "+ " + NumberFormat.getInstance().format(toAdd) + " EMC [Total: " + NumberFormat.getInstance().format(emc + toAdd) + " EMC]");
    }

    /**
     * Gets the total EMC value of the items in the inventory
     *
     * @param inventory The inventory to check
     * @return The total EMC value of the items in the inventory
     */
    private long getEMC(Inventory inventory) {
        long emc = 0;
        for (int i = 0; i < 9*4; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            TransmutableItem item = new TransmutableItem(stack);

            if (item.hasEMC()) {
                emc += item.getEMC();
            }
        }
        return emc;
    }

    private ItemStack createTotalEMCItem(long emc) {
        String formatted = NumberFormat.getInstance().format(emc);
        return createGuiItem(Material.GOLD_INGOT, ChatColor.GREEN + "Total EMC: " + formatted + " EMC", ChatColor.GRAY + "Click to refresh!");
    }
}
