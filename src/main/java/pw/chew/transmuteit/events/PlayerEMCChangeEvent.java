package pw.chew.transmuteit.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * <h2>EMC Change Event</h2>
 *
 * This event is called when an EMC change for a player occurs.<br>
 * This is called right before the EMC is changed. But, if it is cancelled, the player will not be notified.
 */
public class PlayerEMCChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final long emcChange;
    private boolean cancelled;

    public PlayerEMCChangeEvent(Player player, long emcChange) {
        this.player = player;
        this.emcChange = emcChange;
    }

    /**
     * Gets the target player whose EMC is changing.
     *
     * @return The target player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the amount of EMC that is changing. This may be negative.
     *
     * @return The amount of EMC that is changing.
     */
    public long getEMCChange() {
        return emcChange;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels this event. The player will still be notified that they earned or lost emc, but the change will not be applied.
     * Cancelling takes effect even if economy is enabled.
     *
     * @param cancelled Whether this event should be cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
