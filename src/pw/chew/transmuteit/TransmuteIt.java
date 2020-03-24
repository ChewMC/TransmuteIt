package pw.chew.transmuteit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;
import pw.chew.transmuteit.expansions.TransmuteItExpansion;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransmuteIt extends JavaPlugin {
  // Files
  static File emcFile;
  static JSONObject json;
  FileConfiguration config;

  // Vault Hook
  static Economy econ;
  static boolean useEconomy = false;

  // Temporary place to store EMC & Discoveries.
  static Map<UUID, Integer> emc = new HashMap<>();
  static Map<UUID, ArrayList<String>> discoveries = new HashMap<>();

  // Fired when plugin is first enabled
  public void onEnable() {
    config = this.getConfig();
    config.addDefault("economy", false);
    config.options().copyDefaults(true);
    saveDefaultConfig();

    int pluginId = 6819; // <-- Replace with the id of your plugin!
    Metrics metrics = new Metrics(this, pluginId);

    if(!setupEconomy()) {
      System.out.println("[TransmuteIt] Could not find vault (or there's no economy hooked into it), economy won't work!");
      useEconomy = false;
    } else {
      System.out.println("[TransmuteIt] Vault HOOKED! Let's get this cash!");
      if(config.getBoolean("economy")) {
        useEconomy = true;
      } else {
        useEconomy = false;
      }
    }

    if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
      new TransmuteItExpansion(this).register();
    }

    while (true) {
      try {
        loadEMC();
        break;
      } catch (FileNotFoundException e) {
        System.out.println("[TransmuteIt] EMC File missing! Attempting to grab defaults from JAR.");
        try {
          copyFileFromJar();
        } catch (IOException f) {
          System.out.println("[TransmuteIt] Failed getting file! Cya!");
          this.getPluginLoader().disablePlugin(this);
          break;
        }
      }
    }

    TransmuteCommand transmute = new TransmuteCommand();

    // Load Commands
    this.getCommand("getemc").setExecutor(new GetEMCCommand());
    this.getCommand("transmute").setExecutor(transmute);
    this.getCommand("emc").setExecutor(new EMCCommand());
    this.getCommand("setemc").setExecutor(new SetEMCCommand());
    this.getCommand("discoveries").setExecutor(new DiscoveriesCommand());

    // Load tab completes
    this.getCommand("transmute").setTabCompleter(transmute);

    // Register Events
    getServer().getPluginManager().registerEvents(new DiscoveriesGUI(), this);
    getServer().getPluginManager().registerEvents(new TransmuteGUI(), this);
    getServer().getPluginManager().registerEvents(new TransmuteTakeGUI(), this);

    // Magic Time
    getLogger().info("Booted!");
  }

  // Fired when plugin is disabled
  public void onDisable() {

  }

  // Setup Vault Economy Hook
  private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }
    econ = rsp.getProvider();
    return true;
  }

  // Load EMC values from JSON file
  public void loadEMC() throws FileNotFoundException {
    emcFile = new File(getDataFolder(), "emc.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Map<String, Object> map = new HashMap<>();
    map = gson.fromJson(new FileReader(emcFile), new HashMap<String, Object>().getClass());
    String gsson = gson.toJson(map);
    json = new JSONObject(gsson);
  }

  // Copy default EMC values from JSON file hidden in the JAR.
  private void copyFileFromJar() throws IOException {
    String name = "/emc.json";
    File target = new File(getDataFolder(), "emc.json");
    if (!target.exists()) {
      InputStream initialStream = getClass().getResourceAsStream(name);
      byte[] buffer = new byte[initialStream.available()];
      initialStream.read(buffer);
      FileOutputStream out = new FileOutputStream(target);
      out.write(buffer);
      out.close();
    }
  }
}
