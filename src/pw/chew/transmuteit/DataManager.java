package pw.chew.transmuteit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class DataManager {
  static File emcFile;

  public DataManager() {

  }

  public static File getDataFolder() {
    File dataFolder = ((TransmuteIt)Bukkit.getPluginManager().getPlugin("TransmuteIt")).getDataFolder();
    File loc = new File(dataFolder + "/data");
    if(!loc.exists()) {
      loc.mkdirs();
    }
    return loc;
  }

  public int getEMC(UUID uuid, Player player) {
    if(TransmuteIt.useEconomy) {
      double emc = TransmuteIt.econ.getBalance(player);
      return (int)emc;
    } else {
      return getData(uuid).getInt("emc");
    }
  }

  public JSONObject getData(UUID uuid) {
    createDataFileIfNoneExists(uuid);
    prepareDataFile(uuid);
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    StringBuilder data = new StringBuilder();
    try {
      Scanner scanner = new Scanner(userFile);
      while (scanner.hasNextLine()) {
        data.append(scanner.nextLine());
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    JSONObject bob;
    try {
      bob = new JSONObject(data.toString());
    } catch(JSONException e) {
      e.printStackTrace();
      bob = new JSONObject("{\"emc\":0,\"discoveries\":[]}");
    }
    return bob;
  }

  public void createDataFileIfNoneExists(UUID uuid) {
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    if(!userFile.exists()) {
      try {
        copyFileFromJar(uuid);
      } catch (IOException e) {
        System.out.println("[TransmuteIt] Unable to create EMC file! EMC will NOT save!");
      }
    }
  }

  public void copyFileFromJar(UUID uuid) throws IOException {
    String name = "/default.json";
    File target = new File(getDataFolder(), uuid.toString() + ".json");
    if (!target.exists()) {
      InputStream initialStream = getClass().getResourceAsStream(name);
      byte[] buffer = new byte[initialStream.available()];
      initialStream.read(buffer);
      FileOutputStream out = new FileOutputStream(target);
      out.write(buffer);
      out.close();
    }
  }

  public void prepareDataFile(UUID uuid) {
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    PrintWriter writer;
    StringBuilder data = new StringBuilder();
    try {
      Scanner scanner = new Scanner(userFile);
      while (scanner.hasNextLine()) {
        data.append(scanner.nextLine());
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    try {
      writer = new PrintWriter(userFile);
    } catch(FileNotFoundException e) {
      return;
    }
    JSONObject bob;
    try {
      bob = new JSONObject(data.toString());
    } catch(JSONException e) {
      e.printStackTrace();
      bob = new JSONObject("{\"emc\":0,\"discoveries\":[]}");
    }
    if(bob.length() < 2) {
      if(!bob.has("emc")) {
        bob.put("emc", 0);
      } else if(!bob.has("discoveries")){
        Map<String, Object> discoveries = new HashMap<>();
        bob.put("discoveries", discoveries);
      }
    }
    bob.write(writer);
    writer.close();
  }

  public void writeEMC(UUID uuid, int amount, Player player) {
    if(TransmuteIt.useEconomy) {
      double balance = TransmuteIt.econ.getBalance(player);
      EconomyResponse r = TransmuteIt.econ.withdrawPlayer(player, balance);
      EconomyResponse s = TransmuteIt.econ.depositPlayer(player, amount);
    } else {
      File userFile = new File(getDataFolder(), uuid.toString() + ".json");
      try {
        JSONObject data = getData(uuid);
        data.put("emc", amount);
        PrintWriter writer = new PrintWriter(userFile);
        data.write(writer);
        writer.close();
      } catch(FileNotFoundException e) {
        System.out.println("[TransmuteIt] Unable to write to EMC file! EMC will NOT save!");
      }
    }
  }

  public void writeEmptyDiscovery(UUID uuid) {
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    try {
      JSONObject data = getData(uuid);
      Map<String, Object> discoveries = new HashMap<>();
      data.put("discoveries", discoveries);
      PrintWriter writer = new PrintWriter(userFile);
      data.write(writer);
      writer.close();
    } catch(FileNotFoundException e) {
      System.out.println("[TransmuteIt] Unable to write to EMC file! EMC will NOT save!");
    }
  }

  public void writeDiscovery(UUID uuid, String item) {
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    try {
      JSONObject data = getData(uuid);
      data.getJSONArray("discoveries").put(item);
      PrintWriter writer = new PrintWriter(userFile);
      data.write(writer);
      writer.close();
    } catch(FileNotFoundException e) {
      System.out.println("[TransmuteIt] Unable to write to EMC file! EMC will NOT save!");
    }
  }

  public void removeDiscovery(UUID uuid, String item) {
    File userFile = new File(getDataFolder(), uuid.toString() + ".json");
    try {
      JSONObject data = getData(uuid);
      data.getJSONArray("discoveries").toList().remove(item);
      PrintWriter writer = new PrintWriter(userFile);
      data.write(writer);
      writer.close();
    } catch(FileNotFoundException e) {
      System.out.println("[TransmuteIt] Unable to write to EMC file! EMC will NOT save!");
    }
  }

  // Load EMC values from JSON file
  public JSONObject loadEMC() throws FileNotFoundException {
    File dataFolder = Bukkit.getPluginManager().getPlugin("TransmuteIt").getDataFolder();
    emcFile = new File(dataFolder, "emc.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    HashMap<String, Object> map;
    map = gson.fromJson(new FileReader(emcFile), HashMap.class);
    String gsson = gson.toJson(map);
    return new JSONObject(gsson);
  }

  public void writeToEMCFile() {
    try {
      PrintWriter writer = new PrintWriter(emcFile);
      TransmuteIt.json.write(writer);
      writer.close();
    } catch(FileNotFoundException e) {
      System.out.println("[TransmuteIt] Unable to write to EMC file! EMC will NOT save!");
    }
  }

  public boolean discovered(UUID uuid, String item) {
    JSONObject data = getData(uuid);
    List<Object> bob = data.getJSONArray("discoveries").toList();
    return bob.contains(item);
  }

  // Copy default EMC values from JSON file hidden in the JAR.
  public void copyFileFromJar() throws IOException {
    String name = "/emc.json";
    File dataFolder = Bukkit.getPluginManager().getPlugin("TransmuteIt").getDataFolder();
    File target = new File(dataFolder, "emc.json");
    if (!target.exists()) {
      InputStream initialStream = getClass().getResourceAsStream(name);
      byte[] buffer = new byte[initialStream.available()];
      initialStream.read(buffer);
      FileOutputStream out = new FileOutputStream(target);
      out.write(buffer);
      out.close();
    }
  }

  public List<Object> discoveries(UUID uuid) {
    JSONObject data = getData(uuid);
    return data.getJSONArray("discoveries").toList();
  }

  public JSONObject getEMCValues() {
    return TransmuteIt.json;
  }

  public int getAmountOfItemsWithEMC() {
    return getEMCValues().length();
  }
}
