/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class ImpostorRecipe extends ShapedRecipe implements PolymerRecipe
{
    private final String group;

    private ImpostorRecipe( @Nonnull ResourceLocation id, @Nonnull String group, int width, int height, NonNullList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( id, group, CraftingBookCategory.MISC, width, height, ingredients, result );
        this.group = group;
    }

    @Nonnull
    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public boolean matches( @Nonnull CraftingContainer inv, @Nonnull Level world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingContainer inventory, RegistryAccess registryAccess )
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final RecipeSerializer<ImpostorRecipe> SERIALIZER = new RecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe fromJson( @Nonnull ResourceLocation identifier, @Nonnull JsonObject json )
        {
            String group = GsonHelper.getAsString( json, "group", "" );
            ShapedRecipe recipe = RecipeSerializer.SHAPED_RECIPE.fromJson( identifier, json );
            JsonObject resultObject = GsonHelper.getAsJsonObject( json, "result" );
            ItemStack itemStack = ShapedRecipe.itemStackFromJson( resultObject );
            RecipeUtil.setNbt( itemStack, resultObject );
            return new ImpostorRecipe( identifier, group, recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), itemStack );
        }

        @Override
        public ImpostorRecipe fromNetwork( @Nonnull ResourceLocation identifier, @Nonnull FriendlyByteBuf buf )
        {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf( Short.MAX_VALUE );
            NonNullList<Ingredient> items = NonNullList.withSize( width * height, Ingredient.EMPTY );
            for( int k = 0; k < items.size(); ++k ) items.set( k, Ingredient.fromNetwork( buf ) );
            ItemStack result = buf.readItem();
            return new ImpostorRecipe( identifier, group, width, height, items, result );
        }

        @Override
        public void toNetwork( @Nonnull FriendlyByteBuf buf, @Nonnull ImpostorRecipe recipe )
        {
            buf.writeVarInt( recipe.getWidth() );
            buf.writeVarInt( recipe.getHeight() );
            buf.writeUtf( recipe.getGroup() );
            for( Ingredient ingredient : recipe.getIngredients() ) ingredient.toNetwork( buf );
            buf.writeItem( recipe.getResultItem(null) );
        }
    };
}
