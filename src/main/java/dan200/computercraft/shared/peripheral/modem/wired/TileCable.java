/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class TileCable extends TileGeneric implements IPeripheralTile
{
    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral();
    private final WiredModemElement cable = new CableElement();
    private final IWiredNode node = cable.getNode();
    private boolean peripheralAccessAllowed;
    private boolean destroyed = false;
    private final WiredModemPeripheral modem = new WiredModemPeripheral( new ModemState( () -> TickScheduler.schedule( this ) ), cable )
    {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral()
        {
            return peripheral;
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos pos = getBlockPos().relative( getDirection() );
            return new Vec3( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return TileCable.this;
        }
    };
    private boolean connectionsFormed = false;

    public TileCable( BlockEntityType<? extends TileCable> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            destroyed = true;
            modem.destroy();
            onRemove();
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        onRemove();
    }

    private void onRemove()
    {
        if( level == null || !level.isClientSide )
        {
            node.remove();
            connectionsFormed = false;
        }
    }

    @Nonnull
    @Override
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        if( player.isCrouching() )
        {
            return InteractionResult.PASS;
        }
        if( !canAttachPeripheral() )
        {
            return InteractionResult.FAIL;
        }

        if( this.level.isClientSide )
        {
            return InteractionResult.SUCCESS;
        }

        String oldName = peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.displayClientMessage( new TranslatableComponent( "chat.computercraft.wired_modem.peripheral_disconnected",
                    ChatHelpers.copy( oldName ) ), false );
            }
            if( newName != null )
            {
                player.displayClientMessage( new TranslatableComponent( "chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy( newName ) ), false );
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        Direction dir = getDirection();
        if( neighbour.equals( getBlockPos().relative( dir ) ) && hasModem() && !getBlockState().canSurvive( getLevel(), getBlockPos() ) )
        {
            if( hasCable() )
            {
                // Drop the modem and convert to cable
                Block.popResource( getLevel(), getBlockPos(), new ItemStack( ComputerCraftRegistry.ModItems.WIRED_MODEM ) );
                getLevel().setBlockAndUpdate( getBlockPos(),
                    getBlockState().setValue( BlockCable.MODEM, CableModemVariant.None ) );
                modemChanged();
                connectionsChanged();
            }
            else
            {
                // Drop everything and remove block
                Block.popResource( getLevel(), getBlockPos(), new ItemStack( ComputerCraftRegistry.ModItems.WIRED_MODEM ) );
                getLevel().removeBlock( getBlockPos(), false );
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        onNeighbourTileEntityChange( neighbour );
    }

    @Nonnull
    private Direction getDirection()
    {
        Direction direction = getMaybeDirection();
        return direction == null ? Direction.NORTH : direction;
    }

    public boolean hasModem()
    {
        return getBlockState().getValue( BlockCable.MODEM ) != CableModemVariant.None;
    }

    boolean hasCable()
    {
        return getBlockState().getValue( BlockCable.CABLE );
    }

    void modemChanged()
    {
        // Tell anyone who cares that the connection state has changed
        if( getLevel().isClientSide )
        {
            return;
        }

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if( !canAttachPeripheral() && peripheralAccessAllowed )
        {
            peripheralAccessAllowed = false;
            peripheral.detach();
            node.updatePeripherals( Collections.emptyMap() );
            setChanged();
            updateBlockState();
        }
    }

    void connectionsChanged()
    {
        if( getLevel().isClientSide )
        {
            return;
        }

        BlockState state = getBlockState();
        Level world = getLevel();
        BlockPos current = getBlockPos();
        for( Direction facing : DirectionUtil.FACINGS )
        {
            BlockPos offset = current.relative( facing );
            if( !world.hasChunkAt( offset ) )
            {
                continue;
            }

            IWiredElement element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element != null )
            {
                // TODO Figure out why this crashes.
                IWiredNode node = element.getNode();
                if( node != null && this.node != null )
                {
                    if( BlockCable.canConnectIn( state, facing ) )
                    {
                        // If we can connect to it then do so
                        this.node.connectTo( node );
                    }
                    else if( this.node.getNetwork() == node.getNetwork() )
                    {
                        // Otherwise if we're on the same network then attempt to void it.
                        this.node.disconnectFrom( node );
                    }
                }
            }
        }
    }

    private boolean canAttachPeripheral()
    {
        return hasCable() && hasModem();
    }

    private void updateBlockState()
    {
        BlockState state = getBlockState();
        CableModemVariant oldVariant = state.getValue( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant.from( oldVariant.getFacing(), modem.getModemState()
            .isOpen(), peripheralAccessAllowed );

        if( oldVariant != newVariant )
        {
            level.setBlockAndUpdate( getBlockPos(), state.setValue( BlockCable.MODEM, newVariant ) );
        }
    }

    private void refreshPeripheral()
    {
        if( level != null && !isRemoved() && peripheral.attach( level, getBlockPos(), getDirection() ) )
        {
            updateConnectedPeripherals();
        }
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            peripheralAccessAllowed = false;
            updateBlockState();
        }

        node.updatePeripherals( peripherals );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !level.isClientSide && peripheralAccessAllowed )
        {
            Direction facing = getDirection();
            if( getBlockPos().relative( facing )
                .equals( neighbour ) )
            {
                refreshPeripheral();
            }
        }
    }

    @Override
    public void blockTick()
    {
        if( getLevel().isClientSide )
        {
            return;
        }

        if( modem.getModemState()
            .pollChanged() )
        {
            updateBlockState();
        }

        if( !connectionsFormed )
        {
            connectionsFormed = true;

            connectionsChanged();
            if( peripheralAccessAllowed )
            {
                peripheral.attach( level, worldPosition, getDirection() );
                updateConnectedPeripherals();
            }
        }
    }

    private void togglePeripheralAccess()
    {
        if( !peripheralAccessAllowed )
        {
            peripheral.attach( level, getBlockPos(), getDirection() );
            if( !peripheral.hasPeripheral() )
            {
                return;
            }

            peripheralAccessAllowed = true;
            node.updatePeripherals( peripheral.toMap() );
        }
        else
        {
            peripheral.detach();

            peripheralAccessAllowed = false;
            node.updatePeripherals( Collections.emptyMap() );
        }

        updateBlockState();
    }

    @Nullable
    private Direction getMaybeDirection()
    {
        return getBlockState().getValue( BlockCable.MODEM ).getFacing();
    }

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );
        peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public CompoundTag save( CompoundTag nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed );
        peripheral.write( nbt, "" );
        return super.save( nbt );
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        onRemove();
    }

    @Override
    public void clearRemoved()
    {
        super.clearRemoved();
        TickScheduler.schedule( this );
    }

    @Override
    public void setBlockState( BlockState state )
    {
        super.setBlockState( state );
        if( state != null ) return;
        if( !level.isClientSide )
        {
            level.getBlockTicks()
                .scheduleTick( worldPosition,
                    getBlockState().getBlock(), 0 );
        }
    }

    public IWiredElement getElement( Direction facing )
    {
        return BlockCable.canConnectIn( getBlockState(), facing ) ? cable : null;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return !destroyed && hasModem() && side == getDirection() ? modem : null;
    }

    private class CableElement extends WiredModemElement
    {
        @Nonnull
        @Override
        public Level getLevel()
        {
            return TileCable.this.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos pos = getBlockPos();
            return new Vec3( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            modem.attachPeripheral( name, peripheral );
        }

        @Override
        protected void detachPeripheral( String name )
        {
            modem.detachPeripheral( name );
        }
    }
}
