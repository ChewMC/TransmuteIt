package pw.chew.transmuteit.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.chew.transmuteit.TransmuteIt;
import pw.chew.transmuteit.events.ItemEMCChangeEvent;
import pw.chew.transmuteit.events.PlayerEMCChangeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <h2>Data Manager</h2>
 *
 * This is the class that handles the access to the files, like emc.json or data files.
 *
 * <br>This also has helper methods as an intermediary between saving and loading manually.
 */
public class DataManager {
    private static File emcFile;
    private static TransmuteIt plugin;
    private static Economy econ;
    private static JSONObject json = null;
    private final static Map<UUID, JSONObject> userCache = new HashMap<>();

    /**
     * Default EMC value for a new player.
     */
    private static final JSONObject DEFAULT_EMC = new JSONObject("{\"emc\":0,\"discoveries\":[]}");

    /**
     * Sets info for the manager to use. Should only be called once on initialization.
     *
     * @param transmuteIt A reference to the main plugin.
     * @param economy The Economy API.
     */
    public static void setInfo(TransmuteIt transmuteIt, Economy economy) {
        plugin = transmuteIt;
        econ = economy;
    }

    /**
     * An internal method for getting the data folder.
     *
     * @return The File object for /plugins/TransmuteIt/data
     */
    private static File getDataFolder() {
        // Get the main plugin data folder ("TransmuteIt")
        File dataFolder = plugin.getDataFolder();
        // Get the internal data folder
        File loc = new File(dataFolder, "data");
        // If it doesn't exist, make sure it can be created
        if (!loc.exists()) {
            if (!loc.mkdir()) plugin.getLogger().severe("Failed to create the data folder!");
        }
        // Return the folder
        return loc;
    }

    /**
     * Used to get the EMC of a given player.
     *
     * @param player The player being queried.
     * @return The amount of EMC the player has stored.
     */
    public static long getEMC(OfflinePlayer player) {
        // If using Vault, use its API, otherwise get the player's EMC from their data file
        return plugin.getConfig().getBoolean("economy", false) ? (long) econ.getBalance(player) : getData(player.getUniqueId()).getLong("emc");
    }

    /**
     * Gets a player's data. If it does not exist, a new one will not be created, and the default EMC will be used.
     * It will attempt to load the data from cache, if it exists. Otherwise, it will pull from the file.
     *
     * @param uuid The player's UUID.
     * @return The JSONObject representation of the player's data.
     */
    private static JSONObject getData(UUID uuid) {
        // Attempt to get the player's data from the cache
        JSONObject data = userCache.get(uuid);
        if (data != null) {
            // Revisit caching later
            // return data;
        }

        // Get the File object representing the player's data
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        // If it doesn't exist, return a default EMC value
        if (!userFile.exists()) {
            userCache.put(uuid, DEFAULT_EMC);
            return DEFAULT_EMC;
        }

        try {
            // Read the contents of the file to a string
            String file = String.join("\n", Files.readAllLines(userFile.toPath()));
            JSONObject json = new JSONObject(file);
            userCache.put(uuid, json);
            return json;
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to read data file for " + uuid + "! Returning default file.");
            e.printStackTrace();
            userCache.put(uuid, DEFAULT_EMC);
            return DEFAULT_EMC;
        } catch (JSONException e) {
            plugin.getLogger().severe("Corrupt data file for " + uuid + "! Returning default file.");
            userCache.put(uuid, DEFAULT_EMC);
            return DEFAULT_EMC;
        }
    }

    /**
     * Writes a set amount of EMC to a player's data file, or to the attached economy if Vault is being used.
     *
     * @param player The player itself.
     * @param amount The amount to set to.
     */
    public static void writeEMC(Player player, long amount) {
        PlayerEMCChangeEvent event = new PlayerEMCChangeEvent(player, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;

        // If we're using Vault, use the vault API and take it from there.
        if (plugin.getConfig().getBoolean("economy", false)) {
            econ.depositPlayer(player, amount - econ.getBalance(player));
            return;
        }

        // If not, use the inbuilt method.
        getDataAndWrite(player.getUniqueId(), data -> data.put("emc", amount), "Setting EMC to " + amount);
    }

    /**
     * Adds a new item to a player's discoveries.
     *
     * @param uuid The UUID of the player.
     * @param item The item to be discovered.
     */
    public static void writeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").put(item), "Adding discovery " + item);
    }

    /**
     * Adds multiple items to a player's discoveries.
     *
     * @param uuid The UUID of the player.
     * @param items The items to be discovered.
     */
    public static void writeDiscoveries(UUID uuid, List<String> items) {
        getDataAndWrite(uuid, data -> {
            JSONArray discoveries = data.getJSONArray("discoveries");
            for (String item : items) {
                discoveries.put(item);
            }
        }, "Adding discoveries");
    }

