package pw.chew.transmuteit.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

/**
 * <h2>Item EMC Change Event</h2>
 *
 * Called when an item's EMC value is changed.
 * Getting the old or new EMC returns an OptionalInt, because they may not have existed before, or no longer exist.
 */
public class ItemEMCChangeEvent extends Event {
    private final HandlerList handlers = new HandlerList();

    private final String item;
    private final Integer oldEMC;
    private final int newEMC;

    public ItemEMCChangeEvent(String item, Integer oldEMC, int newEMC) {
        this.item = item;
        this.oldEMC = oldEMC;
        this.newEMC = newEMC;
    }

    /**
     * The name of the item whose EMC value was changed.
     * This should be Material.valueOf() friendly.
     *
     * @return The item whose EMC value was changed.
     */
    @NotNull
    public String getItem() {
        return item;
    }

    /**
     * The old EMC value of the item. This may be empty if the item did not have an EMC value before.
     * @return The old EMC value of the item.
     */
    @NotNull
    public OptionalInt getOldEMC() {
        return oldEMC == null ? OptionalInt.empty() : OptionalInt.of(oldEMC);
    }

    /**
     * The new EMC value of the item. This may be empty if the item no longer has an EMC value.
     * @return The new EMC value of the item.
     */
    @NotNull
    public OptionalInt getNewEMC() {
        return newEMC <= 0 ? OptionalInt.empty() : OptionalInt.of(newEMC);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
