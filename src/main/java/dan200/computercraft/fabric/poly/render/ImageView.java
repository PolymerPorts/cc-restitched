package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

public class ImageView extends ScreenElement {
    public DrawableCanvas image;
    public boolean isVisible = true;

    public ImageView(int x, int y, DrawableCanvas image) {
        super(x, y);
        this.image = image;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY) {
        if (this.isVisible) {
            CanvasUtils.draw(canvas, this.x, this.y, this.image);
        }
    }

    @Override
    public int width() {
        return this.image.getWidth();
    }

    @Override
    public int height() {
        return this.image.getHeight();
    }
}
