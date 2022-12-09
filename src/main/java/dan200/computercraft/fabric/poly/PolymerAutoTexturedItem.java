package dan200.computercraft.fabric.poly;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface PolymerAutoTexturedItem extends PolymerItem {

    @Override
    default int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayer player) {
        return PolymerSetup.MODELS.get(this).value();
    }
}
