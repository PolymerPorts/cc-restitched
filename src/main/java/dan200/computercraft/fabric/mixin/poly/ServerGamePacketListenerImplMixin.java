package dan200.computercraft.fabric.mixin.poly;

import dan200.computercraft.fabric.poly.gui.MapGui;
import eu.pb4.sgui.virtual.VirtualScreenHandlerInterface;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.Set;

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
    public abstract void send(Packet<?> packet, @Nullable PacketSendListener packetSendListener);

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V", at = @At("HEAD"), cancellable = true)
    private void ccp_onChat(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onChatInput(serverboundChatPacket.message());
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
            //this.send(new ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor.EYES, computerGui.entity.getX(),
            //    computerGui.entity.getY() + computerGui.entity.getMyRidingOffset() + player.getEyeHeight(Pose.SITTING),
            //    computerGui.entity.getZ() + 1));
            if (serverboundMovePlayerPacket.getXRot(0) != 0 || serverboundMovePlayerPacket.getYRot(0) != 0) {
                this.send(new ClientboundPlayerPositionPacket(player.getX(), player.getY(), player.getZ(), 0, 0, EnumSet.noneOf(RelativeMovement.class), 0));
            }
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

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void ccp_onVehicleMove(ServerboundPlayerCommandPacket serverboundPlayerCommandPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onPlayerCommand(serverboundPlayerCommandPacket.getId(), serverboundPlayerCommandPacket.getAction(), serverboundPlayerCommandPacket.getData());
            });
            ci.cancel();
        }
    }
}
