package pw.chew.transmuteit.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.text.NumberFormat;

/**
 * Set of methods useful for handling chat messages.
 */
public class ChatHelper {
    /**
     * Send an "Error" message to a player. This always returns true.
     * @param sender The player to send the message to.
     * @param message The message to send.
     * @param format Any extra arguments to send to String.format.
     * @return Always returns true.
     */
    public static boolean sendError(CommandSender sender, String message, Object... format) {
        // Iterate over each format argument and, if it's a number, format it.
        for (int i = 0; i < format.length; i++) {
            if (format[i] instanceof Number) {
                format[i] = NumberFormat.getInstance().format(format[i]);
            }
        }

        sender.sendMessage(ChatColor.RED + String.format(message, format));
        return true;
    }
}
