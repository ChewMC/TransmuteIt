package pw.chew.transmuteit.commands;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONException;
import pw.chew.transmuteit.DataManager;
import pw.chew.transmuteit.TransmuteIt;

import java.text.NumberFormat;
import java.util.*;

import static pw.chew.transmuteit.utils.StringFormattingHelper.capitalize;

public class DiscoveriesCommand implements CommandExecutor, Listener {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            UUID uuid = ((Player) sender).getUniqueId();
            List<Object> discoveries = new DataManager().discoveries(uuid);
            List<String> strings = new ArrayList<>(discoveries.size());

            if(discoveries.size() == 0) {
                sender.sendMessage(ChatColor.RED + "You haven't discovered anything yet! Hold an item and type \"/tm learn\" or \"/tm take\" to discover an item!");
                return true;
            }

            // Initialize GUI and background
            Gui gui = new Gui(Bukkit.getPluginManager().getPlugin("TransmuteIt"), 6, "Your Discoveries");
            OutlinePane background = new OutlinePane(0, 0, 9, 6);
            background.addItem(createGuiItem(Material.BLUE_STAINED_GLASS_PANE, ""));
            background.setRepeat(true);
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
            for (Object object : discoveries) {
                strings.add(Objects.toString(object, null));
            }
            Collections.sort(strings);
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 5);
            int panes = (int) Math.ceil((float)discoveries.size() / 28);
            int discovery = 0;
            for(int i = 0; i < panes; i++) {
                StaticPane pagePane = new StaticPane(1, 1, 7, 4);
                for (int j = 0; j < 28; j++) {
                    if(discovery < discoveries.size()) {
                        int[] coord = coords(j);
                        String string = strings.get(discovery);
                        discovery++;
                        String nameformatted = string.replace("_", " ");
                        try {
                            int emc = TransmuteIt.json.getInt(string);
                            if (search) {
                                if (string.contains(term.toString()) || nameformatted.contains(term.toString())) {
                                    pagePane.addItem(createGuiItem(Material.getMaterial(string), string, "Raw Name: " + string, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)), coord[0], coord[1]);
                                } else {
                                    j--;
                                }
                            } else {
                                pagePane.addItem(createGuiItem(Material.getMaterial(string), string, "Raw Name: " + string, "§r§eEMC: §f" + NumberFormat.getInstance().format(emc)), coord[0], coord[1]);
                            }
                        } catch (JSONException e) {
                            new DataManager().removeDiscovery(uuid, string);
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

            ItemStack backArrow = createItemStack(Material.getMaterial("ARROW"), "Back");
            ItemStack nextArrow = createItemStack(Material.getMaterial("ARROW"), "Next");

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

            gui.show((HumanEntity) sender);
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
        if (e.getClick().equals(ClickType.NUMBER_KEY)){
            e.setCancelled(true);
        }
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if(!clickedItem.getItemMeta().hasLore()) {
            e.setCancelled(true);
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
