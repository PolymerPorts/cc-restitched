package dan200.computercraft.fabric.poly;

import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.server.level.ServerPlayer;

public interface ComputerDisplayAccess {
    ServerComputer getComputer();

    boolean canStayOpen(ServerPlayer player);
}
