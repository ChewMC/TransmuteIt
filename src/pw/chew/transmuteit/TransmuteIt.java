package pw.chew.transmuteit;
import java.io.IOException;
import java.io.FileOutputStream;
import com.google.common.io.Files;
import java.io.InputStream;
import java.io.FileNotFoundException;
import org.json.JSONObject;
import java.io.FileReader;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TransmuteIt extends JavaPlugin {
  static File emcFile;
  static JSONObject json;

  // Fired when plugin is first enabled
  public void onEnable() {
    saveDefaultConfig();
    FileConfiguration config = this.getConfig();

    while (true) {
      try {
        loadEMC();
        break;
      } catch (FileNotFoundException e) {
        System.out.println("[TransmuteIt] EMC File missing! Attempting to grab defaults from JAR.");

        //this.getPluginLoader().disablePlugin(this);
        try {
          copyFileFromJar();
        } catch (IOException f) {
          System.out.println("[TransmuteIt] Failed getting file! Cya!");
          this.getPluginLoader().disablePlugin(this);
          break;
        }
      }
    }

    this.getCommand("getemc").setExecutor(new GetEMCCommand());
    System.out.println("[TransmuteIt] Booted!");
  }
  // Fired when plugin is disabled
  public void onDisable() {

  }

  public void loadEMC() throws FileNotFoundException {
    emcFile = new File(getDataFolder(), "emc.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Map<String, Object> map = new HashMap<>();
    map = gson.fromJson(new FileReader(emcFile), new HashMap<String, Object>().getClass());
    String gsson = gson.toJson(map);
    json = new JSONObject(gsson);
  }

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
