/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.fabric.poly.textures.HeadTextures;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockComputer<T extends TileComputer> extends BlockComputerBase<T> implements PolymerHeadBlock
{
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create( "state", ComputerState.class );
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final String offTexture;
    private final String onTexture;

    public BlockComputer( Properties settings, ComputerFamily family, Supplier<BlockEntityType<T>> type )
    {
        super( settings, family, type );
        registerDefaultState( defaultBlockState()
            .setValue( FACING, Direction.NORTH )
            .setValue( STATE, ComputerState.OFF )
        );

        this.offTexture = switch (family) {
            case ADVANCED -> HeadTextures.ADVANCED_COMPUTER;
            case NORMAL -> HeadTextures.COMPUTER;
            case COMMAND -> HeadTextures.COMMAND_COMPUTER;
            default -> PolymerUtils.NO_TEXTURE_HEAD_VALUE;
        };

        this.onTexture = switch (family) {
            case ADVANCED -> HeadTextures.ADVANCED_COMPUTER_ON;
            case NORMAL -> HeadTextures.COMPUTER_ON;
            case COMMAND -> HeadTextures.COMMAND_COMPUTER;
            default -> PolymerUtils.NO_TEXTURE_HEAD_VALUE;
        };
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, STATE );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState().setValue( FACING, placement.getHorizontalDirection().getOpposite() );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState mirror( BlockState state, Mirror mirrorIn )
    {
        return state.rotate( mirrorIn.getRotation( state.getValue( FACING ) ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState rotate( BlockState state, Rotation rot )
    {
        return state.setValue( FACING, rot.rotate( state.getValue( FACING ) ) );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileComputer ? ComputerItemFactory.create( (TileComputer) tile ) : ItemStack.EMPTY;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.PLAYER_HEAD;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.PLAYER_HEAD.defaultBlockState().setValue(SkullBlock.ROTATION, state.getValue(FACING).getOpposite().get2DDataValue() * 4);
    }

    @Override
    public String getPolymerSkinValue(BlockState state, BlockPos pos, ServerPlayer player) {
        return state.getValue(STATE) == ComputerState.OFF ? this.offTexture : this.onTexture;
    }
}
