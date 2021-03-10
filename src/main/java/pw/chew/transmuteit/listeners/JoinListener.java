package pw.chew.transmuteit.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private static boolean outdatedConfig;

    public JoinListener(boolean isOutdatedConfig) {
        outdatedConfig = isOutdatedConfig;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Code that happens when a player joins
        Player player = event.getPlayer();
        if(player.hasPermission("transmute.admin.notices") && outdatedConfig) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[TransmuteIt] " + ChatColor.AQUA + "Your TransmuteIt config is out of date! Please re-generate your config or add missing values to prevent problems. Please visit " + ChatColor.GREEN + "https://github.com/ChewMC/TransmuteIt/wiki/Configuration");
        }
    }
}
