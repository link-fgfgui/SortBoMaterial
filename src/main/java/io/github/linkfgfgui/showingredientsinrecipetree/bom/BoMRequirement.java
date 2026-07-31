package io.github.linkfgfgui.showingredientsinrecipetree.bom;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import dev.emi.emi.bom.BoM;
import dev.emi.emi.bom.FlatMaterialCost;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the currently active EMI recipe tree (BoM) and reports each required
 * item's status against the player's inventory.
 *
 * <p>The {@code dev.emi.emi.bom} classes used here are {@code public} at runtime
 * but not part of EMI's published {@code :api} jar — they are visible at compile
 * time because the full EMI jar is on the compile classpath, and provided at
 * runtime by the player's installed EMI.</p>
 */
public final class BoMRequirement {
    private BoMRequirement() {}

    /** Snapshot of one required item: needed vs. available count. */
    public record Status(Item item, long needed, long available) {
        /** True when the player does not yet have enough in inventory. */
        public boolean unmet() {
            return available < needed;
        }
    }

    /**
     * Collects required items from the currently active EMI BoM tree, but
     * only when EMI is in <em>crafting mode</em>. In viewing mode the recipe
     * tree is just being browsed, so we must not pin its materials to the
     * front of the transmutation tablet.
     *
     * @return {@code null} if EMI has no active recipe tree or is in viewing
     *         mode, otherwise a map keyed by {@link Item} with per-item
     *         requirement status.
     */
    @Nullable
    public static Map<Item, Status> collect() {
        MaterialTree tree = BoM.tree;
        if (tree == null || !BoM.craftingMode) {
            return null;
        }
        // Recalculate so cost maps reflect the latest tree state.
        tree.calculateCost();

        Map<Item, Long> needed = new HashMap<>();
        for (FlatMaterialCost cost : tree.cost.costs.values()) {
            addRequirement(needed, cost);
        }
        for (FlatMaterialCost cost : tree.cost.chanceCosts.values()) {
            addRequirement(needed, cost);
        }
        if (needed.isEmpty()) {
            return null;
        }

        Map<Item, Long> available = countInventory(Minecraft.getInstance().player);

        Map<Item, Status> out = new HashMap<>();
        for (Map.Entry<Item, Long> e : needed.entrySet()) {
            Item item = e.getKey();
            long need = e.getValue();
            long have = available.getOrDefault(item, 0L);
            out.put(item, new Status(item, need, have));
        }
        return out;
    }

    private static void addRequirement(Map<Item, Long> out, FlatMaterialCost cost) {
        long amount = cost.getEffectiveAmount();
        if (amount <= 0) {
            return;
        }
        for (EmiStack es : cost.ingredient.getEmiStacks()) {
            ItemStack is = es.getItemStack();
            if (is.isEmpty()) {
                // Skip fluids and non-item stacks.
                continue;
            }
            out.merge(is.getItem(), amount, Long::sum);
        }
    }

    private static Map<Item, Long> countInventory(@Nullable LocalPlayer player) {
        Map<Item, Long> counts = new HashMap<>();
        if (player == null) {
            return counts;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
            }
        }
        ItemStack cursor = player.containerMenu != null
                ? player.containerMenu.getCarried()
                : ItemStack.EMPTY;
        if (!cursor.isEmpty()) {
            counts.merge(cursor.getItem(), (long) cursor.getCount(), Long::sum);
        }
        return counts;
    }
}
