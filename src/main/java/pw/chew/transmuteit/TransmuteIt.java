package pw.chew.transmuteit;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;
import pw.chew.transmuteit.commands.*;
import pw.chew.transmuteit.expansions.TransmuteItExpansion;
import pw.chew.transmuteit.listeners.JoinListener;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TransmuteIt extends JavaPlugin {
    // Files & Config
    public static JSONObject json;
    public static FileConfiguration config;
    public static DataManager data;
    public static boolean outdatedConfig = false;

    // Vault Hook
    public static Economy econ;
    public static boolean useEconomy = false;

    // Fired when plugin is first enabled
    public void onEnable() {
        // Get and save config
        config = this.getConfig();
        config.addDefault("economy", false);
        if(!config.contains("lore", true)) {
            this.getLogger().warning("Your config is outdated! Please delete your config and re-generate it.");
            outdatedConfig = true;
        }
        config.addDefault("lore", true);
        config.options().copyDefaults(true);
        saveDefaultConfig();

        // Setup DataManager
        data = new DataManager();

        // bStats
        int pluginId = 6819;
        new Metrics(this, pluginId);

        // Setup Vault Hook
        if(!setupEconomy()) {
            this.getLogger().warning("Could not find vault (or there's no economy hooked into it), economy won't work!");
            useEconomy = false;
        } else {
            this.getLogger().info("Vault HOOKED! Let's get this cash!");
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
            this.getLogger().info("EMC File missing! Attempting to grab defaults from JAR.");
            try {
                data.copyFileFromJar();
                json = data.loadEMC();
            } catch (IOException f) {
                this.getLogger().severe("Failed getting file! Shutting down.");
                this.getPluginLoader().disablePlugin(this);
            }
        }

        // Load Commands
        TransmuteCommand transmute = new TransmuteCommand(data);

        loadCommand("getemc", new GetEMCCommand());
        loadCommand("transmute", transmute).setTabCompleter(transmute);
        loadCommand("emc", new EMCCommand());
        loadCommand("setemc", new SetEMCCommand());
        loadCommand("discoveries", new DiscoveriesCommand());

        // Register Events
        getServer().getPluginManager().registerEvents(new TransmuteGUI(), this);
        getServer().getPluginManager().registerEvents(new TransmuteTakeGUI(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        // Magic Time
        getLogger().info("Booted!");
    }

    // Fired when plugin is disabled
    public void onDisable() {

    }

    public PluginCommand loadCommand(String command, CommandExecutor executor) {
        PluginCommand pluginCommand = getCommand(command);
        if(pluginCommand == null) {
            getLogger().severe("Command " + command + " could not load!");
            return null;
        }
        pluginCommand.setExecutor(executor);
        return pluginCommand;
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
