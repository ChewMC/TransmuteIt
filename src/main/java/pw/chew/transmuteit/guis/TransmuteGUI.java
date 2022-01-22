package pw.chew.transmuteit.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import pw.chew.transmuteit.TransmuteIt;
import pw.chew.transmuteit.commands.TransmuteCommand;
import pw.chew.transmuteit.utils.DataManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static pw.chew.transmuteit.utils.GuiHelper.createGuiItem;

public class TransmuteGUI implements InventoryHolder, Listener {
    private final Inventory inv;
    private final TransmuteIt plugin;

    public TransmuteGUI(TransmuteIt plugin) {
        inv = Bukkit.createInventory(this, 27, "TransmuteIt - Home Page");
        this.plugin = plugin;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    // You can call this whenever you want to put the items in
    public void initializeItems(UUID uuid, Player player) {
        long emc = DataManager.getEMC(player);
        int discoveries = DataManager.discoveries(player).size();
        int totalDiscoveries = DataManager.getAmountOfItemsWithEMC();
        inv.setItem(10, createSkullItem(player, ChatColor.YELLOW + "EMC: " + ChatColor.GREEN + NumberFormat.getInstance().format(emc), ChatColor.YELLOW + "Discoveries: " + ChatColor.GREEN + discoveries + " / " + totalDiscoveries));
        inv.setItem(12, createGuiItem(Material.PAPER, "Help!", "Click to view help!"));
        inv.setItem(14, createGuiItem(Material.ENCHANTING_TABLE, "Discoveries", "" + "Click to view your discoveries."));
        inv.setItem(16, createGuiItem(Material.BUCKET, "Transmute Take", "" + "Turn items INTO EMC from your inventory!"));
        inv.setItem(26, createGuiItem(Material.BARRIER, ChatColor.RED + "Close", ChatColor.RED + "Click to close the GUI."));
    }

    private ItemStack createSkullItem(Player player, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setDisplayName(ChatColor.RESET + "You");
        ArrayList<String> metaLore = new ArrayList<>(Arrays.asList(lore));
        skullMeta.setLore(metaLore);
        skullMeta.setOwningPlayer(player);
        head.setItemMeta(skullMeta);

        return head;
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

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        if (e.getRawSlot() > 26) {
            return;
        }

        switch (e.getRawSlot()) {
            case 12 -> {
                TransmuteCommand.helpResponse(player);
                player.closeInventory();
            }
            case 14 -> {
                player.closeInventory();
                player.performCommand("discoveries");
            }
            case 16 -> {
                TransmuteTakeGUI gui = new TransmuteTakeGUI(plugin);
                gui.initializeItems();
                gui.openInventory(player);
            }
            case 26 -> player.closeInventory();
        }
    }
}
