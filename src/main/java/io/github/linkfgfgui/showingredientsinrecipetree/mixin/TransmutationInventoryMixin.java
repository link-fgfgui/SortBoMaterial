package io.github.linkfgfgui.showingredientsinrecipetree.mixin;

import java.util.Comparator;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import io.github.linkfgfgui.showingredientsinrecipetree.bom.BoMRequirement;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.world.item.Item;

/**
 * Reorders the Arcane Transmutation Tablet's item list so items required by
 * the currently active EMI recipe tree (BoM) come first — and among those,
 * items whose required quantity is not yet satisfied by the player's
 * inventory come very first.
 *
 * <p>ProjectE's {@code updateClientTargets} builds the displayed list by
 * taking {@code provider.getKnowledge()}, filtering, then sorting with
 * {@code Comparator.comparingLong(EmcData::emc).reversed()} (EMC descending).
 * We {@code @ModifyArg} that {@code sorted()} call to swap in a comparator
 * that prefixes unmet BoM requirements.</p>
 *
 * <p>Because we sort the full knowledge list (not just the 16 visible
 * slots), paging naturally follows: the first 12 unmet matter items land
 * on page 1, the next 12 on page 2, etc. Once the player's inventory
 * satisfies an item, it drops out of the priority bucket and falls back to
 * EMC-descending order — i.e. it is no longer pinned to the front.</p>
 */
@Mixin(TransmutationInventory.class)
public abstract class TransmutationInventoryMixin {

    /**
     * Replaces the comparator passed to {@code Stream.sorted(...)} inside
     * {@code updateClientTargets(long)}.
     *
     * <p>Returned comparator orders elements by:</p>
     * <ol>
     *   <li>BoM-required AND unmet (inventory insufficient) — priority 0,</li>
     *   <li>everything else (BoM-met, non-BoM) — priority 1,</li>
     * </ol>
     * <p>with EMC descending as the tiebreaker inside each bucket, preserving
     * ProjectE's default visual order when no BoM is active.</p>
     */
    @ModifyArg(
            method = "updateClientTargets(J)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0)
    private Comparator<Object> showingredientsinrecipetree$sortForBom(Comparator<?> original) {
        Map<Item, BoMRequirement.Status> bom = BoMRequirement.collect();
        if (bom == null || bom.isEmpty()) {
            // No active EMI recipe tree — keep ProjectE's EMC-descending order.
            return cast(original);
        }
        return (a, b) -> {
            int pa = priorityOf(a, bom);
            int pb = priorityOf(b, bom);
            if (pa != pb) {
                return Integer.compare(pa, pb);
            }
            // Tiebreaker: EMC descending, matching ProjectE's default.
            return Long.compare(emcOf(b), emcOf(a));
        };
    }

    @SuppressWarnings("unchecked")
    private static Comparator<Object> cast(Comparator<?> c) {
        return (Comparator<Object>) c;
    }

    /**
     * Reads the {@code emc()} accessor of ProjectE's local {@code EmcData}
     * record via reflection. The record is private to the method, so we
     * cannot reference its type at compile time.
     */
    private static long emcOf(Object emcData) {
        try {
            return (Long) emcData.getClass().getMethod("emc").invoke(emcData);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return 0L;
        }
    }

    private static int priorityOf(Object emcData, Map<Item, BoMRequirement.Status> bom) {
        Item item = itemOf(emcData);
        if (item == null) {
            return 1;
        }
        BoMRequirement.Status status = bom.get(item);
        if (status == null) {
            return 1;
        }
        // 0 = unmet (front), 1 = met or non-BoM (back).
        return status.unmet() ? 0 : 1;
    }

    private static Item itemOf(Object emcData) {
        try {
            Object info = emcData.getClass().getMethod("info").invoke(emcData);
            if (info instanceof ItemInfo i) {
                return i.getItem().value();
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Fall through to return null.
        }
        return null;
    }
}