    /**
     * Removes an item from a player's discoveries.
     *
     * @param uuid The UUID of the player.
     * @param item The item to be removed.
     */
    public static void removeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").toList().remove(item), "Removing discovery " + item);
    }

    /**
     * Gets and writes data to a player data file with a specified task.
     *
     * @param uuid The UUID of the player.
     * @param task The task used to write data.
     * @param action The action performed, used for debugging purposes in the event of failure.
     */
    private static void getDataAndWrite(UUID uuid, Consumer<JSONObject> task, String action) {
        // Get the player's data file.
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        JSONObject data = getData(uuid);
        // Run the task required/specified.
        task.accept(data);
        // Then proceed to write the data.
        try (FileWriter writer = new FileWriter(userFile)) {
            data.write(writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to write to EMC file for UUID " + uuid + "! EMC will NOT save for this player! Attempted action: " + action);
        }
    }

    /**
     * Loads the EMC data for the core plugin.
     * This will attempt to load the file, and if it fails, it will create a new one.
     * If there's a syntax error, it will attempt to fix it.
     * If there's an IO error, the plugin will crash.
     */
    public static void loadEMC() {
        emcFile = new File(plugin.getDataFolder(), "emc.json");
        if (!emcFile.exists()) {
            plugin.saveResource("emc.json", true);
            emcFile = new File(plugin.getDataFolder(), "emc.json");
        }

        try {
            // Read the contents of the file to a string
            String file = String.join("\n", Files.readAllLines(emcFile.toPath()));
            json = new JSONObject(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to read EMC file! Shutting down.");
            e.printStackTrace();
            plugin.getPluginLoader().disablePlugin(plugin);
        } catch (JSONException e) {
            plugin.getLogger().severe("The main EMC file has a syntax error in it! Loading the default one...");
            plugin.saveResource("emc.json", true);
            loadEMC();
        }
    }

    /**
     * Writes data to the emc.json file.
     */
    private static void writeToEMCFile() {
        // Write the EMC to a file
        try (FileWriter writer = new FileWriter(emcFile)) {
            json.write(writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to write to EMC file! EMC will NOT save!");
        }
    }

    /**
     * Checks to see if a player has discovered an item
     * @param player The player to check
     * @param item The item to check
     * @return True if the player has discovered the item, false otherwise
     */
    public static boolean hasDiscovered(OfflinePlayer player, String item) {
        JSONObject data = getData(player.getUniqueId());
        return data.getJSONArray("discoveries").toList().contains(item.toUpperCase(Locale.ROOT));
    }

    /**
     * Retrieves a list of discoveries for a player.
     *
     * @param player The player.
     * @return A list of discoveries.
     */
    public static List<String> discoveries(OfflinePlayer player) {
        JSONObject data = getData(player.getUniqueId());
        return data.getJSONArray("discoveries").toList().stream().map(Object::toString).toList();
    }

    /**
     * Checks to see if this player has no discoveries.
     * @param player The player to check
     * @return True if the player has no discoveries, false otherwise
     */
    public static boolean hasNoDiscoveries(OfflinePlayer player) {
        return discoveries(player).size() == 0;
    }

    /**
     * Gets all the EMC values.
     *
     * @return A map of all the EMC values.
     */
    public static JSONObject getEMCValues() {
        if (json == null) loadEMC();
        return json;
    }

    /**
     * Retrieves the EMC value for an item.
     * This is simply the value from the EMC file, and does not include any bonuses.
     *
     * @param item The item to get the EMC value for.
     * @return The EMC value, or 0 if the item has no EMC.
     */
    public static int getItemEMC(String item) {
        try {
            return json.getInt(item.toUpperCase(Locale.ROOT));
        } catch (JSONException e) {
            return 0;
        }
    }

    /**
     * Returns the total number of entries in the EMC file.
     * @return The total number of entries in the EMC file.
     */
    public static int getAmountOfItemsWithEMC() {
        return getEMCValues().length();
    }

    /**
     * Changes the EMC value of an item.
     * @param item The item to change.
     * @param value The new EMC value.
     */
    public static void setEMCValue(String item, int value) {
        item = item.toUpperCase(Locale.ROOT);

        ItemEMCChangeEvent event = new ItemEMCChangeEvent(item, json.getInt(item), value);
        Bukkit.getPluginManager().callEvent(event);

        if (value > 0) {
            json.put(item, value);
        } else {
            json.remove(item);
        }
        writeToEMCFile();
    }
}
