/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.fabric.poly.PolymerAutoTexturedItem;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.util.Colour;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemDisk extends Item implements IMedia, IColouredItem, PolymerAutoTexturedItem
{
    private static final String NBT_ID = "DiskId";

    public ItemDisk( Properties settings )
    {
        super( settings );
    }

    @Nonnull
    public static ItemStack createFromIDAndColour( int id, String label, int colour )
    {
        ItemStack stack = new ItemStack( Registry.ModItems.DISK );
        setDiskID( stack, id );
        Registry.ModItems.DISK.setLabel( stack, label );
        IColouredItem.setColourBasic( stack, colour );
        return stack;
    }

    @Override
    public void appendHoverText( @Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> list, TooltipFlag options )
    {
        if( options.isAdvanced() )
        {
            int id = getDiskID( stack );
            if( id >= 0 )
            {
                list.add( Component.translatable( "gui.computercraft.tooltip.disk_id", id )
                    .withStyle( ChatFormatting.GRAY ) );
            }
        }
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }

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
        int diskID = getDiskID( stack );
        if( diskID < 0 )
        {
            diskID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "disk" );
            setDiskID( stack, diskID );
        }
        return ComputerCraftAPI.createSaveDirMount( world, "disk/" + diskID, ComputerCraft.floppySpaceLimit );
    }

    public static int getDiskID( @Nonnull ItemStack stack )
    {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    }

    private static void setDiskID( @Nonnull ItemStack stack, int id )
    {
        if( id >= 0 ) stack.getOrCreateTag().putInt( NBT_ID, id );
    }

    @Override
    public int getColour( @Nonnull ItemStack stack )
    {
        int colour = IColouredItem.getColourBasic( stack );
        return colour == -1 ? Colour.WHITE.getHex() : colour;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        return Items.LEATHER_CHESTPLATE;
    }

    @Override
    public int getPolymerArmorColor(ItemStack itemStack, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        return getColour(itemStack);
    }
}
