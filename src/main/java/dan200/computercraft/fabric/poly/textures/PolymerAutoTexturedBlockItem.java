package dan200.computercraft.fabric.poly.textures;

import dan200.computercraft.fabric.poly.PolymerAutoTexturedItem;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PolymerAutoTexturedBlockItem extends PolymerBlockItem implements PolymerAutoTexturedItem {
    public PolymerAutoTexturedBlockItem(Block block, Properties settings, Item virtualItem) {
        super(block, settings, virtualItem);
    }
}
