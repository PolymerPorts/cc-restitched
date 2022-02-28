/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

public enum Colour
{
    BLACK( 0x111111, CanvasColor.BLACK_LOW ),
    RED( 0xcc4c4c, CanvasColor.DULL_RED_HIGH ),
    GREEN( 0x57A64E, CanvasColor.PALE_GREEN_NORMAL ),
    BROWN( 0x7f664c, CanvasColor.DIRT_BROWN_NORMAL ),
    BLUE( 0x3366cc, CanvasColor.LAPIS_BLUE_NORMAL ),
    PURPLE( 0xb266e5, CanvasColor.PURPLE_HIGH ),
    CYAN( 0x4c99b2, CanvasColor.DIAMOND_BLUE_LOW ),
    LIGHT_GREY( 0x999999, CanvasColor.LIGHT_GRAY_HIGH ),
    GREY( 0x4c4c4c, CanvasColor.STONE_GRAY_HIGH ),
    PINK( 0xf2b2cc, CanvasColor.PINK_HIGH ),
    LIME( 0x7fcc19, CanvasColor.LIME_HIGH ),
    YELLOW( 0xdede6c, CanvasColor.YELLOW_HIGH ),
    LIGHT_BLUE( 0x99b2f2, CanvasColor.PALE_PURPLE_HIGH ),
    MAGENTA( 0xe57fd8, CanvasColor.MAGENTA_HIGH ),
    ORANGE( 0xf2b233, CanvasColor.ORANGE_HIGH ),
    WHITE( 0xf0f0f0, CanvasColor.OFF_WHITE_HIGH );

    public static final Colour[] VALUES = values();

    public static Colour fromInt( int colour )
    {
        return colour >= 0 && colour < 16 ? Colour.VALUES[colour] : null;
    }

    public static Colour fromHex( int colour )
    {
        for( Colour entry : VALUES )
        {
            if( entry.getHex() == colour ) return entry;
        }

        return null;
    }

    private final int hex;
    private final float[] rgb;
    public final CanvasColor canvasColor;

    Colour( int hex, CanvasColor color )
    {
        this.hex = hex;
        this.canvasColor = color;
        rgb = new float[] {
            ((hex >> 16) & 0xFF) / 255.0f,
            ((hex >> 8) & 0xFF) / 255.0f,
            (hex & 0xFF) / 255.0f,
        };
    }

    public Colour getNext()
    {
        return VALUES[(ordinal() + 1) % 16];
    }

    public Colour getPrevious()
    {
        return VALUES[(ordinal() + 15) % 16];
    }

    public int getHex()
    {
        return hex;
    }

    public float[] getRGB()
    {
        return rgb;
    }

    public float getR()
    {
        return rgb[0];
    }

    public float getG()
    {
        return rgb[1];
    }

    public float getB()
    {
        return rgb[2];
    }
}
