/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.ComputerCraft;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.resources.ResourceLocation;

public final class ArgumentSerializers
{
    @SuppressWarnings( "unchecked" )
    private static <T extends ArgumentType<?>> void registerUnsafe( ResourceLocation id, Class type, ArgumentTypeInfo serializer )
    {

    }

    private static <T extends ArgumentType<?>> void register( ResourceLocation id, Class<T> type, ArgumentTypeInfo<?, ?> serializer )
    {

    }

    private static <T extends ArgumentType<?>> void register( ResourceLocation id, T instance )
    {

    }

    public static void register()
    {
        register( new ResourceLocation( ComputerCraft.MOD_ID, "tracking_field" ), TrackingFieldArgumentType.trackingField() );
        register( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ), ComputerArgumentType.oneComputer() );
        register( new ResourceLocation( ComputerCraft.MOD_ID, "computers" ), ComputersArgumentType.class, new ComputersArgumentType.Serializer() );
        registerUnsafe( new ResourceLocation( ComputerCraft.MOD_ID, "repeat" ), RepeatArgumentType.class, new RepeatArgumentType.Serializer() );
    }
}
