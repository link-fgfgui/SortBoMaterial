package io.github.linkfgfgui.showingredientsinrecipetree.mixin;

import java.util.Comparator;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import io.github.linkfgfgui.showingredientsinrecipetree.bom.BoMRequirement;
import io.github.linkfgfgui.showingredientsinrecipetree.bom.BoMRequirement.Status;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.world.item.Item;

/**
 * Reorders the Arcane Transmutation Tablet's item list so items required by
 * the currently active EMI recipe tree (BoM) come first — and among those,
 * items whose required quantity is not yet satisfied by the player's
 * inventory come very first.
 *
 * <p>ProjectE's {@code updateClientTargets} builds the displayed list by
 * taking {@code provider.getKnowledge()}, filtering, then sorting with
 * {@code Collections.reverseOrder(Comparator.comparing(...))} (EMC
 * descending). We {@code @ModifyArg} that {@code sorted()} call to swap in
 * a comparator that prefixes unmet BoM requirements.</p>
 *
 * <p>Because we sort the full knowledge list (not just the visible output
 * slots), paging naturally follows: the first 12 unmet matter items land
 * on page 1, the next 12 on page 2, etc. Once the player's inventory
 * satisfies an item, it drops out of the priority bucket and falls back to
 * EMC-descending order — i.e. it is no longer pinned to the front.</p>
 */
@Mixin(TransmutationInventory.class)
public abstract class TransmutationInventoryMixin {

    /**
     * Replaces the comparator passed to {@code Stream.sorted(...)} inside
     * {@code updateClientTargets()}.
     *
     * <p>Returned comparator orders elements by:</p>
     * <ol>
     *   <li>BoM-required AND unmet (inventory insufficient) — priority 0,</li>
     *   <li>everything else (BoM-met, non-BoM) — priority 1,</li>
     * </ol>
     * <p>with EMC descending as the tiebreaker inside each bucket,
     * preserving ProjectE's default visual order when no BoM is active.</p>
     */
    // remap = false: TransmutationInventory is a mod class, its method names
    // are not in Forge's searge mappings and are identical in dev and prod.
    @ModifyArg(
            method = "updateClientTargets()V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;",
                    remap = false),
            index = 0)
    private Comparator<ItemInfo> showingredientsinrecipetree$sortForBom(
            Comparator<ItemInfo> original
    ) {
        Map<Item, Status> bom = BoMRequirement.collect();
        if (bom == null || bom.isEmpty()) {
            // No active EMI recipe tree — keep ProjectE's EMC-descending
            // order.
            return original;
        }
        return (a, b) -> {
            int pa = priorityOf(a, bom);
            int pb = priorityOf(b, bom);
            if (pa != pb) {
                return Integer.compare(pa, pb);
            }
            // Tiebreaker: EMC descending, matching ProjectE's default.
            return Long.compare(
                    EMCHelper.getEmcValue(b),
                    EMCHelper.getEmcValue(a)
            );
        };
    }

    /**
     * Returns the BoM priority bucket for an item: {@code 0} when the item
     * is required by the active BoM and the player's inventory does not yet
     * have enough, {@code 1} otherwise (met or non-BoM).
     */
    private static int priorityOf(ItemInfo info, Map<Item, Status> bom) {
        Item item = info.getItem();
        Status status = bom.get(item);
        if (status == null) {
            return 1;
        }
        // 0 = unmet (front), 1 = met or non-BoM (back).
        return status.unmet() ? 0 : 1;
    }
}
