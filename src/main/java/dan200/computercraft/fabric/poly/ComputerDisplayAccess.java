package dan200.computercraft.fabric.poly;

import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface ComputerDisplayAccess {
    ServerComputer getComputer();

    @Nullable
    TileComputerBase getBlockEntity();

    boolean canStayOpen(ServerPlayer player);

    interface Getter {
        ComputerDisplayAccess getDisplayAccess();
    }
}
