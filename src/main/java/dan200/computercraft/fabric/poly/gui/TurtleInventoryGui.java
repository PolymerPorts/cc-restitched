package dan200.computercraft.fabric.poly.gui;

import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.fabric.poly.PolymerSetup;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import eu.pb4.polymer.api.resourcepack.PolymerRPUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public class TurtleInventoryGui extends SimpleGui {

    private final ComputerDisplayAccess access;

    public TurtleInventoryGui(ServerPlayer player, TileTurtle turtle) {
        super(MenuType.GENERIC_9x2, player, false);
        var pack = PolymerRPUtils.hasPack(player);
        this.setTitle(pack
            ? Component.empty().append(Component.literal("-1.").setStyle(Style.EMPTY.withFont(PolymerSetup.GUI_FONT).withColor(ChatFormatting.WHITE))).append(turtle.getDisplayName())
            : turtle.getDisplayName()
        );
        if (!pack) {
            this.setSlot(9, PolymerSetup.FILLER_ITEM);
            this.setSlot(17, PolymerSetup.FILLER_ITEM);
        }

        for (int i = 0 ; i < 9; i++) {
            this.setSlotRedirect(i, new Slot(turtle.getAccess().getInventory(), i, 0, 0));
        }

        for (int i = 9; i < turtle.getContainerSize(); i++) {
            this.setSlotRedirect(i + 1, new Slot(turtle.getAccess().getInventory(), i, 0, 0));
        }

        this.access = turtle.getDisplayAccess();

        this.open();
    }

    @Override
    public void onTick() {
        if (!this.access.canStayOpen(player)) {
            this.close();
        }
    }

    @Override
    public void onClose() {
        ComputerGui.open(this.player, this.access);
    }
}
