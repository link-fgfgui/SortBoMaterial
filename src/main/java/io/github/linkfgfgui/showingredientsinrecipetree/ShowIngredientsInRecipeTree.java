package io.github.linkfgfgui.showingredientsinrecipetree;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Client-only mod that reorders Project Expansion's Arcane Transmutation Tablet
 * output slots so items required by the currently active EMI recipe tree
 * (BoM) come first — and among those, items whose required quantity is not
 * yet satisfied by the player's inventory come very first.
 *
 * <p>All behavior is implemented in {@link
 * io.github.linkfgfgui.showingredientsinrecipetree.mixin.TransmutationInventoryMixin}
 * and {@link io.github.linkfgfgui.showingredientsinrecipetree.bom.BoMRequirement}; this
 * class only exists to register the mod and is gated to {@link Dist#CLIENT}.</p>
 */
@Mod(value = ShowIngredientsInRecipeTree.MODID, dist = Dist.CLIENT)
public class ShowIngredientsInRecipeTree {
    public static final String MODID = "showingredientsinrecipetree";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShowIngredientsInRecipeTree(ModContainer container) {
        // dist=Dist.CLIENT on @Mod already prevents instantiation on the
        // dedicated server; this log line is a no-op confirmation at startup.
        LOGGER.info("{} loaded (client-only)", MODID);
    }
}
