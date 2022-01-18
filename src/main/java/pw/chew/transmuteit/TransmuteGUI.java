package pw.chew.transmuteit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.JSONObject;
import pw.chew.transmuteit.commands.TransmuteCommand;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static pw.chew.transmuteit.utils.GuiHelper.createGuiItem;

public class TransmuteGUI implements InventoryHolder, Listener {
    private final Inventory inv;
    private static JSONObject json;
    private static DataManager dataManager;
    private static FileConfiguration config;

    public TransmuteGUI(JSONObject jsonData, DataManager data, FileConfiguration configFile) {
        inv = Bukkit.createInventory(this, 27, "TransmuteIt - Home Page");
        json = jsonData;
        dataManager = data;
        config = configFile;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    // You can call this whenever you want to put the items in
    public void initializeItems(UUID uuid, Player player) {
        int emc = dataManager.getEMC(player);
        int discoveries = dataManager.discoveries(uuid).size();
        int totalDiscoveries = json.length();
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
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if(e.getRawSlot() > 26) {
            return;
        }

        if(e.getRawSlot() == 12) {
            TransmuteCommand.helpResponse(player);
            player.closeInventory();
        }

        if(e.getRawSlot() == 14) {
            player.closeInventory();
            player.performCommand("discoveries");
        }

        if(e.getRawSlot() == 16) {
            TransmuteTakeGUI gui = new TransmuteTakeGUI(json, dataManager, config);
            gui.initializeItems();
            gui.openInventory(player);
        }

        if(e.getRawSlot() == 26) {
            player.closeInventory();
        }


    }
}
