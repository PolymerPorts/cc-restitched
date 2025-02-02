package dan200.computercraft.fabric.poly.gui;

import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.fabric.poly.Keys;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class ComputerGui extends MapGui implements IContainerComputer {

    private static final Packet<ClientGamePacketListener> ADDITIONAL_SUGGESTIONS_PACKET;
    private static final Packet<ClientGamePacketListener> ADDITIONAL_SUGGESTIONS_REMOVE_PACKET;

    private static final Map<String, BiConsumer<ComputerGui, String>> ACTIONS = new HashMap<>();

    static {

        for (int i = 0; i < 12; i++) {
            ACTIONS.put("f" + (i + 1), pressKey(Keys.F1 + i));
        }
        ACTIONS.put("enter", pressKey(Keys.ENTER));
        ACTIONS.put("backspace", pressKey(Keys.BACKSPACE));
        ACTIONS.put("bsp", pressKey(Keys.BACKSPACE));
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
        ACTIONS.put("exit", (gui, arg) -> gui.close());
        ACTIONS.put("quit", (gui, arg) -> gui.close());

        ACTIONS.put("press", (gui, arg) -> {
            if (arg != null && !arg.isEmpty()) {
                char character = arg.charAt(0);

                var args = arg.length() == 1 ? new String[]{arg} : arg.split(" ", 2);

                try {
                    if (args[0].length() > 1) {
                        character = (char) Integer.parseInt(arg);
                    }
                } catch (Throwable e) {

                }

                int count = 1;

                try {
                    count = Math.min(Integer.parseInt(args[1]), 255);
                } catch (Throwable t) {

                }

                for (int i = 0; i < count; i++) {
                    gui.pressButton(character);
                }
            }
        });

        ACTIONS.put("moveview", (gui, arg) -> {
            try {
                double i = Math.min(Math.max(Double.parseDouble(arg), 1), 8);
                gui.setDistance(i);
            } catch (Exception e) {
                gui.player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
            }
        });


        var list = ACTIONS.keySet().stream().map(x -> ";" + x).collect(Collectors.toList());

        ADDITIONAL_SUGGESTIONS_PACKET = new ClientboundCustomChatCompletionsPacket(
            ClientboundCustomChatCompletionsPacket.Action.ADD, list
        );
        ADDITIONAL_SUGGESTIONS_REMOVE_PACKET = new ClientboundCustomChatCompletionsPacket(
            ClientboundCustomChatCompletionsPacket.Action.REMOVE, list
        );
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
                    this.closeButton = new ImageButton(sideX, sideY + size, GuiTextures.SHUTDOWN_ICON, (x, y, t) -> {
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
        if (this.computer.getComputer().isOn()) {
            this.closeButton.texture = GuiTextures.SHUTDOWN_ACTIVE;
        } else {
            this.closeButton.texture = GuiTextures.SHUTDOWN_ICON;
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
                if (!this.keysToReleaseNextTick.contains(Keys.BACKSPACE) && !this.input.isKeyDown(Keys.BACKSPACE)) {
                    this.input.keyDown(Keys.BACKSPACE, false);
                    this.keysToReleaseNextTick.add(Keys.BACKSPACE);
                } else {
                    this.input.queueEvent("key", new Object[]{Keys.BACKSPACE, false});
                }
            }

            for (; i < command.length(); i++) {
                pressButton(command.charAt(i));
            }

            this.currentInput = command;
        }
    }

    public void pressButton(char character) {
        if (character >= 32 && character <= 126 || character >= 160 && character <= 255) {
            var key = KeyboardView.CHAR_TO_KEY.get(character);
            if (key != null) {
                this.input.keyDown(key.key(), false);
                this.keysToReleaseNextTick.add(key.key());
            }

            if (key.upperCase() == character && key.lowerCase() != character) {
                if (!this.keysToReleaseNextTick.contains(Keys.LEFT_SHIFT) && !this.input.isKeyDown(Keys.LEFT_SHIFT)) {
                    this.input.keyDown(Keys.LEFT_SHIFT, false);

                    this.keysToReleaseNextTick.add(Keys.LEFT_SHIFT);
                }
            }

            this.input.queueEvent("char", new Object[]{Character.toString(character)});
        }
    }

    @Override
    public boolean onClickEntity(int entityId, EntityInteraction type, boolean isSneaking, @Nullable Vec3 interactionPos) {
        return super.onClickEntity(entityId, type, isSneaking, interactionPos);
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
