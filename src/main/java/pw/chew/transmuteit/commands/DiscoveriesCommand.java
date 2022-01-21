package pw.chew.transmuteit.commands;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pw.chew.transmuteit.objects.TransmutableItem;
import pw.chew.transmuteit.utils.ChatHelper;
import pw.chew.transmuteit.utils.DataManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static pw.chew.transmuteit.utils.StringFormattingHelper.capitalize;

public class DiscoveriesCommand implements CommandExecutor, Listener {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            List<String> discoveries = new ArrayList<>(DataManager.discoveries(player));

            if (discoveries.isEmpty()) {
                return ChatHelper.sendError(sender, "You haven't discovered anything yet! Hold an item and type \"/tm learn\" or \"/tm take\" to discover an item!");
            }

            // Initialize GUI and background
            ChestGui gui = new ChestGui(6, "Your Discoveries");
            OutlinePane background = new OutlinePane(0, 0, 9, 6);
            for (int i = 0; i < 9 * 6; i++) {
                background.addItem(createGuiItem(Material.BLUE_STAINED_GLASS_PANE, ""));
            }
            background.setOnClick(event -> event.setCancelled(true));
            gui.addPane(background);

            // Initialize items to show
            boolean search = false;
            StringBuilder term = new StringBuilder();
            if(args.length > 0) {
                search = true;
                for (String arg : args) {
                    term.append(arg.toUpperCase());
                }
            }
            Collections.sort(discoveries);
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 5);
            int panes = (int) Math.ceil((float)discoveries.size() / 28);
            int discovery = 0;
            for(int i = 0; i < panes; i++) {
                StaticPane pagePane = new StaticPane(1, 1, 7, 4);
                for (int j = 0; j < 28; j++) {
                    if(discovery < discoveries.size()) {
                        int[] coord = coords(j);
                        String string = discoveries.get(discovery);
                        discovery++;
                        String nameformatted = string.replace("_", " ");
                        int emc = DataManager.getItemEMC(string);
                        if (emc > 0) {
                            if (search) {
                                if (string.contains(term.toString()) || nameformatted.contains(term.toString())) {
                                    pagePane.addItem(createGuiItem(Material.getMaterial(string), string, "Raw Name: " + string, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)), coord[0], coord[1]);
                                } else {
                                    j--;
                                }
                            } else {
                                pagePane.addItem(createGuiItem(Material.getMaterial(string), string, "Raw Name: " + string, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)), coord[0], coord[1]);
                            }
                        }
                    }
                }
                pane.addPane(i, pagePane);
            }
            pane.setOnClick(this::onItemClick);
            gui.addPane(pane);

            //page selection (only if more than 1 pane)
            StaticPane back = new StaticPane(2, 5, 1, 1);
            StaticPane forward = new StaticPane(6, 5, 1, 1);

            ItemStack backArrow = createItemStack(Material.ARROW, "Back");
            ItemStack nextArrow = createItemStack(Material.ARROW, "Next");

            back.addItem(new GuiItem(backArrow, event -> {
                pane.setPage(pane.getPage() - 1);

                if (pane.getPage() == 0) {
                    back.setVisible(false);
                }

                forward.setVisible(true);
                gui.update();
            }), 0, 0);

            back.setVisible(false);

            forward.addItem(new GuiItem(nextArrow, event -> {
                pane.setPage(pane.getPage() + 1);

                if (pane.getPage() == pane.getPages() - 1) {
                    forward.setVisible(false);
                }

                back.setVisible(true);
                gui.update();
            }), 0, 0);

            if(panes != 1) {
                gui.addPane(back);
                gui.addPane(forward);
            }

            gui.show(player);
            // gui.initializeItems(player.getUniqueId(), args);
            // gui.openInventory(player);
        } else {
            sender.sendMessage("[TransmuteIt] Only players may run this command.");
        }

        // If the player (or console) uses our command correct, we can return true
        return true;
    }

    private int[] coords(int loc) {
        int x;
        int y = 0;
        if (loc >= 7) {
            while (loc >= 7) {
                loc -= 7;
                y++;
            }
        }
        x = loc;
        return new int[]{x, y};
    }

    // Nice little method to create a gui item with a custom name, and description
    private GuiItem createGuiItem(Material material, String name, String...lore) {
        return new GuiItem(createItemStack(material, name, lore));
    }

    private ItemStack createItemStack(Material material, String name, String...lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RESET + capitalize(name));

        if(lore.length > 0) {
            ArrayList<String> metaLore = new ArrayList<>(Arrays.asList(lore));
            meta.setLore(metaLore);
        }

        item.setItemMeta(meta);

        return item;
    }

    public void onItemClick(InventoryClickEvent e) {
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if(!clickedItem.getItemMeta().hasLore()) {
            return;
        }

        // Ensure item has EMC and stuff
        TransmutableItem transmutableItem = new TransmutableItem(clickedItem);
        if (!transmutableItem.hasEMC()) {
            ChatHelper.sendError(player, "This item no longer has an EMC value!");
            return;
        }

        UUID uuid = player.getUniqueId();
        String name = clickedItem.getType().toString();

        if (DataManager.hasDiscovered(player, name)) {
            long emc = DataManager.getEMC(player);
            int amount = e.isShiftClick() ? 64 : 1;
            int value = transmutableItem.getItemEMC();
            long change = (long) value * amount;
            if (change > emc) {
                ChatHelper.sendError(player, "You don't have enough EMC! You still need %s EMC.", (change - emc));
                return;
            }

            PlayerInventory inventory = player.getInventory();
            ItemStack item = new ItemStack(Material.getMaterial(name), amount);
            DataManager.writeEMC(player, emc - change);
            inventory.addItem(item);
            player.sendMessage(ChatColor.GREEN + "Successfully transmuted " + change + " EMC into " + amount + " " + name);
        } else {
            player.sendMessage(ChatColor.GREEN + "You've discovered " + name + "!");
            if (DataManager.hasNoDiscoveries(player)) {
                player.sendMessage(ChatColor.GRAY + "Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
            }
            DataManager.writeDiscovery(uuid, name);
        }
    }
}
