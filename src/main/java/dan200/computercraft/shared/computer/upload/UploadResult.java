/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import net.minecraft.network.chat.Component;

public enum UploadResult
{
    SUCCESS,
    ERROR,
    CONFIRM_OVERWRITE;

    public static final Component SUCCESS_TITLE = Component.translatable( "gui.computercraft.upload.success" );

    public static final Component FAILED_TITLE = Component.translatable( "gui.computercraft.upload.failed" );
    public static final Component COMPUTER_OFF_MSG = Component.translatable( "gui.computercraft.upload.failed.computer_off" );
    public static final Component OUT_OF_SPACE_MSG = Component.translatable( "gui.computercraft.upload.failed.out_of_space" );
    public static final Component TOO_MUCH_MSG = Component.translatable( "gui.computercraft.upload.failed.too_much" );

    public static final Component UPLOAD_OVERWRITE = Component.translatable( "gui.computercraft.upload.overwrite" );
}
