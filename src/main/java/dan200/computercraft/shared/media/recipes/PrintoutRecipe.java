/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemPrintout;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class PrintoutRecipe extends CustomRecipe implements PolymerRecipe
{
    private final Ingredient paper = Ingredient.of( Items.PAPER );
    private final Ingredient leather = Ingredient.of( Items.LEATHER );
    private final Ingredient string = Ingredient.of( Items.STRING );

    private PrintoutRecipe( ResourceLocation id, CraftingBookCategory category )
    {
        super( id, category );
    }

    @Override
    public boolean canCraftInDimensions( int x, int y )
    {
        return x >= 3 && y >= 3;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess)
    {
        return ItemPrintout.createMultipleFromTitleAndText( null, null, null );
    }

    @Override
    public boolean matches( @Nonnull CraftingContainer inventory, @Nonnull Level world )
    {
        return !assemble( inventory, world.registryAccess() ).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingContainer inventory, RegistryAccess registryAccess )
    {
        // See if we match the recipe, and extract the input disk ID and dye colour
        int numPages = 0;
        int numPrintouts = 0;
        ItemStack[] printouts = null;
        boolean stringFound = false;
        boolean leatherFound = false;
        boolean printoutFound = false;
        for( int y = 0; y < inventory.getHeight(); y++ )
        {
            for( int x = 0; x < inventory.getWidth(); x++ )
            {
                ItemStack stack = inventory.getItem( x + y * inventory.getWidth() );
                if( !stack.isEmpty() )
                {
                    if( stack.getItem() instanceof ItemPrintout printout && printout.getType() != ItemPrintout.Type.BOOK )
                    {
                        if( printouts == null ) printouts = new ItemStack[9];
                        printouts[numPrintouts] = stack;
                        numPages += ItemPrintout.getPageCount( stack );
                        numPrintouts++;
                        printoutFound = true;
                    }
                    else if( paper.test( stack ) )
                    {
                        if( printouts == null )
                        {
                            printouts = new ItemStack[9];
                        }
                        printouts[numPrintouts] = stack;
                        numPages++;
                        numPrintouts++;
                    }
                    else if( string.test( stack ) && !stringFound )
                    {
                        stringFound = true;
                    }
                    else if( leather.test( stack ) && !leatherFound )
                    {
                        leatherFound = true;
                    }
                    else
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Build some pages with what was passed in
        if( numPages <= ItemPrintout.MAX_PAGES && stringFound && printoutFound && numPrintouts >= (leatherFound ? 1 : 2) )
        {
            String[] text = new String[numPages * ItemPrintout.LINES_PER_PAGE];
            String[] colours = new String[numPages * ItemPrintout.LINES_PER_PAGE];
            int line = 0;

            for( int printout = 0; printout < numPrintouts; printout++ )
            {
                ItemStack stack = printouts[printout];
                if( stack.getItem() instanceof ItemPrintout )
                {
                    // Add a printout
                    String[] pageText = ItemPrintout.getText( printouts[printout] );
                    String[] pageColours = ItemPrintout.getColours( printouts[printout] );
                    for( int pageLine = 0; pageLine < pageText.length; pageLine++ )
                    {
                        text[line] = pageText[pageLine];
                        colours[line] = pageColours[pageLine];
                        line++;
                    }
                }
                else
                {
                    // Add a blank page
                    for( int pageLine = 0; pageLine < ItemPrintout.LINES_PER_PAGE; pageLine++ )
                    {
                        text[line] = "";
                        colours[line] = "";
                        line++;
                    }
                }
            }

            String title = null;
            if( printouts[0].getItem() instanceof ItemPrintout )
            {
                title = ItemPrintout.getTitle( printouts[0] );
            }

            if( leatherFound )
            {
                return ItemPrintout.createBookFromTitleAndText( title, text, colours );
            }
            else
            {
                return ItemPrintout.createMultipleFromTitleAndText( title, text, colours );
            }
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final SimpleCraftingRecipeSerializer<?> SERIALIZER = new SimpleCraftingRecipeSerializer( PrintoutRecipe::new );
}
