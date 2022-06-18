package dan200.computercraft.fabric.poly.gui;

import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.fabric.poly.gui.ComputerGui;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class TurtleInventoryGui extends SimpleGui {

    private final ComputerDisplayAccess access;

    public TurtleInventoryGui(ServerPlayer player, TileTurtle turtle) {
        super(MenuType.GENERIC_9x2, player, false);
        var empty = new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE).setName(Component.empty());
        this.setTitle(turtle.getDisplayName());
        this.setSlot(9, empty);
        for (int i = 0 ; i < turtle.getContainerSize(); i++) {
            this.addSlotRedirect(new Slot(turtle.getAccess().getInventory(), i, 0, 0));
        }

        this.addSlot(empty);

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
