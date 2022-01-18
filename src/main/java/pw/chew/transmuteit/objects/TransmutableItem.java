package pw.chew.transmuteit.objects;

import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pw.chew.transmuteit.TransmuteIt;

import java.util.Map;

/**
 * A wrapper for an ItemStack that can be transmuted.
 */
public record TransmutableItem(ItemStack item) {
    /**
     * The EMC bonus for enchantment
     */
    private static final long ENCH_EMC_BONUS = 5_000;

    /**
     * Gets the item.
     *
     * @return the item
     */
    public ItemStack getItem() {
        return item;
    }

    public boolean hasLore() {
        return item.getItemMeta().hasLore();
    }

    /**
     * Gets the type of item
     */
    public @NotNull Material getType() {
        return item.getType();
    }

    public int getAmount() {
        return item.getAmount();
    }

    public boolean isEnchanted() {
        return !item.getEnchantments().isEmpty();
    }

    /**
     * Gets this item's EMC based on the item itself, not taking in other factors.
     *
     * @return this item's EMC
     * @throws IllegalArgumentException if this item has no EMC value
     */
    public int getItemEMC() {
        if (hasEMC()) {
            return TransmuteIt.getDataManager().getEMCValues().getInt(item.getType().toString());
        } else {
            throw new IllegalArgumentException("No EMC value for item " + item.getType());
        }
    }

    /**
     * Checks to see if this item has an EMC value.
     *
     * @return true if this item has an EMC value, false otherwise
     */
    public boolean hasEMC() {
        return TransmuteIt.getDataManager().getEMCValues().has(item.getType().toString());
    }

    /**
     * Gets the EMC value of this item, taking in factors such as:
     * <ul>
     *     <li>The item's durability</li>
     *     <li>The item's enchantments (Paper only)</li>
     *     <li>Amount of items</li>
     * </ul>
     *
     * @param amount The amount of items to factor in
     * @return This item stack's EMC value
     */
    public long getEMC(double amount) {
        if (!hasEMC()) {
            throw new IllegalArgumentException("No EMC value for item " + item.getType());
        }

        double maxDurability;
        double currentDurability = 0;
        long emc = getItemEMC();
        Material type;
        type = item.getType();

        // Handle enchantments. Paper only.
        // Code is based on ProjectE code. Thanks guys!
        // See https://github.com/sinkillerj/ProjectE/blob/mc1.16.x/src/main/java/moze_intel/projecte/emc/nbt/processor/EnchantmentProcessor.java
        if (PaperLib.isPaper() && isEnchanted()) {
            Map<Enchantment, Integer> enchants = item.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                int rarityWeight = entry.getKey().getRarity().getWeight();
                if (rarityWeight > 0) {
                    emc = Math.addExact(emc, Math.multiplyExact(ENCH_EMC_BONUS / rarityWeight, entry.getValue()));
                }
            }
        }

        // Handle durability
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damage) {
            currentDurability = damage.getDamage();
        }
        maxDurability = type.getMaxDurability();
        if (currentDurability > maxDurability) {
            currentDurability = maxDurability;
        }
        if (maxDurability > 0) {
            emc = (int) ((double) emc * ((maxDurability - currentDurability) / maxDurability));
        }

        // Multiply by amount
        return (int) (emc * amount);
    }

    /**
     * Gets the EMC value of this item, taking in factors such as:
     * <ul>
     *     <li>The item's durability</li>
     *     <li>The item's enchantments (Paper only)</li>
     *     <li>Amount of items</li>
     * </ul>
     *
     * @return This item stack's EMC value
     */
    public long getEMC() {
        return getEMC(item.getAmount());
    }
}
