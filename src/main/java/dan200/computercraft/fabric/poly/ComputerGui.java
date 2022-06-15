package dan200.computercraft.fabric.poly;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.fabric.poly.render.*;
import dan200.computercraft.fabric.poly.textures.GuiTextures;
import dan200.computercraft.fabric.poly.textures.RepeatingCanvas;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ComputerGui extends MapGui implements IContainerComputer {

    private static final ClientboundPlayerInfoPacket ADDITIONAL_SUGGESTIONS_PACKET;
    private static final ClientboundPlayerInfoPacket ADDITIONAL_SUGGESTIONS_REMOVE_PACKET;

    private static final Map<String, BiConsumer<ComputerGui, String>> ACTIONS = new HashMap<>();

    static {

        for (int i = 0; i < 12; i++) {
            ACTIONS.put("f" + (i + 1), pressKey(Keys.F1 + i));
        }
        ACTIONS.put("enter", pressKey(Keys.ENTER));
        ACTIONS.put("backspace", pressKey(Keys.BACKSPACE));
        ACTIONS.put("back", pressKey(Keys.BACKSPACE));
        ACTIONS.put("esc", pressKey(Keys.ESCAPE));
        ACTIONS.put("ctrl", pressKey(Keys.LEFT_CONTROL));
        ACTIONS.put("shift", pressKey(Keys.LEFT_SHIFT));
        ACTIONS.put("shift_hold", holdKey(Keys.LEFT_SHIFT));
        ACTIONS.put("tab", pressKey(Keys.TAB));
        ACTIONS.put("up", pressKey(Keys.UP));
        ACTIONS.put("down", pressKey(Keys.DOWN));
        ACTIONS.put("left", pressKey(Keys.LEFT));
        ACTIONS.put("right", pressKey(Keys.RIGHT));
        ACTIONS.put("close", (gui, arg) -> gui.close());
        ACTIONS.put("moveview", (gui, arg) -> {
            try {
                double i = Math.min(Math.max(Double.parseDouble(arg), 1), 8);
                gui.setDistance(i);
            } catch (Exception e) {
                gui.player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
            }
        });


        ADDITIONAL_SUGGESTIONS_PACKET = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER);
        ADDITIONAL_SUGGESTIONS_REMOVE_PACKET = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER);

        int i = 0;
        for (var s : ACTIONS.keySet()) {
            var entry = new ClientboundPlayerInfoPacket.PlayerUpdate(
                new GameProfile(new UUID(0x54345345634l, i++), ";" + s), 999, GameType.SPECTATOR,
                Component.literal(";" + s).withStyle(ChatFormatting.DARK_RED), null
            );
            ADDITIONAL_SUGGESTIONS_PACKET.getEntries().add(entry);
            ADDITIONAL_SUGGESTIONS_REMOVE_PACKET.getEntries().add(entry);
        }
    }

    public final ComputerDisplayAccess computer;
    public final ImageButton closeButton;
    public final ImageButton terminateButton;
    public final InputState input = new InputState(this);
    public final KeyboardView keyboard;
    public String currentInput = "";
    public IntSet keysToReleaseNextTick = new IntArraySet();

    public ComputerGui(ServerPlayer player, ComputerDisplayAccess computer) {
        super(player);
        this.computer = computer;

        {
            var terminal = this.computer.getComputer().getTerminal();
            int centerX = canvas.getWidth() / 2;
            int centerY = canvas.getHeight() / 2 - 48;

            boolean turtle = computer.getBlockEntity() instanceof TileTurtle;

            int termX = centerX - terminal.getRenderedWidth() / 2;
            int termY = centerY - terminal.getRenderedHeight() / 2;

            if (turtle) {
                termX -= 36;
            }

            var terminalView = new TerminalView(
                termX, termY,
                terminal,
                this.input
            );

            terminalView.zIndex = 2;

            this.renderer.add(terminalView);

            var compText = switch (this.computer.getComputer().getFamily()) {
                case NORMAL -> GuiTextures.COMPUTER;
                case ADVANCED -> GuiTextures.ADVANCED_COMPUTER;
                case COMMAND -> GuiTextures.COMMAND_COMPUTER;
            };

            if (turtle) {
                var xi = termX + terminal.getRenderedWidth() + 32;
                var yi = termY - 28;
                var inv = new TurtleInventoryView(xi, yi, this);
                this.renderer.add(inv);

                this.renderer.add(new ImageView(
                        xi, yi - compText.top().getHeight(),
                        new RepeatingCanvas(compText.top(), inv.width(), compText.top().getHeight())
                    )
                );

                this.renderer.add(new ImageView(
                        xi, yi + inv.height(),
                        new RepeatingCanvas(compText.bottomSmall(), inv.width(), compText.bottomSmall().getHeight())
                    )
                );

                this.renderer.add(new ImageView(
                        xi - compText.leftSide().getWidth(), yi,
                        new RepeatingCanvas(compText.leftSide(), compText.leftSide().getWidth(), inv.height())
                    )
                );

                this.renderer.add(new ImageView(
                        xi + inv.width(), yi,
                        new RepeatingCanvas(compText.rightSide(), compText.rightSide().getWidth(), inv.height())
                    )
                );

                this.renderer.add(new ImageView(xi - compText.leftTop().getWidth(), yi - compText.leftTop().getHeight(), compText.leftTop()));
                this.renderer.add(new ImageView(xi + inv.width(), yi - compText.rightTop().getHeight(), compText.rightTop()));

                this.renderer.add(new ImageView(xi - compText.smallLeftBottom().getWidth(), yi + inv.height(), compText.smallLeftBottom()));
                this.renderer.add(new ImageView(xi + inv.width(), yi + inv.height(), compText.smallRightBottom()));
            }

            {
                int sideX = termX - compText.sideButtonPlateSide().getWidth() - compText.leftSide().getWidth() + 3;

                var sideTop = new ImageView(sideX - 3, termY + 8, compText.sideButtonPlateTop());
                sideTop.zIndex = -1;
                this.renderer.add(sideTop);

                int sideY = termY + 8 + compText.sideButtonPlateTop().getHeight();

                int size = 0;

                {
                    this.closeButton = new ImageButton(sideX, sideY + size, GuiTextures.CLOSE_ICON, (x, y, t) -> {
                        if (this.getComputer().isOn()) {
                            this.getComputer().shutdown();
                        } else {
                            this.getComputer().turnOn();
                        }
                    });

                    this.closeButton.zIndex = 2;
                    this.renderer.add(this.closeButton);
                    size += this.closeButton.height() + 2;


                    this.terminateButton = new ImageButton(sideX, sideY + size, GuiTextures.TERMINATE, (x, y, t) -> {
                        this.getComputer().queueEvent("terminate");
                    });

                    this.terminateButton.zIndex = 2;
                    this.renderer.add(this.terminateButton);
                    size += this.terminateButton.height() + 2;
                }

                size -= 2;

                var side = new ImageView(
                    sideX - 3, sideY,
                    new RepeatingCanvas(compText.sideButtonPlateSide(), compText.sideButtonPlateSide().getWidth(), size)
                );
                side.zIndex = -1;
                this.renderer.add(side);

                var sideBottom = new ImageView(sideX - 3, sideY + size, compText.sideButtonPlateBottom());
                sideBottom.zIndex = -1;
                this.renderer.add(sideBottom);
            }

            this.renderer.add(new ImageView(
                    termX, termY - compText.top().getHeight(),
                    new RepeatingCanvas(compText.top(), terminal.getRenderedWidth(), compText.top().getHeight())
                )
            );

            this.renderer.add(new ImageView(
                    termX, termY + terminal.getRenderedHeight(),
                    new RepeatingCanvas(compText.bottomSmall(), terminal.getRenderedWidth(), compText.bottomSmall().getHeight())
                )
            );

            this.renderer.add(new ImageView(
                    termX - compText.leftSide().getWidth(), termY,
                    new RepeatingCanvas(compText.leftSide(), compText.leftSide().getWidth(), terminal.getRenderedHeight())
                )
            );

            this.renderer.add(new ImageView(
                    termX + terminal.getRenderedWidth(), termY,
                    new RepeatingCanvas(compText.rightSide(), compText.rightSide().getWidth(), terminal.getRenderedHeight())
                )
            );

            this.renderer.add(new ImageView(termX - compText.leftTop().getWidth(), termY - compText.leftTop().getHeight(), compText.leftTop()));
            this.renderer.add(new ImageView(termX + terminal.getRenderedWidth(), termY - compText.rightTop().getHeight(), compText.rightTop()));

            this.renderer.add(new ImageView(termX - compText.smallLeftBottom().getWidth(), termY + terminal.getRenderedHeight(), compText.smallLeftBottom()));
            this.renderer.add(new ImageView(termX + terminal.getRenderedWidth(), termY + terminal.getRenderedHeight(), compText.smallRightBottom()));

            this.renderer.add(terminalView);

            this.keyboard = new KeyboardView(centerX - (KeyboardView.KEYBOARD_WIDTH / 2), terminalView.y + terminalView.height() + 16, this);
            this.renderer.add(this.keyboard);
        }

        this.render();


        player.connection.send(ADDITIONAL_SUGGESTIONS_PACKET);

        for (int i = 0; i < 9; i++) {
            this.setSlot(i, new ItemStack(Items.STICK));
        }

        this.open();
    }

    public static void open(ServerPlayer player, ComputerDisplayAccess computer) {
        if (player.isOnGround()) {
            new ComputerGui(player, computer);
        }
    }

    private static BiConsumer<ComputerGui, String> pressKey(int key) {
        return (gui, arg) -> {
            int i;
            try {
                i = Integer.parseInt(arg);
            } catch (Exception e) {
                i = 1;
            }

            for (int a = 0; a < i; a++) {
                gui.input.keyDown(key, false);
            }
            gui.keysToReleaseNextTick.add(key);
        };
    }

    private static BiConsumer<ComputerGui, String> holdKey(int key) {
        return (gui, arg) -> {
            if (!gui.input.isKeyDown(key)) {
                gui.input.keyDown(key, true);
            } else {
                gui.input.keyUp(key);
            }
        };
    }

    public void render() {

        {
            boolean isIn = this.closeButton.isIn(this.cursorX / 2, this.cursorY / 2);
            if (this.computer.getComputer().isOn()) {
                this.closeButton.image = isIn ? GuiTextures.CLOSE_ICON_ACTIVE_HOVER : GuiTextures.CLOSE_ICON_ACTIVE;
            } else {
                this.closeButton.image = isIn ? GuiTextures.CLOSE_ICON_HOVER : GuiTextures.CLOSE_ICON;
            }
        }
        {
            boolean isIn = this.terminateButton.isIn(this.cursorX / 2, this.cursorY / 2);
            this.terminateButton.image = isIn ? GuiTextures.TERMINATE_HOVER : GuiTextures.TERMINATE;
        }

        super.render();
    }

    @Override
    public void onTick() {
        if (this.computer.canStayOpen(this.player)) {
            this.render();
        } else {
            this.close();
        }

        for (var key : this.keysToReleaseNextTick) {
            this.input.keyUp(key);
        }
        this.keysToReleaseNextTick.clear();
    }

    @Override
    public void onClose() {
        this.player.connection.send(ADDITIONAL_SUGGESTIONS_REMOVE_PACKET);

        super.onClose();
    }

    public ComputerDisplayAccess getAccess() {
        return this.computer;
    }

    public void onChatInput(String message) {
        if (message.startsWith(";")) {
            for (var line : message.substring(1).split(";")) {
                var args = line.split(" ", 2);

                var action = ACTIONS.get(args[0]);
                if (action != null) {
                    action.accept(this, args.length != 2 ? "" : args[1]);
                }
            }
        } else {
            if (!message.startsWith("/")) {
                for (var character : message.codePoints().toArray()) {
                    if (character >= 32 && character <= 126 || character >= 160 && character <= 255) {
                        this.input.queueEvent("char", new Object[]{Character.toString(character)});
                    }
                }

            }

            this.input.keyDown(Keys.ENTER, false);
            this.keysToReleaseNextTick.add(Keys.ENTER);
            this.currentInput = "";
        }
    }

    public void onCommandInput(String command) {
        this.input.keyDown(Keys.ENTER, false);
        this.keysToReleaseNextTick.add(Keys.ENTER);
        this.currentInput = "";
    }

    public void onCommandSuggestion(int id, String fullCommand) {
        var old = this.currentInput;
        var commandBuilder = new StringBuilder();

        for (var character : fullCommand.substring(1).codePoints().toArray()) {
            if (character >= 32 && character <= 126 || character >= 160 && character <= 255) {
                commandBuilder.append(Character.toChars(character));
            }
        }
        var command = commandBuilder.toString();

        if (!old.equals(command)) {
            int i;
            for (i = 0; i < old.length(); i++) {
                if (command.length() <= i || command.charAt(i) != old.charAt(i)) {
                    break;
                }
            }

            for (var tmp = i; tmp < old.length(); tmp++) {
                this.input.queueEvent("key", new Object[]{Keys.BACKSPACE, false});
            }

            for (; i < command.length(); i++) {
                this.input.queueEvent("char", new Object[]{Character.toString(command.charAt(i))});
            }

            this.currentInput = command;
        }
    }

    public void onCameraMove(float xRot, float yRot) {
        this.mouseMoves++;

        if (this.mouseMoves < 16) {
            return;
        }

        this.xRot = xRot;
        this.yRot = yRot;

        this.cursorX = this.cursorX + (int) ((xRot > 0.3 ? 3 : xRot < -0.3 ? -3 : 0) * (Math.abs(xRot) - 0.3));
        this.cursorY = this.cursorY + (int) ((yRot > 0.3 ? 3 : yRot < -0.3 ? -3 : 0) * (Math.abs(yRot) - 0.3));

        this.cursorX = Mth.clamp(this.cursorX, 5, this.canvas.getWidth() * 2 - 5);
        this.cursorY = Mth.clamp(this.cursorY, 5, this.canvas.getHeight() * 2 - 5);

        this.cursor.move(this.cursorX + 4, this.cursorY + 4, this.cursor.getRotation());
    }

    @Override
    public boolean onClickEntity(int entityId, EntityInteraction type, boolean isSneaking, @Nullable Vec3 interactionPos) {
        //this.player.sendMessage(new TextComponent("x: " + this.cursorX / 2 + " | y: " + this.cursorY / 2), Util.NIL_UUID);
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

    @Nullable
    @Override
    public IComputer getComputer() {
        return this.computer.getComputer();
    }

    @NotNull
    @Override
    public InputState getInput() {
        return this.input;
    }

    @Override
    public void startUpload(@NotNull UUID uploadId, @NotNull List<FileUpload> files) {

    }

    @Override
    public void continueUpload(@NotNull UUID uploadId, @NotNull List<FileSlice> slices) {

    }

    @Override
    public void finishUpload(@NotNull ServerPlayer uploader, @NotNull UUID uploadId) {

    }

    @Override
    public void confirmUpload(@NotNull ServerPlayer uploader, boolean overwrite) {

    }

    public void onPlayerAction(ServerboundPlayerActionPacket.Action action, Direction direction, BlockPos pos) {
        if (action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            this.close();
        }
    }
}
