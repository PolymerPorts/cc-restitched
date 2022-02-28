/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.fabric.events.CustomServerEvents;
import dan200.computercraft.shared.network.client.TerminalState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Queue;

public final class MonitorWatcher
{
    private static final Queue<TileMonitor> watching = new ArrayDeque<>();
    private static final Queue<PlayerUpdate> playerUpdates = new ArrayDeque<>();

    private MonitorWatcher()
    {
    }

    public static void init()
    {
        ServerTickEvents.END_SERVER_TICK.register( MonitorWatcher::onTick );
        CustomServerEvents.SERVER_PLAYER_LOADED_CHUNK_EVENT.register( MonitorWatcher::onWatch );
    }

    static void enqueue( TileMonitor monitor )
    {
        if( monitor.enqueued ) return;

        monitor.enqueued = true;
        monitor.cached = null;
        watching.add( monitor );
    }

    public static void onWatch( ServerPlayer serverPlayer, ChunkPos chunkPos )
    {
        LevelChunk chunk = (LevelChunk) serverPlayer.getLevel().getChunk( chunkPos.x, chunkPos.z, ChunkStatus.FULL, false );
        if( chunk == null ) return;

        for( BlockEntity te : chunk.getBlockEntities().values() )
        {
            // Find all origin monitors who are not already on the queue.
            if( !(te instanceof TileMonitor monitor) ) continue;

            ServerMonitor serverMonitor = getMonitor( monitor );
            if( serverMonitor == null || monitor.enqueued ) continue;

            // The chunk hasn't been sent to the client yet, so we can't send an update. Do it on tick end.
            playerUpdates.add( new PlayerUpdate( serverPlayer, monitor ) );
        }
    }

    public static void onTick( MinecraftServer server )
    {
        PlayerUpdate playerUpdate;
        while( (playerUpdate = playerUpdates.poll()) != null )
        {
            TileMonitor tile = playerUpdate.monitor;
            if( tile.enqueued || tile.isRemoved() ) continue;

            ServerMonitor monitor = getMonitor( tile );
            if( monitor == null ) continue;

            // Some basic sanity checks to the player. It's possible they're no longer within range, but that's harder
            // to track efficiently.
            ServerPlayer player = playerUpdate.player;
            if( !player.isAlive() || player.getLevel() != tile.getLevel() ) continue;

            //NetworkHandler.sendToPlayer( playerUpdate.player, new MonitorClientMessage( tile.getBlockPos(), getState( tile, monitor ) ) );
        }

        long limit = ComputerCraft.monitorBandwidth;
        boolean obeyLimit = limit > 0;

        TileMonitor tile;
        while( (!obeyLimit || limit > 0) && (tile = watching.poll()) != null )
        {
            tile.enqueued = false;
            ServerMonitor monitor = getMonitor( tile );
            if( monitor == null ) continue;

            BlockPos pos = tile.getBlockPos();
            Level world = tile.getLevel();
            if( !(world instanceof ServerLevel) ) continue;

            LevelChunk chunk = world.getChunkAt( pos );
            if( ((ServerLevel) world).getChunkSource().chunkMap.getPlayers( chunk.getPos(), false ).isEmpty() )
            {
                continue;
            }

            TerminalState state = getState( tile, monitor );
            //NetworkHandler.sendToAllTracking( new MonitorClientMessage( pos, state ), chunk );

            limit -= state.size();
        }
    }

    private static ServerMonitor getMonitor( TileMonitor monitor )
    {
        return !monitor.isRemoved() && monitor.getXIndex() == 0 && monitor.getYIndex() == 0 ? monitor.getCachedServerMonitor() : null;
    }

    private static TerminalState getState( TileMonitor tile, ServerMonitor monitor )
    {
        TerminalState state = tile.cached;
        if( state == null ) state = tile.cached = monitor.write();
        return state;
    }

    private static final class PlayerUpdate
    {
        final ServerPlayer player;
        final TileMonitor monitor;

        private PlayerUpdate( ServerPlayer player, TileMonitor monitor )
        {
            this.player = player;
            this.monitor = monitor;
        }
    }
}
