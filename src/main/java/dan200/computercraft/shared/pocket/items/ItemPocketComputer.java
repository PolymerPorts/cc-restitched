/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.fabric.poly.ComputerGui;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem, PolymerItem
{
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_LIGHT = "Light";
    private static final String NBT_ON = "On";

    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;
    private final Item polymerItem;

    public ItemPocketComputer( Properties settings, ComputerFamily family )
    {
        super( settings );
        this.family = family;

        this.polymerItem = switch (family) {
            case NORMAL -> Items.IRON_INGOT;
            case ADVANCED -> Items.GOLD_INGOT;
            case COMMAND -> Items.COPPER_INGOT;
        };
    }

    public ItemStack create( int id, String label, int colour, IPocketUpgrade upgrade )
    {
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setHoverName( Component.literal( label ) );
        if( upgrade != null ) result.getOrCreateTag().putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        if( colour != -1 ) result.getOrCreateTag().putInt( NBT_COLOUR, colour );
        return result;
    }

    @Override
    public void fillItemCategory( @Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> stacks )
    {
        if( !allowedIn( group ) ) return;
        stacks.add( create( -1, null, -1, null ) );
        PocketUpgrades.getVanillaUpgrades().map( x -> create( -1, null, -1, x ) ).forEach( stacks::add );
    }

    private boolean tick( @Nonnull ItemStack stack, @Nonnull Level world, @Nonnull Entity entity, @Nonnull PocketServerComputer computer )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );

        computer.setLevel( world );
        computer.updateValues( entity, stack, upgrade );

        boolean changed = false;

        // Sync ID
        int id = computer.getID();
        if( id != getComputerID( stack ) )
        {
            changed = true;
            setComputerID( stack, id );
        }

        // Sync label
        String label = computer.getLabel();
        if( !Objects.equal( label, getLabel( stack ) ) )
        {
            changed = true;
            setLabel( stack, label );
        }

        boolean on = computer.isOn();
        if( on != isMarkedOn( stack ) )
        {
            changed = true;
            stack.getOrCreateTag().putBoolean( NBT_ON, on );
        }

        // Update pocket upgrade
        if( upgrade != null ) upgrade.update( computer, computer.getPeripheral( ComputerSide.BACK ) );

        return changed;
    }

    @Override
    public void inventoryTick( @Nonnull ItemStack stack, Level world, @Nonnull Entity entity, int slotNum, boolean selected )
    {
        if( !world.isClientSide )
        {
            Container inventory = entity instanceof Player player ? player.getInventory() : null;
            PocketServerComputer computer = createServerComputer( world, inventory, entity, stack );
            computer.keepAlive();

            boolean changed = tick( stack, world, entity, computer );
            if( changed && inventory != null ) inventory.setChanged();
        }
    }

    @Override
    public boolean onEntityItemUpdate( ItemStack stack, ItemEntity entity )
    {
        if( entity.level.isClientSide ) return false;

        PocketServerComputer computer = getServerComputer( stack );
        if( computer != null && tick( stack, entity.level, entity, computer ) ) entity.setItem( stack.copy() );
        return false;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use( Level world, Player player, @Nonnull InteractionHand hand )
    {
        ItemStack stack = player.getItemInHand( hand );
        if( !world.isClientSide )
        {
            PocketServerComputer computer = createServerComputer( world, player.getInventory(), player, stack );
            computer.turnOn();

            boolean stop = false;
            IPocketUpgrade upgrade = getUpgrade( stack );
            if( upgrade != null )
            {
                computer.updateValues( player, stack, upgrade );
                stop = upgrade.onRightClick( world, computer, computer.getPeripheral( ComputerSide.BACK ) );
            }

            if( !stop )
            {
                ComputerGui.open((ServerPlayer) player, new ComputerDisplayAccess() {
                    @Override
                    public ServerComputer getComputer() {
                        return computer;
                    }

                    @Override
                    public boolean canStayOpen(ServerPlayer player) {
                        return player.getItemInHand(hand) == stack;
                    }
                });

                boolean isTypingOnly = hand == InteractionHand.OFF_HAND;
                //new ComputerContainerData( computer ).open( player, new PocketComputerMenuProvider( computer, stack, this, hand, isTypingOnly ) );
            }
        }
        return new InteractionResultHolder<>( InteractionResult.SUCCESS, stack );
    }

    @Nonnull
    @Override
    public Component getName( @Nonnull ItemStack stack )
    {
        String baseString = getDescriptionId( stack );
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            return Component.translatable( baseString + ".upgraded",
                Component.translatable( upgrade.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return super.getName( stack );
        }
    }


    @Override
    public void appendHoverText( @Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> list, TooltipFlag flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                list.add( Component.translatable( "gui.computercraft.tooltip.computer_id", id )
                    .withStyle( ChatFormatting.GRAY ) );
            }
        }
    }

    public PocketServerComputer createServerComputer( final Level world, Container inventory, Entity entity, @Nonnull ItemStack stack )
    {
        if( world.isClientSide ) throw new IllegalStateException( "Cannot call createServerComputer on the client" );

        PocketServerComputer computer;
        int instanceID = getInstanceID( stack );
        int sessionID = getSessionID( stack );
        int correctSessionID = ComputerCraft.serverComputerRegistry.getSessionID();

        if( instanceID >= 0 && sessionID == correctSessionID && ComputerCraft.serverComputerRegistry.contains( instanceID ) )
        {
            computer = (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID );
        }
        else
        {
            if( instanceID < 0 || sessionID != correctSessionID )
            {
                instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                setInstanceID( stack, instanceID );
                setSessionID( stack, correctSessionID );
            }
            int computerID = getComputerID( stack );
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer" );
                setComputerID( stack, computerID );
            }
            computer = new PocketServerComputer( world, computerID, getLabel( stack ), instanceID, getFamily() );
            computer.updateValues( entity, stack, getUpgrade( stack ) );
            computer.addAPI( new PocketAPI( computer ) );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );

            // Only turn on when initially creating the computer, rather than each tick.
            if( isMarkedOn( stack ) && entity instanceof Player ) computer.turnOn();

            if( inventory != null ) inventory.setChanged();
        }
        computer.setLevel( world );
        return computer;
    }

    @Nullable
    public static PocketServerComputer getServerComputer( @Nonnull ItemStack stack )
    {
        int session = getSessionID( stack );
        if( session != ComputerCraft.serverComputerRegistry.getSessionID() ) return null;

        int instanceID = getInstanceID( stack );
        return instanceID >= 0 ? (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID ) : null;
    }

    // IComputerItem implementation

    private static void setComputerID( @Nonnull ItemStack stack, int computerID )
    {
        stack.getOrCreateTag().putInt( NBT_ID, computerID );
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return PocketComputerItemFactory.create(
            getComputerID( stack ), getLabel( stack ), getColour( stack ),
            family, getUpgrade( stack )
        );
    }

    // IMedia

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setHoverName( Component.literal( label ) );
        }
        else
        {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull Level world )
    {
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
        }
        return null;
    }

    private static int getInstanceID( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_INSTANCE ) ? nbt.getInt( NBT_INSTANCE ) : -1;
    }

    private static void setInstanceID( @Nonnull ItemStack stack, int instanceID )
    {
        stack.getOrCreateTag().putInt( NBT_INSTANCE, instanceID );
    }

    private static int getSessionID( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_SESSION ) ? nbt.getInt( NBT_SESSION ) : -1;
    }

    private static void setSessionID( @Nonnull ItemStack stack, int sessionID )
    {
        stack.getOrCreateTag().putInt( NBT_SESSION, sessionID );
    }

    private static boolean isMarkedOn( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.getBoolean( NBT_ON );
    }

    public static ComputerState getState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        return computer == null ? ComputerState.OFF : computer.getState();
    }

    public static int getLightState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        if( computer != null && computer.isOn() )
        {
            CompoundTag computerNBT = computer.getUserData();
            if( computerNBT != null && computerNBT.contains( NBT_LIGHT ) )
            {
                return computerNBT.getInt( NBT_LIGHT );
            }
        }
        return -1;
    }

    public static IPocketUpgrade getUpgrade( @Nonnull ItemStack stack )
    {
        CompoundTag compound = stack.getTag();
        return compound != null && compound.contains( NBT_UPGRADE )
            ? PocketUpgrades.get( compound.getString( NBT_UPGRADE ) ) : null;
    }

    public static void setUpgrade( @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        CompoundTag compound = stack.getOrCreateTag();

        if( upgrade == null )
        {
            compound.remove( NBT_UPGRADE );
        }
        else
        {
            compound.putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        }

        compound.remove( NBT_UPGRADE_INFO );
    }

    public static CompoundTag getUpgradeInfo( @Nonnull ItemStack stack )
    {
        return stack.getOrCreateTagElement( NBT_UPGRADE_INFO );
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        return this.polymerItem;
    }
}
