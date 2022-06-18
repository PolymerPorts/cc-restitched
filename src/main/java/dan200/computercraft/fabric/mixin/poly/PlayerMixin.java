package dan200.computercraft.fabric.mixin.poly;

import dan200.computercraft.fabric.poly.gui.MapGui;
import eu.pb4.sgui.virtual.VirtualScreenHandlerInterface;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Shadow
    public AbstractContainerMenu containerMenu;

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.BEFORE))
    private void ccp_closeOnDamage(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if (f > 0 && this.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            computerGui.close();
        }
    }
}
