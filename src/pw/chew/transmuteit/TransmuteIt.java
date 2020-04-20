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
  // Files & Config
  static JSONObject json;
  FileConfiguration config;
  static DataManager data;

  // Vault Hook
  static Economy econ;
  static boolean useEconomy = false;

  // Temporary place to store EMC & Discoveries.
  static Map<UUID, Integer> emc = new HashMap<>();
  static Map<UUID, ArrayList<String>> discoveries = new HashMap<>();

  // Fired when plugin is first enabled
  public void onEnable() {
    // Get and save config
    config = this.getConfig();
    config.addDefault("economy", false);
    config.options().copyDefaults(true);
    saveDefaultConfig();

    // Setup DataManager
    data = new DataManager();

    // bStats
    int pluginId = 6819;
    Metrics metrics = new Metrics(this, pluginId);

    // Setup Vault Hook
    if(!setupEconomy()) {
      System.out.println("[TransmuteIt] Could not find vault (or there's no economy hooked into it), economy won't work!");
      useEconomy = false;
    } else {
      System.out.println("[TransmuteIt] Vault HOOKED! Let's get this cash!");
      useEconomy = config.getBoolean("economy");
    }

    // Set up PAPI Hook
    if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
      new TransmuteItExpansion(this).register();
    }

    // Load EMC
     try {
       json = data.loadEMC();
      } catch (FileNotFoundException e) {
       System.out.println("[TransmuteIt] EMC File missing! Attempting to grab defaults from JAR.");
       try {
         data.copyFileFromJar();
         json = data.loadEMC();
       } catch (IOException f) {
         System.out.println("[TransmuteIt] Failed getting file! Cya!");
         this.getPluginLoader().disablePlugin(this);
       }
     }


    // Load Commands
    TransmuteCommand transmute = new TransmuteCommand();

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
}
