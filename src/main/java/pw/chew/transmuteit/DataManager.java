package pw.chew.transmuteit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static final String DEFAULT_EMC = "{\"emc\":0,\"discoveries\":[]}";

    public DataManager(TransmuteIt transmuteIt, boolean useEconomyConfig, Economy economy, JSONObject jsonData) {
        plugin = transmuteIt;
        useEconomy = useEconomyConfig;
        econ = economy;
        json = jsonData;
    }

    public static File getDataFolder() {
        File dataFolder = plugin.getDataFolder();
        File loc = new File(dataFolder + "/data");
        if (!loc.exists()) loc.mkdirs();
        return loc;
    }

    /**
     * Used to get the EMC of a given player. TODO - only require one argument
     *
     * @param uuid The UUID of the player being queried.
     * @param player The player being queried.
     * @return The amount of EMC the player has stored.
     */
    public int getEMC(UUID uuid, Player player) {
        return useEconomy ? (int) econ.getBalance(player) : getData(uuid).getInt("emc");
    }

    public JSONObject getData(UUID uuid) {
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        if (!userFile.exists()) {
            try {
                copyFileFromJar(uuid);
            } catch (IOException e) {
                plugin.getLogger().severe("Unable to create EMC file! EMC will NOT save!");
            }
        }
        StringBuilder data = new StringBuilder();
        try (Scanner scanner = new Scanner(userFile)) {
            while (scanner.hasNextLine()) data.append(scanner.nextLine());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try (PrintWriter writer = new PrintWriter(userFile)) {
            JSONObject bob;
            try {
                bob = new JSONObject(data.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                bob = new JSONObject(DEFAULT_EMC);
            }
            if (bob.length() < 2) {
                if (!bob.has("emc")) bob.put("emc", 0);
                if (!bob.has("discoveries")) bob.put("discoveries", new HashMap<>());
            }
            bob.write(writer);
            return bob;
        } catch (FileNotFoundException ignored) {
            // TODO - check
        }
        return new JSONObject(DEFAULT_EMC);
    }

    public void copyFileFromJar(UUID uuid) throws IOException {
        copyFileFromJar(getDataFolder(), "/default.json", uuid.toString() + ".json");
    }

    public void writeEMC(UUID uuid, int amount, Player player) {
        if (useEconomy) {
            econ.depositPlayer(player, amount - econ.getBalance(player));
            return;
        }
        getDataAndWrite(uuid, data -> data.put("emc", amount));
    }

    public void writeEmptyDiscovery(UUID uuid) {
        getDataAndWrite(uuid, data -> data.put("discoveries", new HashMap<>()));
    }

    public void writeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").put(item));
    }

    public void removeDiscovery(UUID uuid, String item) {
        getDataAndWrite(uuid, data -> data.getJSONArray("discoveries").toList().remove(item));
    }

    private void getDataAndWrite(UUID uuid, Consumer<JSONObject> task) {
        File userFile = new File(getDataFolder(), uuid.toString() + ".json");
        JSONObject data = getData(uuid);
        task.accept(data);
        try (PrintWriter writer = new PrintWriter(userFile)) {
            data.write(writer);
        } catch (FileNotFoundException e) {
            plugin.getLogger().severe("Unable to write to EMC file! EMC will NOT save!");
        }
    }

    // Load EMC values from JSON file
    public JSONObject loadEMC() throws FileNotFoundException {
        emcFile = new File(plugin.getDataFolder(), "emc.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        HashMap<String, Object> map = gson.fromJson(new FileReader(emcFile), HashMap.class);
        String gsson = gson.toJson(map);
        return new JSONObject(gsson);
    }

    public void writeToEMCFile() {
        try (PrintWriter writer = new PrintWriter(emcFile)) {
            json.write(writer);
        } catch (FileNotFoundException e) {
            plugin.getLogger().severe("Unable to write to EMC file! EMC will NOT save!");
        }
    }

    public boolean discovered(UUID uuid, String item) {
        JSONObject data = getData(uuid);
        List<Object> bob = data.getJSONArray("discoveries").toList();
        return bob.contains(item);
    }

    // Copy default EMC values from JSON file hidden in the JAR.
    public void copyFileFromJar() throws IOException {
        copyFileFromJar(plugin.getDataFolder(),"emc.json", "emc.json");
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
