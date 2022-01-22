package pw.chew.transmuteit;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pw.chew.transmuteit.commands.DiscoveriesCommand;
import pw.chew.transmuteit.commands.EMCCommand;
import pw.chew.transmuteit.commands.GetEMCCommand;
import pw.chew.transmuteit.commands.SetEMCCommand;
import pw.chew.transmuteit.commands.TransmuteCommand;
import pw.chew.transmuteit.expansions.TransmuteItExpansion;
import pw.chew.transmuteit.guis.TransmuteGUI;
import pw.chew.transmuteit.guis.TransmuteTakeGUI;
import pw.chew.transmuteit.listeners.JoinListener;
import pw.chew.transmuteit.utils.DataManager;

public class TransmuteIt extends JavaPlugin {
    private static boolean outdatedConfig = false;

    // Vault Hook
    private static Economy econ;

    // Fired when plugin is first enabled
    public void onEnable() {
        // Get and save config
        // Files & Config
        FileConfiguration config = this.getConfig();
        config.addDefault("economy", false);
        config.addDefault("lore", true);
        if(!config.contains("lore", true)) {
            this.getLogger().warning("Your config is outdated! Please delete your config and re-generate it.");
            outdatedConfig = true;
        }
        config.options().copyDefaults(true);
        saveDefaultConfig();

        // bStats
        int pluginId = 6819;
        new Metrics(this, pluginId);

        // Setup Vault Hook
        boolean useEconomy = false;
        if(!setupEconomy()) {
            this.getLogger().warning("Could not find vault (or there's no economy hooked into it), economy won't work!");
        } else {
            this.getLogger().info("Vault HOOKED! Let's get this cash!");
            useEconomy = config.getBoolean("economy");
        }

        // Setup DataManager
        DataManager.setInfo(this, useEconomy, econ);

        // Set up PAPI Hook
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new TransmuteItExpansion(this).register();
        }

        // Load EMC
        DataManager.loadEMC();

        // Load Commands
        TransmuteCommand transmute = new TransmuteCommand(config);

        loadCommand("getemc", new GetEMCCommand());
        loadCommand("transmute", transmute).setTabCompleter(transmute);
        loadCommand("emc", new EMCCommand());
        loadCommand("setemc", new SetEMCCommand());
        loadCommand("discoveries", new DiscoveriesCommand());

        // Register Events
        getServer().getPluginManager().registerEvents(new TransmuteGUI(config), this);
        getServer().getPluginManager().registerEvents(new TransmuteTakeGUI(config), this);
        getServer().getPluginManager().registerEvents(new JoinListener(outdatedConfig), this);

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
