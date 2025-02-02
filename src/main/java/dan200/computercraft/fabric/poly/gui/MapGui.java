package dan200.computercraft.fabric.poly.gui;

import com.google.common.base.Predicates;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dan200.computercraft.fabric.poly.render.CanvasRenderer;
import dan200.computercraft.fabric.poly.render.ImageButton;
import dan200.computercraft.fabric.poly.render.ScreenElement;
import dan200.computercraft.fabric.poly.textures.GuiTextures;
import eu.pb4.mapcanvas.api.core.*;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.sgui.api.gui.HotbarGui;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MapGui extends HotbarGui {
    private static final Packet<?> COMMAND_PACKET;

    public final Entity entity;
    public final CombinedPlayerCanvas canvas;
    public final VirtualDisplay virtualDisplay;
    public final CanvasRenderer renderer;
    public final BlockPos pos;
    public final CanvasIcon cursor;

    public final IntList additionalEntities = new IntArrayList();

    public float xRot;
    public float yRot;
    public int cursorX;
    public int cursorY;
    public int mouseMoves;

    public MapGui(ServerPlayer player) {
        super(player);
        var pos = player.blockPosition().atY(2048);
        this.pos = pos;
        var dir = Direction.NORTH;
        this.canvas = DrawableCanvas.create(5, 3);
        this.virtualDisplay = VirtualDisplay.of(this.canvas, pos.relative(dir).relative(dir.getClockWise(), 2).above(), dir, 0, true);
        this.renderer = CanvasRenderer.of(new CanvasImage(this.canvas.getWidth(), this.canvas.getHeight()));
        this.renderer.add(new ImageButton(560, 32, GuiTextures.CLOSE_ICON, (a, b, c) -> this.close()));

        this.canvas.addPlayer(player);
        this.virtualDisplay.addPlayer(player);

        this.entity = new Horse(EntityType.HORSE, PolymerUtils.getFakeWorld());
        this.entity.setPos(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() - 1.8);
        this.entity.setNoGravity(true);
        this.entity.setYHeadRot(dir.getOpposite().toYRot());
        this.entity.setInvisible(true);

        this.cursorX = this.canvas.getWidth();
        this.cursorY = this.canvas.getHeight(); // MapDecoration.Type.TARGET_POINT
        this.cursor = this.canvas.createIcon(MapDecoration.Type.TARGET_POINT, true, this.cursorX, this.cursorY, (byte) 14, null);
        player.connection.send(this.entity.getAddEntityPacket());

        player.connection.send(new ClientboundSetEntityDataPacket(this.entity.getId(), this.entity.getEntityData().getNonDefaultValues()));
        player.connection.send(new ClientboundSetCameraPacket(this.entity));
        this.xRot = player.getXRot();
        this.yRot = player.getYRot();
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.entity.getId());
        buf.writeVarIntArray(new int[]{player.getId()});
        player.connection.send(new ClientboundSetPassengersPacket(buf));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.SPECTATOR.getId()));
        player.connection.send(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) 0, (byte) 0, player.isOnGround()));

        player.connection.send(COMMAND_PACKET);

        for (int i = 0; i < 9; i++) {
            this.setSlot(i, new ItemStack(Items.STICK));
        }

        player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("polyport.cc.press_to_close", "Ctrl", "Q (Drop)"/*new KeybindComponent("key.drop")*/).withStyle(ChatFormatting.DARK_RED)));
    }

    public void render() {
        this.renderer.render(this.player.level.getGameTime(), this.cursorX / 2, this.cursorY / 2);
        // Debug maps
        if (false && FabricLoader.getInstance().isDevelopmentEnvironment()) {
            for (int x = 0; x < this.canvas.getSectionsWidth(); x++) {
                CanvasUtils.fill(this.renderer.canvas(), x * 128, 0, x * 128 + 1, this.canvas.getHeight(), CanvasColor.RED_HIGH);
            }
            for (int x = 0; x < this.canvas.getSectionsHeight(); x++) {
                CanvasUtils.fill(this.renderer.canvas(), 0, x * 128, this.canvas.getWidth(), x * 128 + 1, CanvasColor.BLUE_HIGH);
            }
        }

        CanvasUtils.draw(this.canvas, 0, 0, this.renderer.canvas());
        this.canvas.sendUpdates();
    }

    @Override
    public void onTick() {
        this.render();
    }

    @Override
    public void onClose() {
        this.cursor.remove();
        this.virtualDisplay.removePlayer(this.player);
        this.virtualDisplay.destroy();
        //this.virtualDisplay2.destroy();
        this.canvas.removePlayer(this.player);
        this.canvas.destroy();
        this.player.server.getCommands().sendCommands(this.player);
        this.player.connection.send(new ClientboundSetCameraPacket(this.player));
        this.player.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
        if (!this.additionalEntities.isEmpty()) {
            this.player.connection.send(new ClientboundRemoveEntitiesPacket(this.additionalEntities));
        }
        this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, this.player.gameMode.getGameModeForPlayer().getId()));
        this.player.connection.send(new ClientboundPlayerPositionPacket(player.getX(), player.getY(), player.getZ(), player.getXRot(), player.getYRot(), EnumSet.noneOf(RelativeMovement.class), 0));
        super.onClose();
    }

    public void onChatInput(String message) {

    }

    public void onCommandInput(String command) {

    }

    public void onCommandSuggestion(int id, String fullCommand) {

    }

    public void onCameraMove(float xRot, float yRot) {
        this.mouseMoves++;
        if (this.mouseMoves < 2) {
            return;
        }

        this.xRot = xRot;
        this.yRot = yRot;

        this.cursorX = this.cursorX + (int) ((xRot > 0.3 ? 6 : xRot < -0.3 ? -6 : 0) * (Math.abs(xRot) - 0.3));
        this.cursorY = this.cursorY + (int) ((yRot > 0.3 ? 6 : yRot < -0.3 ? -6 : 0) * (Math.abs(yRot) - 0.3));

        this.cursorX = Mth.clamp(this.cursorX, 5, this.canvas.getWidth() * 2 - 5);
        this.cursorY = Mth.clamp(this.cursorY, 5, this.canvas.getHeight() * 2 - 5);

        this.cursor.move(this.cursorX + 4, this.cursorY + 4, this.cursor.getRotation());
    }

    @Override
    public boolean onClickEntity(int entityId, EntityInteraction type, boolean isSneaking, @Nullable Vec3 interactionPos) {
        if (type == EntityInteraction.ATTACK) {
            this.renderer.click(this.cursorX / 2, this.cursorY / 2, ScreenElement.ClickType.LEFT_DOWN);
        } else {
            this.renderer.click(this.cursorX / 2, this.cursorY / 2, ScreenElement.ClickType.RIGHT_DOWN);
        }

        return super.onClickEntity(entityId, type, isSneaking, interactionPos);
    }

    public void setDistance(double i) {
        this.entity.setPos(this.entity.getX(), this.entity.getY(), this.pos.getZ() - 0.8 - i);
        this.player.connection.send(new ClientboundTeleportEntityPacket(this.entity));
    }

    public void onPlayerAction(ServerboundPlayerActionPacket.Action action, Direction direction, BlockPos pos) {
        if (action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            this.close();
        }
    }

    // deltaX/Z is currently useless while in camera mode, as it is always 0
    public void onPlayerInput(float deltaX, float deltaZ, boolean jumping, boolean shiftKeyDown) {

    }

    public void onPlayerCommand(int id, ServerboundPlayerCommandPacket.Action action, int data) {
    }

    static {
        var commandNode = new RootCommandNode<SharedSuggestionProvider>();

        commandNode.addChild(
            new ArgumentCommandNode<>(
                "command",
                StringArgumentType.greedyString(),
                null,
                Predicates.alwaysTrue(),
                null,
                null,
                true,
                (ctx, builder) -> null
            )
        );

        COMMAND_PACKET = new ClientboundCommandsPacket(commandNode);
    }


}
