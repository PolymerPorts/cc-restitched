package dan200.computercraft.fabric.poly.textures;

import eu.pb4.mapcanvas.api.core.CanvasIcon;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public record RepeatingCanvas(DrawableCanvas source, int width, int height) implements DrawableCanvas {

    @Override
    public byte getRaw(int x, int y) {
        return this.source.getRaw(x % this.source.getWidth(), y % this.source.getHeight());
    }

    @Override
    public void setRaw(int x, int y, byte color) {
        this.source.setRaw(x % this.source.getWidth(), y % this.source.getHeight(), color);
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }
}
