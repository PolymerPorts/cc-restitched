/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.common.collect.Multimap;
import dan200.computercraft.ComputerCraft;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * An alternative to {@link net.minecraft.data.loot.LootTableProvider}, with a more flexible interface.
 */
abstract class LootTableProvider implements DataProvider
{
    private final FabricDataOutput output;

    LootTableProvider( FabricDataOutput output )
    {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run( @Nonnull CachedOutput cache )
    {
        Map<ResourceLocation, LootTable> tables = new HashMap<>();
        ValidationContext validation = new ValidationContext( LootContextParamSets.ALL_PARAMS, x -> null, tables::get );

        registerLoot( ( id, table ) -> {
            if( tables.containsKey( id ) ) validation.reportProblem( "Duplicate loot tables for " + id );
            tables.put( id, table );
        } );

        tables.forEach( ( key, value ) -> LootTables.validate( validation, key, value ) );

        Multimap<String, String> problems = validation.getProblems();
        if( !problems.isEmpty() )
        {
            problems.forEach( ( child, problem ) ->
                ComputerCraft.log.warn( "Found validation problem in " + child + ": " + problem ) );
            throw new IllegalStateException( "Failed to validate loot tables, see logs" );
        }

        tables.forEach( ( key, value ) -> {
            Path path = getPath( key );
            try
            {
                DataProvider.saveStable( cache, LootTables.serialize( value ), path );
            }
            catch( Throwable e )
            {
                ComputerCraft.log.error( "Couldn't save loot table {}", path, e );
            }
        } );

        CompletableFuture<?> future = new CompletableFuture<>();
        future.complete( null );

        return future;
    }

    protected abstract void registerLoot( BiConsumer<ResourceLocation, LootTable> add );

    @Nonnull
    @Override
    public String getName()
    {
        return "LootTables";
    }

    private Path getPath( ResourceLocation id )
    {
        return output.getOutputFolder()
            .resolve( "data" ).resolve( id.getNamespace() ).resolve( "loot_tables" )
            .resolve( id.getPath() + ".json" );
    }
}
