/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import eu.pb4.polymer.api.block.PolymerHeadBlock;
import eu.pb4.polymer.api.utils.PolymerUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class BlockWiredModemFull extends BlockGeneric implements PolymerHeadBlock
{
    public static final BooleanProperty MODEM_ON = BooleanProperty.create( "modem" );
    public static final BooleanProperty PERIPHERAL_ON = BooleanProperty.create( "peripheral" );

    public BlockWiredModemFull( Properties settings )
    {
        super( settings, () -> Registry.ModBlockEntities.WIRED_MODEM_FULL );
        registerDefaultState( getStateDefinition().any()
            .setValue( MODEM_ON, false )
            .setValue( PERIPHERAL_ON, false )
        );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM_ON, PERIPHERAL_ON );
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.PLAYER_HEAD;
    }

    @Override
    public String getPolymerSkinValue(BlockState state) {
        return PolymerUtils.NO_TEXTURE_HEAD_VALUE;
    }
}
