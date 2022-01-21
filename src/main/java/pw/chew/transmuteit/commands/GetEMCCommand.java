package pw.chew.transmuteit.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pw.chew.transmuteit.objects.TransmutableItem;
import pw.chew.transmuteit.utils.ChatHelper;
import pw.chew.transmuteit.utils.DataManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pw.chew.transmuteit.utils.StringFormattingHelper.capitalize;

public class GetEMCCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Get JSON values
        JSONObject json = DataManager.getEMCValues();

        if (sender instanceof Player player) {
            TransmutableItem item;
            if (args.length == 0) {
                if (player.getInventory().getItemInMainHand().getType().isAir()) {
                    return ChatHelper.sendError(sender, "Please hold an item to find its EMC value!");
                }

                item = new TransmutableItem(player.getInventory().getItemInMainHand());
            } else {
                String friendlyName = args[0].toUpperCase();
                Material material = Material.matchMaterial(friendlyName);
                if (!json.has(friendlyName) || material == null) {
                    sender.sendMessage(ChatColor.YELLOW + "EMC Value: " + ChatColor.GREEN + "None!");
                    return true;
                }

                item = new TransmutableItem(new ItemStack(material));
            }

            // Type and name storage
            Material type = item.getType();
            String name = type.toString();

            // Storage values
            int itemEMC = item.getItemEMC();
            long handEMC = item.getEMC();
            int inventoryAmount = 0;
            int inventoryEMC = 0;

            // Get EMC from inventory
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null || stack.getType().isAir()) continue;
                if (type != stack.getType()) continue;

                inventoryEMC += new TransmutableItem(stack).getEMC();
                inventoryAmount += stack.getAmount();
            }

            boolean discovered = DataManager.hasDiscovered(player, name);

            // Return responses
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "--------[ " + ChatColor.AQUA + "Item Information" + ChatColor.LIGHT_PURPLE + " ]--------");
            sender.sendMessage(ChatColor.YELLOW + "Friendly Name: " + ChatColor.GREEN + capitalize(name));
            sender.sendMessage(ChatColor.YELLOW + "Raw Name: " + ChatColor.GREEN + name);
            sender.sendMessage(ChatColor.YELLOW + "Discovered? " + (discovered ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            sender.sendMessage(ChatColor.YELLOW + "Item EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(itemEMC));
            if (args.length == 0) {
                sender.sendMessage(ChatColor.YELLOW + "Hand EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(handEMC) + " (for " + item.getAmount() + " items)");
            }
            if (inventoryAmount > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Inventory EMC Value: " + ChatColor.GREEN + NumberFormat.getInstance().format(inventoryEMC) + " (for " + inventoryAmount + " items)");
            }
        } else {
            // If the sender isn't a player, return basic information

            // An option is required to get the EMC value
            if (args.length == 0) {
                return ChatHelper.sendError(sender, "Please specify an item!");
            }

            // Get the item and return the EMC value
            String friendlyName = args[0].toUpperCase();
            int emc = json.optInt(friendlyName, -1);
            if (emc > 0) {
                sender.sendMessage("EMC Value for " + friendlyName + ": " + NumberFormat.getInstance().format(emc));
            } else {
                sender.sendMessage("EMC Value for " + friendlyName + ": " + "None!");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            String[] items = DataManager.getEMCValues().keySet().toArray(new String[0]);
            Collections.addAll(commands, items);
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        Collections.sort(completions);
        return completions;
    }
}
