package dan200.computercraft.fabric.mixin.poly;

import dan200.computercraft.fabric.poly.MapGui;
import eu.pb4.sgui.virtual.VirtualScreenHandlerInterface;
import eu.pb4.sgui.virtual.hotbar.HotbarScreenHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Shadow
    public abstract void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> genericFutureListener);

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V", at = @At("HEAD"), cancellable = true)
    private void ccp_onChat(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onChatInput(serverboundChatPacket.getMessage());
            });
            ci.cancel();
        }
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void ccp_onChat(ServerboundChatCommandPacket serverboundChatCommandPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onCommandInput(serverboundChatCommandPacket.command());
            });
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void ccp_onMove(ServerboundMovePlayerPacket serverboundMovePlayerPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.send(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) 0, (byte) 0, player.isOnGround()));
            this.server.execute(() -> {
                var xRot = serverboundMovePlayerPacket.getXRot(computerGui.xRot);
                var yRot = serverboundMovePlayerPacket.getYRot(computerGui.yRot);
                if (xRot != 0 || yRot != 0) {
                    computerGui.onCameraMove(yRot, xRot);
                }
            });
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void ccp_onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onPlayerAction(packet.getAction(), packet.getDirection(), packet.getPos());
            });
            ci.cancel();
        }
    }

    @Inject(method = "handleCustomCommandSuggestions", at = @At("HEAD"), cancellable = true)
    private void ccp_onCustomSuggestion(ServerboundCommandSuggestionPacket serverboundCommandSuggestionPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onCommandSuggestion(serverboundCommandSuggestionPacket.getId(), serverboundCommandSuggestionPacket.getCommand());
            });
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void ccp_onVehicleMove(ServerboundPlayerInputPacket serverboundPlayerInputPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onPlayerInput(serverboundPlayerInputPacket.getXxa(), serverboundPlayerInputPacket.getZza(),  serverboundPlayerInputPacket.isJumping(), serverboundPlayerInputPacket.isShiftKeyDown());
            });
            ci.cancel();
        }
    }
}
