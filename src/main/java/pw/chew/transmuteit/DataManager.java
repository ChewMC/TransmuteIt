package pw.chew.transmuteit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class DataManager {
    private static File emcFile;
    private static TransmuteIt plugin;
    private static boolean useEconomy;
    private static Economy econ;
    private static JSONObject json;

    // Default data file
    private static final String DEFAULT_EMC = "{\"emc\":0,\"discoveries\":[]}";

    public DataManager(TransmuteIt transmuteIt, boolean useEconomyConfig, Economy economy, JSONObject jsonData) {
        plugin = transmuteIt;
        useEconomy = useEconomyConfig;
        econ = economy;
        json = jsonData;
    }

    /*
     * GETTERS
     */

    /**
     * An internal method for getting the data folder.
     *
     * @return The File object for /plugins/TransmuteIt/data
     */
    public static File getDataFolder() {
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
     * TODO - only require one argument
     *
     * @param uuid The UUID of the player being queried.
     * @param player The player being queried.
     * @return The amount of EMC the player has stored.
     */
    public int getEMC(UUID uuid, Player player) {
        // If using Vault, use its API, otherwise get the player's EMC from their data file
        return useEconomy ? (int) econ.getBalance(player) : getData(uuid).getInt("emc");
    }

    /**
     * Gets a player's data.
     *
     * @param uuid The player's UUID.
     * @return The JSONObject representation of the player's data.
     */
    public JSONObject getData(UUID uuid) {
        // Get the File object representing the player's data
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        // If it doesn't exist, copy a default file from the main jar
        if (!userFile.exists()) {
            try {
                copyFileFromJar(uuid);
            } catch (IOException e) {
                // If all else fails, say the file couldn't be created and return a default EMC file
                plugin.getLogger().severe("Unable to create EMC file for UUID " + uuid + "! EMC will NOT save for this player!");
                return new JSONObject(DEFAULT_EMC);
            }
        }
        StringBuilder data = new StringBuilder();
        // Read the file and write all data into the data string builder
        try (Scanner scanner = new Scanner(userFile)) {
            while (scanner.hasNextLine()) data.append(scanner.nextLine());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // The JSON object for storing the dats
        JSONObject bob;
        try {
            // Attempts to load the provided data
            bob = new JSONObject(data.toString());
        } catch (JSONException e) {
            // If that fails, load the default data and send an error in the console
            plugin.getLogger().severe("Failed to load the EMC file for UUID " + uuid + "! Loading the default file...");
            e.printStackTrace();
            bob = new JSONObject(DEFAULT_EMC);
        }
        // If there's data missing from the player file, re-add it where necessary
        if (bob.length() < 2) {
            if (!bob.has("emc")) bob.put("emc", 0);
            if (!bob.has("discoveries")) bob.put("discoveries", new HashMap<>());
        }
        // Then write it to the file
        try (PrintWriter writer = new PrintWriter(userFile)) {
            bob.write(writer);
        } catch (FileNotFoundException ignored) {
        }
        // And return it!
        return bob;
    }

    /**
     * Copies the default data file for individual players to a player's data file.
     *
     * @param uuid The UUID of the player.
     * @throws IOException When you swear on my christian minecraft server
     */
    public void copyFileFromJar(UUID uuid) throws IOException {
        copyFileFromJar(getDataFolder(), "/default.json", uuid.toString() + ".json");
    }

    /**
     * Writes a set amount of EMC to a player's data file. TODO - remove redundant argument
     *
     * @param uuid The UUID of the player.
     * @param amount The amount to set to.
     * @param player The player itself.
     */
    public void writeEMC(UUID uuid, int amount, Player player) {
        // If we're using Vault, use the vault API and take it from there.
        if (useEconomy) {
            econ.depositPlayer(player, amount - econ.getBalance(player));
            return;
        }
        // If not, use the inbuilt method.
        getDataAndWrite(uuid, data -> data.put("emc", amount), "Setting EMC to " + amount);
    }

    /**
     * Resets a player's discoveries.
     *
     * @param uuid The UUID of the player.
     */
    public void writeEmptyDiscovery(UUID uuid) {
        getDataAndWrite(uuid, data -> data.put("discoveries", new HashMap<>()), "Emptying discoveries");
    }

    /**
     * Adds a new item to a player's discoveries.
     *
     * @param uuid The UUID of the player.
     * @param item The item to be discovered.
     */
    public void writeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").put(item), "Adding discovery " + item);
    }

    /**
     * Removes an item from a player's discoveries.
     *
     * @param uuid The UUID of the player.
     * @param item The item to be removed.
     */
    public void removeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").toList().remove(item), "Removing discovery " + item);
    }

    private void getDataAndWrite(UUID uuid, Consumer<JSONObject> task, String action) {
        // Get the player's data file.
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        JSONObject data = getData(uuid);
        // Run the task required/specified.
        task.accept(data);
        // Then proceed to write the data.
        try (PrintWriter writer = new PrintWriter(userFile)) {
            data.write(writer);
        } catch (FileNotFoundException e) {
            plugin.getLogger().severe("Unable to write to EMC file for UUID " + uuid + "! EMC will NOT save for this player! Attempted action: " + action);
        }
    }

    /**
     * Loads the EMC data for the core plugin.
     */
    public JSONObject loadEMC() throws FileNotFoundException {
        emcFile = new File(plugin.getDataFolder(), "emc.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        HashMap<String, Object> map;
        try {
            map = gson.fromJson(new FileReader(emcFile), HashMap.class);

        } catch (JsonSyntaxException ex) {
            plugin.getLogger().severe("The main EMC file has a syntax error in it! Loading the default one...");
            try {
                copyFileFromJar();
            } catch (IOException e) {
                e.printStackTrace();
                return new JSONObject();
            }
            map = gson.fromJson(new FileReader(emcFile), HashMap.class);
        }
        String gsson = gson.toJson(map);
        return new JSONObject(gsson);
    }

    public void writeToEMCFile() {
        try (PrintWriter writer = new PrintWriter(emcFile)) {
            json.write(writer);
        } catch (FileNotFoundException e) {
            plugin.getLogger().severe("Unable to write to the main EMC file! EMC will NOT save!");
        }
    }

    public boolean discovered(UUID uuid, String item) {
        JSONObject data = getData(uuid);
        List<Object> bob = data.getJSONArray("discoveries").toList();
        return bob.contains(item);
    }

    // Copy default EMC values from JSON file hidden in the JAR.
    public void copyFileFromJar() throws IOException {
        copyFileFromJar(plugin.getDataFolder(),"/emc.json", "emc.json");
    }

    private void copyFileFromJar(File folder, String resource, String child) throws IOException {
        File target = new File(folder, child);
        if (target.exists()) return;
        InputStream initialStream = getClass().getResourceAsStream(resource);
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);
        FileOutputStream out = new FileOutputStream(target);
        out.write(buffer);
        out.close();
    }

    public List<Object> discoveries(UUID uuid) {
        JSONObject data = getData(uuid);
        return data.getJSONArray("discoveries").toList();
    }

    public JSONObject getEMCValues() {
        return json;
    }

    public int getAmountOfItemsWithEMC() {
        return getEMCValues().length();
    }
}
