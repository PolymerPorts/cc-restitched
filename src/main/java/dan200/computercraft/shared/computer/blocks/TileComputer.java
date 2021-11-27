/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileComputer extends TileComputerBase
{
    private ComputerProxy proxy;

    public TileComputer( ComputerFamily family, BlockEntityType<? extends TileComputer> type, BlockPos pos, BlockState state )
    {
        super( type, family, pos, state );
    }

    public boolean isUsableByPlayer( Player player )
    {
        return isUsable( player, false );
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
        BlockState existing = getBlockState();
        if( existing.getValue( BlockComputer.STATE ) != newState )
        {
            getLevel().setBlock( getBlockPos(), existing.setValue( BlockComputer.STATE, newState ), 3 );
        }
    }

    @Override
    public Direction getDirection()
    {
        return getBlockState().getValue( BlockComputer.FACING );
    }

    @Override
    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if( localSide == ComputerSide.RIGHT )
        {
            return ComputerSide.LEFT;
        }
        if( localSide == ComputerSide.LEFT )
        {
            return ComputerSide.RIGHT;
        }
        return localSide;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ComputerFamily family = getFamily();
        ServerComputer computer = new ServerComputer( getLevel(),
            id, label,
            instanceID,
            family,
            ComputerCraft.computerTermWidth,
            ComputerCraft.computerTermHeight );
        computer.setPosition( getBlockPos() );
        return computer;
    }

    @Override
    public ComputerProxy createProxy()
    {
        if( proxy == null )
        {
            proxy = new ComputerProxy( () -> this )
            {
                @Override
                protected TileComputerBase getTile()
                {
                    return TileComputer.this;
                }
            };
        }
        return proxy;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu( int id, @Nonnull Inventory inventory, @Nonnull Player player )
    {
        return new ComputerMenuWithoutInventory( ComputerCraftRegistry.ModContainers.COMPUTER, id, inventory, this::isUsableByPlayer, createServerComputer(), getFamily() );
    }

}
