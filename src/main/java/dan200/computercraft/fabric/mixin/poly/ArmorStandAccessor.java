package dan200.computercraft.fabric.mixin.poly;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Invoker
    void callSetSmall(boolean bl);
}
