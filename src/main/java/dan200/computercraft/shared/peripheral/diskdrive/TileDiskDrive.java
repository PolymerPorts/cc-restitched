/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RecordUtil;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TileDiskDrive extends TileGeneric implements IPeripheralTile, DefaultInventory, Nameable
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";

    private static class MountInfo
    {
        String mountPath;
    }

    Component customName;
    private LockCode lockCode = LockCode.NO_LOCK;

    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    @Nonnull
    private ItemStack diskStack = ItemStack.EMPTY;
    private DiskDrivePeripheral peripheral;
    private IMount diskMount = null;

    private boolean recordQueued = false;
    private boolean recordPlaying = false;
    private boolean restartRecord = false;
    private boolean ejectQueued;

    public TileDiskDrive( BlockEntityType<TileDiskDrive> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        if( recordPlaying ) stopRecord();
    }

    @Override
    public boolean isUsable( Player player )
    {
        return super.isUsable( player ) && BaseContainerBlockEntity.canUnlock( player, lockCode, getDisplayName() );
    }

    @Nonnull
    @Override
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        if( player.isCrouching() ) {
            // Try to put a disk into the drive
            ItemStack disk = player.getItemInHand( hand );
            if(!disk.isEmpty() && getItem(0).isEmpty() && MediaProviders.get(disk) != null) {
                setDiskStack(disk);
                player.setItemInHand(hand, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            } else if (disk.isEmpty() && !getItem( 0).isEmpty()) {
                player.setItemInHand(hand, getItem( 0));
                setDiskStack(ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        } else {
            // Open the GUI
            if( !getLevel().isClientSide && isUsable( player ) ) this.openMenu((ServerPlayer) player);
            return InteractionResult.SUCCESS;
        }
    }

    private void openMenu(ServerPlayer player) {
        var gui = new SimpleGui(MenuType.HOPPER, player, false);
        gui.setTitle(this.getDisplayName());
        var empty = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        empty.setHoverName(Component.empty());

        gui.setSlot(0, empty);
        gui.setSlot(1, empty);
        gui.setSlot(3, empty);
        gui.setSlot(4, empty);

        gui.setSlotRedirect(2, new Slot(this, 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return MediaProviders.get(itemStack) != null;
            }

            @Override
            public int getMaxStackSize(ItemStack itemStack) {
                return 1;
            }
        });

        gui.open();
    }

    public Direction getDirection()
    {
        return getBlockState().getValue( BlockDiskDrive.FACING );
    }

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );
        customName = nbt.contains( NBT_NAME ) ? Component.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            CompoundTag item = nbt.getCompound( NBT_ITEM );
            diskStack = ItemStack.of( item );
            diskMount = null;
        }

        lockCode = LockCode.fromTag( nbt );
    }

    @Override
    public void saveAdditional( @Nonnull CompoundTag nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, Component.Serializer.toJson( customName ) );

        if( !diskStack.isEmpty() )
        {
            CompoundTag item = new CompoundTag();
            diskStack.save( item );
            nbt.put( NBT_ITEM, item );
        }

        lockCode.addToTag( nbt );

        super.saveAdditional( nbt );
    }

    void serverTick()
    {
        // Ejection
        if( ejectQueued )
        {
            ejectContents( false );
            ejectQueued = false;
        }

        // Music
        synchronized( this )
        {
            if( recordPlaying != recordQueued || restartRecord )
            {
                restartRecord = false;
                if( recordQueued )
                {
                    IMedia contents = getDiskMedia();
                    SoundEvent record = contents != null ? contents.getAudio( diskStack ) : null;
                    if( record != null )
                    {
                        recordPlaying = true;
                        playRecord();
                    }
                    else
                    {
                        recordQueued = false;
                    }
                }
                else
                {
                    stopRecord();
                    recordPlaying = false;
                }
            }
        }
    }

    // IInventory implementation

    @Override
    public int getContainerSize()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return diskStack.isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getItem( int slot )
    {
        return diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int slot )
    {
        ItemStack result = diskStack;
        diskStack = ItemStack.EMPTY;
        diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int slot, int count )
    {
        if( diskStack.isEmpty() ) return ItemStack.EMPTY;

        if( diskStack.getCount() <= count )
        {
            ItemStack disk = diskStack;
            setItem( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = diskStack.split( count );
        setItem( slot, diskStack.isEmpty() ? ItemStack.EMPTY : diskStack );
        return part;
    }

    @Override
    public void setItem( int slot, @Nonnull ItemStack stack )
    {
        if( getLevel().isClientSide )
        {
            diskStack = stack;
            diskMount = null;
            setChanged();
            return;
        }

        synchronized( this )
        {
            if( InventoryUtil.areItemsStackable( stack, diskStack ) )
            {
                diskStack = stack;
                return;
            }

            // Unmount old disk
            if( !diskStack.isEmpty() )
            {
                // TODO: Is this iteration thread safe?
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers ) unmountDisk( computer );
            }

            // Stop music
            if( recordPlaying )
            {
                stopRecord();
                recordPlaying = false;
                recordQueued = false;
            }

            // Swap disk over
            diskStack = stack;
            diskMount = null;
            setChanged();

            // Mount new disk
            if( !diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers ) mountDisk( computer );
            }
        }
    }

    @Override
    public void setChanged()
    {
        if( !level.isClientSide ) updateBlockState();
        super.setChanged();
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return isUsable( player );
    }

    @Override
    public void clearContent()
    {
        setItem( 0, ItemStack.EMPTY );
    }

    @Nonnull
    ItemStack getDiskStack()
    {
        return getItem( 0 );
    }

    void setDiskStack( @Nonnull ItemStack stack )
    {
        setItem( 0, stack );
    }

    private IMedia getDiskMedia()
    {
        return MediaProviders.get( getDiskStack() );
    }

    String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            MountInfo info = computers.get( computer );
            return info != null ? info.mountPath : null;
        }
    }

    void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            computers.put( computer, new MountInfo() );
            mountDisk( computer );
        }
    }

    void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            computers.remove( computer );
        }
    }

    void playDiskAudio()
    {
        synchronized( this )
        {
            IMedia media = getDiskMedia();
            if( media != null && media.getAudioTitle( diskStack ) != null )
            {
                recordQueued = true;
                restartRecord = recordPlaying;
            }
        }
    }

    void stopDiskAudio()
    {
        synchronized( this )
        {
            recordQueued = false;
            restartRecord = false;
        }
    }

    void ejectDisk()
    {
        synchronized( this )
        {
            ejectQueued = true;
        }
    }

    // private methods

    private synchronized void mountDisk( IComputerAccess computer )
    {
        if( !diskStack.isEmpty() )
        {
            MountInfo info = computers.get( computer );
            IMedia contents = getDiskMedia();
            if( contents != null )
            {
                if( diskMount == null )
                {
                    diskMount = contents.createDataMount( diskStack, getLevel() );
                }
                if( diskMount != null )
                {
                    if( diskMount instanceof IWritableMount )
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mountWritable( n == 1 ? "disk" : "disk" + n, (IWritableMount) diskMount );
                            n++;
                        }
                    }
                    else
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mount( n == 1 ? "disk" : "disk" + n, diskMount );
                            n++;
                        }
                    }
                }
                else
                {
                    info.mountPath = null;
                }
            }
            computer.queueEvent( "disk", computer.getAttachmentName() );
        }
    }

    private synchronized void unmountDisk( IComputerAccess computer )
    {
        if( !diskStack.isEmpty() )
        {
            MountInfo info = computers.get( computer );
            assert info != null;
            if( info.mountPath != null )
            {
                computer.unmount( info.mountPath );
                info.mountPath = null;
            }
            computer.queueEvent( "disk_eject", computer.getAttachmentName() );
        }
    }

    private void updateBlockState()
    {
        if( remove || level == null ) return;

        if( !diskStack.isEmpty() )
        {
            IMedia contents = getDiskMedia();
            updateBlockState( contents != null ? DiskDriveState.FULL : DiskDriveState.INVALID );
        }
        else
        {
            updateBlockState( DiskDriveState.EMPTY );
        }
    }

    private void updateBlockState( DiskDriveState state )
    {
        BlockState blockState = getBlockState();
        if( blockState.getValue( BlockDiskDrive.STATE ) == state ) return;

        getLevel().setBlockAndUpdate( getBlockPos(), blockState.setValue( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getLevel().isClientSide || diskStack.isEmpty() ) return;

        // Remove the disks from the inventory
        ItemStack disks = diskStack;
        setDiskStack( ItemStack.EMPTY );

        // Spawn the item in the world
        int xOff = 0;
        int zOff = 0;
        if( !destroyed )
        {
            Direction dir = getDirection();
            xOff = dir.getStepX();
            zOff = dir.getStepZ();
        }

        BlockPos pos = getBlockPos();
        double x = pos.getX() + 0.5 + xOff * 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5 + zOff * 0.5;
        ItemEntity entityitem = new ItemEntity( getLevel(), x, y, z, disks );
        entityitem.setDeltaMovement( xOff * 0.15, 0, zOff * 0.15 );

        getLevel().addFreshEntity( entityitem );
        if( !destroyed ) getLevel().globalLevelEvent( 1000, getBlockPos(), 0 );
    }

    // Private methods

    private void playRecord()
    {
        IMedia contents = getDiskMedia();
        SoundEvent record = contents != null ? contents.getAudio( diskStack ) : null;
        if( record != null )
        {
            RecordUtil.playRecord( record, contents.getAudioTitle( diskStack ), getLevel(), getBlockPos() );
        }
        else
        {
            RecordUtil.playRecord( null, null, getLevel(), getBlockPos() );
        }
    }

    private void stopRecord()
    {
        RecordUtil.playRecord( null, null, getLevel(), getBlockPos() );
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( @NotNull Direction side )
    {
        if( peripheral == null ) peripheral = new DiskDrivePeripheral( this );
        return peripheral;
    }

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
    }

    @Nullable
    @Override
    public Component getCustomName()
    {
        return customName;
    }

    @Nonnull
    @Override
    public Component getName()
    {
        return customName != null ? customName : Component.translatable( getBlockState().getBlock().getDescriptionId() );
    }

    @Nonnull
    @Override
    public Component getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }
}
