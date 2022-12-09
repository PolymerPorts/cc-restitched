/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class TurtleModem extends AbstractTurtleUpgrade
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private final ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle, boolean advanced )
        {
            super( new ModemState(), advanced );
            this.turtle = turtle;
        }

        @Nonnull
        @Override
        public Level getLevel()
        {
            return turtle.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos turtlePos = turtle.getPosition();
            return new Vec3(
                turtlePos.getX(),
                turtlePos.getY(),
                turtlePos.getZ()
            );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral modem && modem.turtle == turtle);
        }
    }

    @Environment( EnvType.CLIENT )
    private class Models
    {
        private final ModelResourceLocation leftOffModel = advanced ?
            new ModelResourceLocation( "computercraft", "turtle_modem_advanced_off_left", "inventory" ) :
            new ModelResourceLocation( "computercraft", "turtle_modem_normal_off_left", "inventory" );

        private final ModelResourceLocation rightOffModel = advanced ?
            new ModelResourceLocation( "computercraft", "turtle_modem_advanced_off_right", "inventory" ) :
            new ModelResourceLocation( "computercraft", "turtle_modem_normal_off_right", "inventory" );

        private final ModelResourceLocation leftOnModel = advanced ?
            new ModelResourceLocation( "computercraft", "turtle_modem_advanced_on_left", "inventory" ) :
            new ModelResourceLocation( "computercraft", "turtle_modem_normal_on_left", "inventory" );

        private final ModelResourceLocation rightOnModel = advanced ?
            new ModelResourceLocation( "computercraft", "turtle_modem_advanced_on_right", "inventory" ) :
            new ModelResourceLocation( "computercraft", "turtle_modem_normal_on_right", "inventory" );
    }

    private final boolean advanced;

    @Environment( EnvType.CLIENT )
    private Models models;

    public TurtleModem( ResourceLocation id, ItemStack stack, boolean advanced )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, advanced ? WirelessModemPeripheral.ADVANCED_ADJECTIVE : WirelessModemPeripheral.NORMAL_ADJECTIVE, stack );
        this.advanced = advanced;
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new Peripheral( turtle, advanced );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction dir )
    {
        return TurtleCommandResult.failure();
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        // Advance the modem
        if( !turtle.getLevel().isClientSide )
        {
            IPeripheral peripheral = turtle.getPeripheral( side );
            if( peripheral instanceof Peripheral modem )
            {
                ModemState state = modem.getModemState();
                if( state.pollChanged() )
                {
                    turtle.getUpgradeNBTData( side ).putBoolean( "active", state.isOpen() );
                    turtle.updateUpgradeNBTData( side );
                }
            }
        }
    }
}
