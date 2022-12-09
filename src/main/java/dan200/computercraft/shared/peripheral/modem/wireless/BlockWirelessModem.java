/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.fabric.poly.textures.HeadTextures;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class BlockWirelessModem extends BlockGeneric implements SimpleWaterloggedBlock, PolymerHeadBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create( "on" );
    private final HeadTextures.SidedModem texture;

    public BlockWirelessModem(Properties settings, Supplier<BlockEntityType<? extends TileWirelessModem>> type, HeadTextures.SidedModem texture)
    {
        super( settings, type );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( ON, false )
            .setValue( WATERLOGGED, false ) );

        this.texture = texture;
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, ON, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( BlockState blockState, @Nonnull BlockGetter blockView, @Nonnull BlockPos blockPos, @Nonnull CollisionContext context )
    {
        return ModemShapes.getBounds( blockState.getValue( FACING ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return WaterloggableHelpers.getFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updateShape( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull LevelAccessor world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        WaterloggableHelpers.updateShape( state, world, pos );
        return side == state.getValue( FACING ) && !state.canSurvive( world, pos )
            ? state.getFluidState().createLegacyBlock()
            : state;
    }

    @Override
    @Deprecated
    public boolean canSurvive( BlockState state, @Nonnull LevelReader world, BlockPos pos )
    {
        Direction facing = state.getValue( FACING );
        return canSupportCenter( world, pos.relative( facing ), facing.getOpposite() ) || world.getBlockState(pos.relative( facing )).getBlock() instanceof BlockGeneric;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState()
            .setValue( FACING, placement.getClickedFace().getOpposite() )
            .setValue( WATERLOGGED, getFluidStateForPlacement( placement ) );
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

    private final BlockEntityTicker<TileWirelessModem> serverTicker = ( level, pos, state, computer ) -> computer.serverTick();

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<U> type )
    {
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper( type, (BlockEntityType<TileWirelessModem>) this.getBEType(), serverTicker );
    }

    @Override
    public String getPolymerSkinValue(BlockState state, BlockPos pos, ServerPlayer player) {
        return switch (state.getValue(FACING)) {
            case UP -> this.texture.up();
            case DOWN -> this.texture.down();
            default -> this.texture.side();
        };
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return state.getValue(FACING).getAxis() != Direction.Axis.Y ? Blocks.PLAYER_WALL_HEAD : Blocks.PLAYER_HEAD;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        var dir = state.getValue(FACING);
        return dir.getAxis() != Direction.Axis.Y
            ? Blocks.PLAYER_WALL_HEAD.defaultBlockState().setValue(WallSkullBlock.FACING, dir.getOpposite())
            : Blocks.PLAYER_HEAD.defaultBlockState().setValue(SkullBlock.ROTATION, dir.getStepY() == 1 ? 0 : 2);
    }
}
