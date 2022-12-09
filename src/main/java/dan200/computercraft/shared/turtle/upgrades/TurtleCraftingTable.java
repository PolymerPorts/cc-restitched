/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TurtleCraftingTable extends AbstractTurtleUpgrade
{
    @Environment( EnvType.CLIENT )
    private static class Models
    {
        private static final ModelResourceLocation leftModel = new ModelResourceLocation( "computercraft", "turtle_crafting_table_left", "inventory" );
        private static final ModelResourceLocation rightModel = new ModelResourceLocation( "computercraft", "turtle_crafting_table_right", "inventory" );
    }

    public TurtleCraftingTable( ResourceLocation id, ItemStack stack )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, "upgrade.minecraft.crafting_table.adjective", stack );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new CraftingTablePeripheral( turtle );
    }
}
