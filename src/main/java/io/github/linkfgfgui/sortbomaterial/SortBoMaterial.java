package io.github.linkfgfgui.sortbomaterial;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;

/**
 * Client-only mod that reorders Project Expansion's Arcane Transmutation Tablet
 * output slots so items required by the currently active EMI recipe tree
 * (BoM) come first — and among those, items whose required quantity is not
 * yet satisfied by the player's inventory come very first.
 *
 * <p>All behavior is implemented in {@link
 * io.github.linkfgfgui.sortbomaterial.mixin.TransmutationInventoryMixin}
 * and {@link io.github.linkfgfgui.sortbomaterial.bom.BoMRequirement};
 * this class only exists to register the mod. The client-only restriction is
 * declared in {@code mods.toml} via {@code clientSideOnly=true}, since Forge
 * 1.20.1's {@code @Mod} annotation has no {@code dist} attribute.</p>
 *
 * <p>Note: Forge 1.20.1's {@code javafml} loader instantiates the {@code @Mod}
 * class via its no-arg constructor (unlike NeoForge, which passes a
 * {@code ModContainer}), so this constructor must take no parameters.</p>
 */
@Mod(SortBoMaterial.MODID)
public class SortBoMaterial {
    public static final String MODID = "sortbomaterial";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SortBoMaterial() {
        // clientSideOnly=true in mods.toml prevents loading on dedicated
        // servers; this log line is a no-op confirmation at startup.
        LOGGER.info("{} loaded (client-only)", MODID);
    }
}
