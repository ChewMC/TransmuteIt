package pw.chew.transmuteit.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.json.JSONObject;
import pw.chew.transmuteit.DataManager;
import pw.chew.transmuteit.TransmuteGUI;
import pw.chew.transmuteit.TransmuteIt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.*;

import static pw.chew.transmuteit.utils.StringFormattingHelper.capitalize;

public class TransmuteCommand implements CommandExecutor, TabCompleter {

    // /transmute command handler.
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // If sender is not a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("[TransmuteIt] Only players may run this command.");
            return true;
        }

        Player player = (Player)sender;
        // Show GUI or /tm help, permission depending, if no ARGs are specified
        if(args.length == 0) {
            if(sender.hasPermission("transmute.gui")) {
                TransmuteGUI gui = new TransmuteGUI();
                gui.initializeItems(player.getUniqueId(), player);
                gui.openInventory(player);
                return true;
            } else {
                return helpResponse(sender);
            }
        }

        // Main sub-command handler. If no perm, tell them.
        String arg0 = args[0].toLowerCase();
        switch (arg0) {
            case "help":
                return helpResponse(sender);
            case "get":
                if(sender.hasPermission("transmute.command.get")) {
                    return this.handleGet(sender, player, args);
                } else {
                    return missingPermissionResponse(sender, "transmute.command.get");
                }
            case "take":
                if(sender.hasPermission("transmute.command.take")) {
                    return this.handleTake(sender, player, args);
                } else {
                    return missingPermissionResponse(sender, "transmute.command.take");
                }
            case "learn":
                if(sender.hasPermission("transmute.command.learn")) {
                    return this.handleLearn(sender);
                } else {
                    return missingPermissionResponse(sender, "transmute.command.learn");
                }
            case "analyze":
                if(sender.hasPermission("transmute.command.analyze")) {
                    return this.handleAnalyze(sender);
                } else {
                    return missingPermissionResponse(sender, "transmute.command.analyze");
                }
            case "version":
                if(sender.hasPermission("transmute.command.version")) {
                    return this.handleVersion(sender);
                } else {
                    return missingPermissionResponse(sender, "transmute.command.version");
                }
            default:
                sender.sendMessage("Invalid sub-command! Need help? Try \"/transmute help\"");
                return true;
        }
    }

    // Handle /tm get and its args.
    private boolean handleGet(CommandSender sender, Player player, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("This sub-command requires more arguments! Check \"/transmute help\" for more info.");
            return true;
        }
        UUID uuid = player.getUniqueId();
        String name = args[1].toUpperCase();
        int amount = 0;
        try {
            amount = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            sender.sendMessage("Invalid number input! Please enter a number!");
            return true;
        }

        if(new DataManager().discovered(uuid, name)) {
            int emc = new DataManager().getEMC(uuid, player);
            int value;
            try {
                value = TransmuteIt.json.getInt(name);
            } catch(org.json.JSONException e) {
                sender.sendMessage("This item no longer has an EMC value!");
                return true;
            }
            if((value * amount) > emc) {
                sender.sendMessage("You don't have enough EMC!");
                return true;
            }

            PlayerInventory inventory = player.getInventory();
            ItemStack item = new ItemStack(Material.getMaterial(name), amount);
            inventory.addItem(item);
            DataManager bob = new DataManager();
            bob.writeEMC(uuid, emc - (value * amount), player);
            sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
            sender.sendMessage(ChatColor.GREEN + "+ " + amount + " " + capitalize(name));
            sender.sendMessage(ChatColor.RED + "- " + NumberFormat.getInstance().format(amount * value) + " EMC [Total: " + NumberFormat.getInstance().format(emc - (value * amount)) + " EMC]");
        } else {
            sender.sendMessage(ChatColor.RED + "Uh oh! You don't appear to have discovered " + name + ". Type \"/discoveries\" to view your discoveries.");
        }
        return true;
    }

    // Handle /tm take and its args.
    private boolean handleTake(CommandSender sender, Player player, String[] args) {
        PlayerInventory inventory = ((Player)sender).getInventory();
        ItemStack item = inventory.getItemInMainHand();
        boolean loreAllowed = TransmuteIt.config.getBoolean("lore");
        if(!loreAllowed && item.getItemMeta().hasLore()) {
            player.sendMessage(ChatColor.RED + "This item has a custom lore set, and items with lore can't be transmuted as per the config.");
            return true;
        }
        Material type = item.getType();
        String name = type.toString();
        // If it's nothing
        if(name.equals("AIR")) {
            sender.sendMessage("Please hold an item to transmute it!");
            return true;
        }
        boolean enchantments = item.getEnchantments().size() > 0;
        boolean confirm = false;
        ItemStack[] items = inventory.all(item.getType()).values().toArray(new ItemStack[0]);
        int amount = 0;
        for (ItemStack itemStack : items) {
            amount += itemStack.getAmount();
        }
        int takeAmount;
        boolean hand = false;
        if(args.length >= 2) {
            if(args[1].toLowerCase().equals("hand")) {
                takeAmount = item.getAmount();
                hand = true;
            } else if(args[1].toLowerCase().equals("confirm")) {
                takeAmount = 1;
                confirm = true;
            } else {
                try {
                    takeAmount = Integer.parseInt(args[1]);
                } catch(NumberFormatException e) {
                    sender.sendMessage("Invalid number input! Please enter a number!");
                    return true;
                }
            }
        } else {
            takeAmount = amount;
        }
        if(args.length >= 3) {
            if (args[2].toLowerCase().equals("confirm")) {
                confirm = true;
            }
        }
        if(!confirm && enchantments) {
            sender.sendMessage(ChatColor.YELLOW + "WARNING: " + ChatColor.RED + "This item has enchantments! They will NOT be calculated into the EMC, are you sure you want to transmute this? Add \"confirm\" to the command if so!");
            return true;
        }

        if(takeAmount <= 0) {
            sender.sendMessage("Please select a value greater than 0!");
            return true;
        }
        if(amount - takeAmount < 0) {
            sender.sendMessage("You don't have enough of this item! (You only have " + amount + ")");
            return true;
        }

        // If it's something
        try {
            DataManager bob = new DataManager();
            int emc = TransmuteIt.json.getInt(type.toString());
            short currentDurability = item.getDurability();
            short maxDurability = type.getMaxDurability();
            if(maxDurability > 0) {
                emc = (int)((double)emc * (((double)maxDurability-(double)currentDurability)/(double)maxDurability));
            }
            if(hand) {
                item.setAmount(0);
            } else {
                int taken = 0;
                for (ItemStack itemStack : items) {
                    if (taken != takeAmount) {
                        int inStack = itemStack.getAmount();
                        if (inStack + taken <= takeAmount) {
                            itemStack.setAmount(0);
                            taken += inStack;
                        } else {
                            itemStack.setAmount(Math.abs(takeAmount - taken - inStack));
                            taken = takeAmount;
                        }
                    }
                }
            }
            UUID uuid = ((Player)sender).getUniqueId();
            int current = new DataManager().getEMC(uuid, player);
            int newEMC = current + (takeAmount * emc);
            bob.writeEMC(uuid, newEMC, player);
            sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
            if(!bob.discovered(uuid, name)) {
                sender.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
                if(bob.discoveries(uuid).size() == 0) {
                    sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.COLOR_CHAR + "7Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                }
                new DataManager().writeDiscovery(uuid, name);
            }
            sender.sendMessage(ChatColor.GREEN + "+ " + NumberFormat.getInstance().format(takeAmount * emc) + " EMC [Total: " + NumberFormat.getInstance().format(newEMC) + " EMC]");
            sender.sendMessage(ChatColor.RED + "- " + takeAmount + " " + capitalize(name));
            return true;
            // If there's no JSON file or it's not IN the JSON file
        } catch(org.json.JSONException e) {
            sender.sendMessage("This item has no set EMC value!");
            return true;
        }
    }

    // Handle /tm learn
    private boolean handleLearn(CommandSender sender) {
        PlayerInventory inventory = ((Player) sender).getInventory();
        ItemStack item = inventory.getItemInMainHand();
        boolean loreAllowed = TransmuteIt.config.getBoolean("lore");
        if(!loreAllowed && item.getItemMeta().hasLore()) {
            sender.sendMessage(ChatColor.RED + "This item has a custom lore set, and items with lore can't be transmuted as per the config.");
            return true;
        }
        Material type = item.getType();
        String name = type.toString();
        // If it's nothing
        if (name.equals("AIR")) {
            sender.sendMessage("Please hold an item to learn it!");
            return true;
        }
        // If it's something
        try {
            TransmuteIt.json.getInt(type.toString());
            DataManager bob = new DataManager();
            UUID uuid = ((Player) sender).getUniqueId();
            sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bTransmuting Stats" + ChatColor.COLOR_CHAR + "d ]--------");
            if (!bob.discovered(uuid, name)) {
                sender.sendMessage(ChatColor.COLOR_CHAR + "aYou've discovered " + name + "!");
                if (bob.discoveries(uuid).size() == 0) {
                    sender.sendMessage(ChatColor.COLOR_CHAR + "7" + ChatColor.ITALIC + "Now you can run /transmute get " + name + " [amount] to get this item, given you have enough EMC!");
                }
                new DataManager().writeDiscovery(uuid, name);
            } else {
                sender.sendMessage(ChatColor.COLOR_CHAR + "cYou've already discovered " + name + "!");
            }
            return true;
            // If there's no JSON file or it's not IN the JSON file
        } catch (org.json.JSONException e) {
            sender.sendMessage("This item has no set EMC value!");
            return true;
        }
    }

    // Handle /tm analyze
    private boolean handleAnalyze(CommandSender sender) {
        PlayerInventory inventory = ((Player) sender).getInventory();
        HashMap<String, Integer> amountMap = new HashMap<>();
        HashMap<String, Integer> emcValueMap = new HashMap<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            try {
                ItemStack item = inventory.getItem(i);
                String name = item.getType().toString();
                if (amountMap.containsKey(name)) {
                    int current = amountMap.get(name);
                    amountMap.replace(name, current + item.getAmount());
                } else {
                    amountMap.put(name, item.getAmount());
                }
                int emc = -1;
                try {
                    emc = TransmuteIt.json.getInt(name);
                } catch(org.json.JSONException ignored) {

                }
                if(item.getItemMeta() instanceof Damageable) {
                    Damageable damage = ((Damageable) item.getItemMeta());
                    emcValueMap.put(name, damage.getDamage() * emc);
                } else {
                    emcValueMap.put(name, emc);
                }
            } catch(NullPointerException ignored) {

            }
        }
        sender.sendMessage(ChatColor.COLOR_CHAR + "d--------[ " + ChatColor.COLOR_CHAR + "bInventory Analysis" + ChatColor.COLOR_CHAR + "d ]--------");
        Object[] keys = amountMap.keySet().toArray();
        Arrays.sort(keys);
        int total = 0;
        for(int i = 0; i < keys.length; i++) {
            String name = (String) keys[i];
            int amount = amountMap.get(name);
            try {
                int emc = TransmuteIt.json.getInt(name);
                sender.sendMessage(ChatColor.YELLOW + capitalize(name) + ": " + ChatColor.GREEN + NumberFormat.getInstance().format(emc * amount) + " EMC (" + NumberFormat.getInstance().format(emc) + " EMC each for " + amount + " items)");
                total += emc * amount;
            } catch(org.json.JSONException e) {
                sender.sendMessage(ChatColor.YELLOW + capitalize(name) + ": " + ChatColor.GREEN + "No EMC Value!");
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "TOTAL" + ": " + ChatColor.GREEN + NumberFormat.getInstance().format(total) + " EMC");
        return true;
    }

    public boolean handleVersion(CommandSender sender) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TransmuteIt");
        String current = plugin.getDescription().getVersion();
        String cversion = current.split("-")[0];
        int cbuild;
        try {
            cbuild = Integer.parseInt(current.split("b")[1]);
        } catch (NumberFormatException e) {
            cbuild = 0;
        }
        sender.sendMessage("Running TransmuteIt Version: " + current);
        sender.sendMessage("Checking for new updates...");

        int finalCbuild = cbuild;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] response = getLatestVersion();
            int lbuild = Integer.parseInt(response[0]);
            String lversion = response[1];

            int behind = lbuild - finalCbuild;
            if(behind == 0) {
                sender.sendMessage("You are running the latest build!");
            } else {
                sender.sendMessage("You are " + behind + " builds behind! (Latest: " + lversion + "-b" + lbuild + ")");
            }
        });

        return true;
    }

    public String[] getLatestVersion() {
        try {
            // We're connecting to TransmuteIt's Jenkins REST api
            URL url = new URL("https://jenkins.chew.pw/job/TransmuteIt/lastSuccessfulBuild/api/json");
            // Creating a connection
            URLConnection request = url.openConnection();
            request.setRequestProperty("User-Agent", "TransmuteIt Itself owo");
            request.connect();

            // Get response
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Convert to a JSON object
            JSONObject root = new JSONObject(response.toString()); // Convert the response to a json element
            String lbuild = root.getString("id");
            String lversion = root.getJSONArray("artifacts").getJSONObject(0).getString("displayPath").split("-")[1];
            return new String[]{lbuild, lversion};
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Handle tab completion
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        String[] completes = {"get", "take", "learn", "analyze", "version"};

        if (args.length == 1) {
            commands.add("help");
            for (String complete : completes) {
                if (sender.hasPermission("transmute.command." + complete)) {
                    commands.add(complete);
                }
            }
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length >= 2 && args[0].equals("help")) {
            StringUtil.copyPartialMatches(args[1], commands, completions);
        } else if (args[0].equals("get")) {
            if(args.length == 2) {
                List<Object> discoveries = new DataManager().discoveries(((Player)sender).getUniqueId());
                for (Object discovery : discoveries) {
                    commands.add(discovery.toString());
                }
            }
            StringUtil.copyPartialMatches(args[1], commands, completions);
        }
        Collections.sort(completions);
        return completions;
    }

    // The response found in /tm help or the paper in the GUI
    public static boolean helpResponse(CommandSender sender) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "-----[ " + ChatColor.AQUA + "Welcome to TransmuteIt!" + ChatColor.LIGHT_PURPLE + " ]-----");
        sender.sendMessage(ChatColor.YELLOW + "/transmute help" + ChatColor.GRAY + " - " + ChatColor.GREEN + "This command.");
        if(sender.hasPermission("transmute.command.version")) {
            sender.sendMessage(helpCommandFormatting("/transmute version", "Gets the version of the plugin and checks for updates."));
        }
        if(sender.hasPermission("transmute.command.take")) {
            sender.sendMessage(helpCommandFormatting("/transmute take (amount)", "Take [amount] of held item and convert to EMC."));
        }
        if(sender.hasPermission("transmute.command.get")) {
            sender.sendMessage(helpCommandFormatting("/transmute get [item] [amount]", "Get [amount] of [item] using EMC."));
        }
        if(sender.hasPermission("transmute.command.learn")) {
            sender.sendMessage(helpCommandFormatting("/transmute learn", "Discover the item without transmuting it."));
        }
        if(sender.hasPermission("transmute.command.analyze")) {
            sender.sendMessage(helpCommandFormatting("/transmute analyze", "Analyze your inventory for its EMC value."));
        }
        if(sender.hasPermission("transmute.command.getemc")) {
            sender.sendMessage(helpCommandFormatting("/getEMC (item)", "Get the EMC value of an item, blank for currently held item."));
        }
        if(sender.hasPermission("transmute.player.emc")) {
            sender.sendMessage(helpCommandFormatting("/emc", "View your EMC."));
        }
        if(sender.hasPermission("transmute.player.discoveries")) {
            sender.sendMessage(helpCommandFormatting("/discoveries (search term)", "View your Discoveries. Leave blank to see all, or type to search."));
        }
        if(sender.hasPermission("transmute.admin.emc.set")) {
            sender.sendMessage(helpCommandFormatting("/setEMC [amount]", "Set the EMC value of held item. Use 0 to remove."));
        }
        return true;
    }

    // Helpful command formatter that gives it colors
    private static String helpCommandFormatting(String command, String description) {
        return ChatColor.YELLOW + command + ChatColor.GRAY + " - " + ChatColor.GREEN + description;
    }

    // Response if there's missing permissions.
    public static boolean missingPermissionResponse(CommandSender sender, String missingo) {
        sender.sendMessage(ChatColor.RED + "You are missing the proper permission to run this command! You need: " + ChatColor.GREEN + missingo);
        return true;
    }
}
