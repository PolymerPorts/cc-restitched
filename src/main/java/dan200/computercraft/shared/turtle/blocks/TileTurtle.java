/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.fabric.mixin.poly.ArmorStandAccessor;
import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.fabric.poly.textures.HeadTextures;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.*;
import eu.pb4.polymer.api.utils.PolymerObject;
import eu.pb4.polymer.api.utils.PolymerUtils;
import eu.pb4.polymer.impl.other.FakeWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileTurtle extends TileComputerBase implements ITurtleTile, DefaultInventory, PolymerObject
{
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    enum MoveState
    {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private final NonNullList<ItemStack> inventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private final NonNullList<ItemStack> previousInventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private boolean inventoryChanged = false;
    private TurtleBrain brain = new TurtleBrain( this );
    private MoveState moveState = MoveState.NOT_MOVED;

    @Nullable
    public TurtleModel model = null;

    public TileTurtle( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state, family );
    }

    private boolean hasMoved()
    {
        return moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = new ServerComputer(
            getLevel(), id, label, instanceID, getFamily(),
            ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight
        );
        computer.setPosition( getBlockPos() );
        computer.addAPI( new TurtleAPI( computer.getAPIEnvironment(), getAccess() ) );
        brain.setupComputer( computer );
        return computer;
    }

    public ComputerProxy createProxy()
    {
        return brain.getProxy();
    }

    @Override
    public void destroy()
    {
        if( !hasMoved() )
        {
            // Stop computer
            super.destroy();

            // Drop contents
            if( !getLevel().isClientSide )
            {
                int size = getContainerSize();
                for( int i = 0; i < size; i++ )
                {
                    ItemStack stack = getItem( i );
                    if( !stack.isEmpty() )
                    {
                        WorldUtil.dropItemStack( stack, getLevel(), getBlockPos() );
                    }
                }
            }
        }
        else
        {
            // Just turn off any redstone we had on
            for( Direction dir : DirectionUtil.FACINGS )
            {
                RedstoneUtil.propagateRedstoneOutput( getLevel(), getBlockPos(), dir );
            }
        }
    }

    @Override
    protected void unload()
    {
        if( !hasMoved() )
        {
            super.unload();
        }

        if (this.model != null) {
            for (var player : this.model.watchers) {
                player.connection.send(new ClientboundRemoveEntitiesPacket(this.model.main.getId()));
                if (this.model.right != null) {
                    player.connection.send(new ClientboundRemoveEntitiesPacket(this.model.right.getId()));
                }

                if (this.model.left != null) {
                    player.connection.send(new ClientboundRemoveEntitiesPacket(this.model.left.getId()));
                }
            }
        }
    }

    @Nonnull
    @Override
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        // Apply dye
        ItemStack currentItem = player.getItemInHand( hand );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() instanceof DyeItem dyeItem )
            {
                // Dye to change turtle colour
                if( !getLevel().isClientSide )
                {
                    DyeColor dye = dyeItem.getDyeColor();
                    if( brain.getDyeColour() != dye )
                    {
                        brain.setDyeColour( dye );
                        if( !player.isCreative() )
                        {
                            currentItem.shrink( 1 );
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            else if( currentItem.getItem() == Items.WATER_BUCKET && brain.getColour() != -1 )
            {
                // Water to remove turtle colour
                if( !getLevel().isClientSide )
                {
                    if( brain.getColour() != -1 )
                    {
                        brain.setColour( -1 );
                        if( !player.isCreative() )
                        {
                            player.setItemInHand( hand, new ItemStack( Items.BUCKET ) );
                            player.getInventory().setChanged();
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, hand, hit );
    }

    @Override
    protected boolean canNameWithTag( Player player )
    {
        return true;
    }

    @Override
    protected double getInteractRange( Player player )
    {
        return 12.0;
    }

    @Override
    protected void serverTick()
    {
        super.serverTick();
        brain.update();

        if (this.model == null && this.moveState == MoveState.NOT_MOVED) {
            this.model = new TurtleModel(this.createProxy());
            this.model.setPos(Vec3.atBottomCenterOf(this.getBlockPos()), this.getDirection());
        }

        if (this.model != null) {
            this.model.tick();

            if (this.model.main.getYRot() != this.getDirection().toYRot()) {
                this.model.setPos(Vec3.atBottomCenterOf(this.getBlockPos()), this.getDirection());
            }
        }

        if( inventoryChanged )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) computer.queueEvent( "turtle_inventory" );

            inventoryChanged = false;
            for( int n = 0; n < getContainerSize(); n++ )
            {
                previousInventory.set( n, getItem( n ).copy() );
            }
        }
    }

    protected void clientTick()
    {
        brain.update();
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED ) super.onNeighbourChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED ) super.onNeighbourTileEntityChange( neighbour );
    }

    public void notifyMoveStart()
    {
        if( moveState == MoveState.NOT_MOVED ) moveState = MoveState.IN_PROGRESS;
    }

    public void notifyMoveEnd()
    {
        // MoveState.MOVED is final
        if( moveState == MoveState.IN_PROGRESS ) moveState = MoveState.NOT_MOVED;
    }

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );

        // Read inventory
        ListTag nbttaglist = nbt.getList( "Items", Tag.TAG_COMPOUND );
        inventory.clear();
        previousInventory.clear();
        for( int i = 0; i < nbttaglist.size(); i++ )
        {
            CompoundTag tag = nbttaglist.getCompound( i );
            int slot = tag.getByte( "Slot" ) & 0xff;
            if( slot < getContainerSize() )
            {
                inventory.set( slot, ItemStack.of( tag ) );
                previousInventory.set( slot, inventory.get( slot ).copy() );
            }
        }

        // Read state
        brain.readFromNBT( nbt );
        brain.readDescription( nbt );
    }

    //    @Override
    //    public void handleUpdateTag( @Nonnull CompoundTag nbt )
    //    {
    //        super.handleUpdateTag( nbt );
    //        brain.readDescription( nbt );
    //    }

    @Override
    public void saveAdditional( @Nonnull CompoundTag nbt )
    {
        // Write inventory
        ListTag nbttaglist = new ListTag();
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                CompoundTag tag = new CompoundTag();
                tag.putByte( "Slot", (byte) i );
                inventory.get( i ).save( tag );
                nbttaglist.add( tag );
            }
        }
        nbt.put( "Items", nbttaglist );

        // Write brain
        nbt = brain.writeToNBT( nbt );

        super.saveAdditional( nbt );
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    // IDirectionalTile

    @Override
    public Direction getDirection()
    {
        return getBlockState().getValue( BlockTurtle.FACING );
    }

    public void setDirection( Direction dir )
    {
        if( dir.getAxis() == Direction.Axis.Y ) dir = Direction.NORTH;
        level.setBlockAndUpdate( worldPosition, getBlockState().setValue( BlockTurtle.FACING, dir ) );

        updateOutput();
        updateInputsImmediately();

        onTileEntityChange();
    }

    // ITurtleTile

    @Override
    public ITurtleUpgrade getUpgrade( TurtleSide side )
    {
        return brain.getUpgrade( side );
    }

    @Override
    public int getColour()
    {
        return brain.getColour();
    }

    @Override
    public ResourceLocation getOverlay()
    {
        return brain.getOverlay();
    }

    @Override
    public ITurtleAccess getAccess()
    {
        return brain;
    }

    @Override
    public Vec3 getRenderOffset( float f )
    {
        return brain.getRenderOffset( f );
    }

    @Override
    public float getRenderYaw( float f )
    {
        return brain.getVisualYaw( f );
    }

    @Override
    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return brain.getToolRenderAngle( side, f );
    }

    void setOwningPlayer( GameProfile player )
    {
        brain.setOwningPlayer( player );
        setChanged();
    }

    // IInventory

    @Override
    public int getContainerSize()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem( int slot )
    {
        return slot >= 0 && slot < INVENTORY_SIZE ? inventory.get( slot ) : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int slot )
    {
        ItemStack result = getItem( slot );
        setItem( slot, ItemStack.EMPTY );
        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int slot, int count )
    {
        if( count == 0 ) return ItemStack.EMPTY;

        ItemStack stack = getItem( slot );
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        if( stack.getCount() <= count )
        {
            setItem( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        onInventoryDefinitelyChanged();
        return part;
    }

    @Override
    public void setItem( int i, @Nonnull ItemStack stack )
    {
        if ( i >= 0 && i < INVENTORY_SIZE )
        {
            inventory.set( i, stack );
            if ( !InventoryUtil.areItemsEqual( stack, inventory.get( i ) ) )
            {
                onInventoryDefinitelyChanged();
            }
        }
    }

    @Override
    public void clearContent()
    {
        boolean changed = false;
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                inventory.set( i, ItemStack.EMPTY );
                changed = true;
            }
        }

        if( changed ) onInventoryDefinitelyChanged();
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        if( !inventoryChanged )
        {
            for( int n = 0; n < getContainerSize(); n++ )
            {
                if( !ItemStack.matches( getItem( n ), previousInventory.get( n ) ) )
                {
                    inventoryChanged = true;
                    break;
                }
            }
        }
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return isUsable( player );
    }

    private void onInventoryDefinitelyChanged()
    {
        super.setChanged();
        inventoryChanged = true;
    }

    public void onTileEntityChange()
    {
        super.setChanged();
    }

    // Networking stuff

    @Nonnull
    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbt = super.getUpdateTag();
        brain.writeDescription( nbt );
        return nbt;
    }

    @Nonnull
    @Override
    protected String getPeripheralName()
    {
        return "turtle";
    }

    // Privates

    private boolean hasPeripheralUpgradeOnSide( ComputerSide side )
    {
        ITurtleUpgrade upgrade;
        switch( side )
        {
            case RIGHT:
                upgrade = getUpgrade( TurtleSide.RIGHT );
                break;
            case LEFT:
                upgrade = getUpgrade( TurtleSide.LEFT );
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom( TileTurtle copy )
    {
        super.transferStateFrom( copy );
        Collections.copy( inventory, copy.inventory );
        Collections.copy( previousInventory, copy.previousInventory );
        inventoryChanged = copy.inventoryChanged;
        brain = copy.brain;
        brain.setOwner( this );

        this.model = copy.model;
        this.model.setPos(Vec3.atBottomCenterOf(this.getBlockPos()), this.getDirection());
        // Mark the other turtle as having moved, and so its peripheral is dead.
        copy.moveState = MoveState.MOVED;
        copy.model = null;
    }

    @Override
    public ComputerDisplayAccess getDisplayAccess() {
        return this.createProxy();
    }

    public static class TurtleModel {

        private final ComputerProxy proxy;
        private final ArmorStand main;
        @Nullable
        private final ArmorStand right;
        @Nullable
        private final ArmorStand left;

        private final Set<ServerPlayer> watchers = new HashSet<>();

        public TurtleModel(ComputerProxy proxy) {
            this.proxy = proxy;
            this.main = new ArmorStand(EntityType.ARMOR_STAND, FakeWorld.INSTANCE);
            this.main.setNoGravity(true);
            this.main.setInvisible(true);
            var stack = new ItemStack(Items.PLAYER_HEAD);
            stack.getOrCreateTag().put("SkullOwner", PolymerUtils.createSkullOwner(this.proxy.getBlockEntity().getFamily() == ComputerFamily.ADVANCED ? HeadTextures.ADVANCED_TURTLE : HeadTextures.TURTLE));
            this.main.setItemSlot(EquipmentSlot.HEAD, stack);

            var rightUpgrade = ((TileTurtle) proxy.getBlockEntity()).getUpgrade(TurtleSide.RIGHT);
            if (rightUpgrade != null) {
                this.right = new ArmorStand(EntityType.ARMOR_STAND, FakeWorld.INSTANCE);
                this.right.setNoGravity(true);
                this.right.setInvisible(true);
                ((ArmorStandAccessor) this.right).callSetSmall(rightUpgrade.getCraftingItem().getItem() instanceof BlockItem);
                this.right.setItemSlot(EquipmentSlot.HEAD, rightUpgrade.getCraftingItem().copy());
            } else {
                this.right = null;
            }


            var leftUpgrade = ((TileTurtle) proxy.getBlockEntity()).getUpgrade(TurtleSide.LEFT);
            if (leftUpgrade != null) {
                this.left = new ArmorStand(EntityType.ARMOR_STAND, FakeWorld.INSTANCE);
                this.left.setNoGravity(true);
                this.left.setInvisible(true);
                ((ArmorStandAccessor) this.left).callSetSmall(leftUpgrade.getCraftingItem().getItem() instanceof BlockItem);
                this.left.setItemSlot(EquipmentSlot.HEAD, leftUpgrade.getCraftingItem().copy());
            } else {
                this.left = null;
            }
        }

        public void tick() {
            boolean active = this.proxy.getBlockEntity() != null;

            for (var player : new ArrayList<>(this.watchers)) {
                if (player.isRemoved()) {
                    this.watchers.remove(player);
                } else if (active && player.getEyePosition().distanceToSqr(this.main.getEyePosition()) > 32*32) {
                    player.connection.send(new ClientboundRemoveEntitiesPacket(this.main.getId()));
                    if (this.right != null) {
                        player.connection.send(new ClientboundRemoveEntitiesPacket(this.right.getId()));
                    }
                    if (this.left != null) {
                        player.connection.send(new ClientboundRemoveEntitiesPacket(this.left.getId()));
                    }
                    this.watchers.remove(player);
                }
            }

            if (active) {
                for (var player : ((ServerLevel) this.proxy.getBlockEntity().getLevel()).getPlayers((player) -> player.getEyePosition().distanceToSqr(this.main.getEyePosition()) < 32*32)) {
                    if (this.watchers.add(player)) {
                        player.connection.send(this.main.getAddEntityPacket());
                        player.connection.send(new ClientboundSetEntityDataPacket(this.main.getId(), this.main.getEntityData(), true));
                        player.connection.send(new ClientboundSetEquipmentPacket(this.main.getId(),
                            List.of(Pair.of(EquipmentSlot.HEAD, this.main.getItemBySlot(EquipmentSlot.HEAD)))));

                        if (this.right != null) {
                            player.connection.send(this.right.getAddEntityPacket());
                            player.connection.send(new ClientboundSetEntityDataPacket(this.right.getId(), this.right.getEntityData(), true));
                            player.connection.send(new ClientboundSetEquipmentPacket(this.right.getId(),
                                List.of(Pair.of(EquipmentSlot.HEAD, this.right.getItemBySlot(EquipmentSlot.HEAD)))));
                        }

                        if (this.left != null) {
                            player.connection.send(this.left.getAddEntityPacket());
                            player.connection.send(new ClientboundSetEntityDataPacket(this.left.getId(), this.left.getEntityData(), true));
                            player.connection.send(new ClientboundSetEquipmentPacket(this.left.getId(),
                                List.of(Pair.of(EquipmentSlot.HEAD, this.left.getItemBySlot(EquipmentSlot.HEAD)))));
                        }
                    }
                }
            }
        }

        public void setPos(Vec3 pos, Direction direction) {
            this.main.setPos(pos.add(0, -1.4, 0));
            this.main.setYRot(direction.toYRot());

            Packet<ClientGamePacketListener> right;
            Packet<ClientGamePacketListener> left;

            if (this.right != null) {
                if (this.right.isSmall()) {
                    this.right.setPos(pos.add(direction.getStepZ() * -0.3, -0.4, direction.getStepX() * 0.3));
                } else {
                    this.right.setPos(pos.add(direction.getStepZ() * -0.65, -1.7, direction.getStepX() * 0.65));
                }
                this.right.setYRot(direction.getClockWise().toYRot());

                right = new ClientboundTeleportEntityPacket(this.right);
            } else {
                right = null;
            }

            if (this.left != null) {
                if (this.left.isSmall()) {
                    this.left.setPos(pos.add(direction.getStepZ() * 0.3, -0.4, direction.getStepX() * 0.3));
                    this.left.setYRot(direction.getCounterClockWise().toYRot());
                } else {
                    this.left.setPos(pos.add(direction.getStepZ() * 0.1, -1.7, direction.getStepX() * 0.1));
                    this.left.setYRot(direction.getClockWise().toYRot());
                }

                left = new ClientboundTeleportEntityPacket(this.left);
            } else {
                left = null;
            }

            var packet = new ClientboundTeleportEntityPacket(this.main);
            for (var player : this.watchers) {
                player.connection.send(packet);

                if (right != null) {
                    player.connection.send(right);
                }

                if (left != null) {
                    player.connection.send(left);
                }
            }
        }
    }
}
