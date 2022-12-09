/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.filesystem.ResourceMount;
import dan200.computercraft.core.tracking.ComputerMBean;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.peripheral.monitor.MonitorWatcher;
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
import dan200.computercraft.shared.util.Config;
import dan200.computercraft.shared.util.TickScheduler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public final class ComputerCraftProxyCommon
{
    public static void init()
    {
        MonitorWatcher.init();

        registerProviders();
        registerHandlers();

        ArgumentSerializers.register();

        ComputerCraftAPI.registerGenericSource( new InventoryMethods() );
    }

    private static void registerProviders()
    {
        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            BlockEntity tile = world.getBlockEntity( pos );
            return tile instanceof IPeripheralTile ? ((IPeripheralTile) tile).getPeripheral( side ) : null;
        } );

        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            BlockEntity tile = world.getBlockEntity( pos );
            return ComputerCraft.enableCommandBlock && tile instanceof CommandBlockEntity ?
                new CommandBlockPeripheral( (CommandBlockEntity) tile ) : null;
        } );

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia )
            {
                return (IMedia) item;
            }
            if( item instanceof RecordItem )
            {
                return RecordMedia.INSTANCE;
            }
            return null;
        } );
    }

    private static void registerHandlers()
    {
        CommandRegistrationCallback.EVENT.register( CommandComputerCraft::register );

        ServerTickEvents.START_SERVER_TICK.register( server -> {
            MainThread.executePendingTasks();
            ComputerCraft.serverComputerRegistry.update();
            TickScheduler.tick();
        } );

        ServerLifecycleEvents.SERVER_STARTING.register( server -> {
            if( server instanceof DedicatedServer dediServer && dediServer.getProperties().enableJmxMonitoring )
            {
                ComputerMBean.register();
            }
            resetState();
            ComputerMBean.registerTracker();
        } );

        ServerLifecycleEvents.SERVER_STOPPED.register( server -> {
            ComputerCraft.serverComputerRegistry.reset();
            resetState();
        } );

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register( ( blockEntity, world ) -> {
            if( blockEntity instanceof TileGeneric )
            {
                ((TileGeneric) blockEntity).onChunkUnloaded();
            }
        } );

        PlayerBlockBreakEvents.BEFORE.register( ( world, player, pos, state, blockEntity ) -> {
            if( state.getBlock() instanceof BlockCable blockCable )
            {
                return blockCable.removedByPlayer( state, world, pos, player, false, null );
            }
            return true;
        } );

        // Config
        ServerLifecycleEvents.SERVER_STARTING.register( Config::serverStarting );
        ServerLifecycleEvents.SERVER_STOPPING.register( Config::serverStopping );

        TurtleEvent.EVENT_BUS.register( FurnaceRefuelHandler.INSTANCE );
        TurtleEvent.EVENT_BUS.register( new TurtlePermissions() );

        ResourceManagerHelper.get( PackType.SERVER_DATA ).registerReloadListener( ResourceMount.RELOAD_LISTENER );
    }

    private static void resetState()
    {
        ComputerCraft.serverComputerRegistry.reset();
        MainThread.reset();
        WirelessNetwork.resetNetworks();
        Tracking.reset();
        NetworkUtils.reset();
    }

    public static void registerLoot()
    {
        registerCondition( "block_named", BlockNamedEntityLootCondition.TYPE );
        registerCondition( "player_creative", PlayerCreativeLootCondition.TYPE );
        registerCondition( "has_id", HasComputerIdLootCondition.TYPE );
    }

    private static void registerCondition( String name, LootItemConditionType serializer )
    {
        Registry.register( BuiltInRegistries.LOOT_CONDITION_TYPE, new ResourceLocation( ComputerCraft.MOD_ID, name ), serializer );
    }
}
