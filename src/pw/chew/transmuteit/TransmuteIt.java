package pw.chew.transmuteit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TransmuteIt extends JavaPlugin {
  // Fired when plugin is first enabled
  public void onEnable() {
    FileConfiguration config = this.getConfig();
    saveConfig();

    this.getCommand("getemc").setExecutor(new GetEMCCommand());
    System.out.println("[TransmuteIt] Booted!");
  }
  // Fired when plugin is disabled
  public void onDisable() {

  }
}
