/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import eu.pb4.polymer.core.api.utils.PolymerObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;

public abstract class TileGeneric extends BlockEntity implements PolymerObject
{
    public TileGeneric( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
    }

    public void destroy()
    {
    }

    // This is called by fabric event hook in ComputerCraftProxyCommon
    public void onChunkUnloaded()
    {
    }

    public final void updateBlock()
    {
        setChanged();
        BlockPos pos = getBlockPos();
        BlockState state = getBlockState();
        getLevel().sendBlockUpdated( pos, state, state, Block.UPDATE_ALL );
    }

    @Nonnull
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        return InteractionResult.PASS;
    }

    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
    }

    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
    }

    protected void blockTick()
    {
    }

    protected double getInteractRange( Player player )
    {
        return 8.0;
    }

    public boolean isUsable( Player player )
    {
        if( player == null || !player.isAlive() || getLevel().getBlockEntity( getBlockPos() ) != this ) return false;

        double range = getInteractRange( player );
        BlockPos pos = getBlockPos();
        return player.getCommandSenderWorld() == getLevel() &&
            player.distanceToSqr( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ) <= range * range;
    }
}
