/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.fabric.poly.ComputerDisplayAccess;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A proxy object for computer objects, delegating to {@link IComputer} or {@link TileComputer} where appropriate.
 */
public final class ComputerProxy implements ComputerDisplayAccess
{
    private final Supplier<TileComputerBase> get;

    public ComputerProxy( Supplier<TileComputerBase> get )
    {
        this.get = get;
    }

    TileComputerBase getTile()
    {
        return get.get();
    }

    public void turnOn()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = true;
        }
        else
        {
            computer.turnOn();
        }
    }

    public void shutdown()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = false;
        }
        else
        {
            computer.shutdown();
        }
    }

    public void reboot()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        if( computer == null )
        {
            tile.startOn = true;
        }
        else
        {
            computer.reboot();
        }
    }

    public int assignID()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getComputerID() : computer.getID();
    }

    public boolean isOn()
    {
        ServerComputer computer = getTile().getServerComputer();
        return computer != null && computer.isOn();
    }

    public String getLabel()
    {
        TileComputerBase tile = getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getLabel() : computer.getLabel();
    }

    @Override
    public ServerComputer getComputer() {
        return this.get.get().getServerComputer();
    }

    @Override
    public @Nullable TileComputerBase getBlockEntity() {
        return this.get.get();
    }

    @Override
    public boolean canStayOpen(ServerPlayer player) {
        return this.get.get().getBlockPos().distSqr(player.getOnPos()) <= 64;
    }
}
