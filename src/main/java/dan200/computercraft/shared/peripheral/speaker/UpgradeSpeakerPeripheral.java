/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IComputerAccess;
import me.shedaniel.cloth.api.utils.v1.GameInstanceUtils;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;

/**
 * A speaker peripheral which is used on an upgrade, and so is only attached to one computer.
 */
public abstract class UpgradeSpeakerPeripheral extends SpeakerPeripheral
{
    public static final String ADJECTIVE = "upgrade.computercraft.speaker.adjective";

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        super.detach( computer );

        // We could be in the process of shutting down the server, so we can't send packets in this case.
        MinecraftServer server = GameInstanceUtils.getServer();
        if( server == null || server.isStopped() ) return;

        //NetworkHandler.sendToAllPlayers( new SpeakerStopClientMessage( getSource() ) );
    }
}
