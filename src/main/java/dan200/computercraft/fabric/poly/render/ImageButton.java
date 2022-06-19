package dan200.computercraft.fabric.poly.render;

import dan200.computercraft.fabric.poly.textures.ButtonTexture;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;

public class ImageButton extends ImageView {
    public ButtonTexture texture;
    private OnClick callback;

    public ImageButton(int x, int y, ButtonTexture image, OnClick callback) {
        super(x, y, image.base());
        this.texture = image;
        this.callback = callback;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY) {
        this.image = this.isIn(mouseX, mouseY) ? this.texture.hover() : this.texture.base();
        super.render(canvas, tick, mouseX, mouseY);
    }

    @Override
    public void click(int x, int y, ClickType type) {
        this.callback.click(x, y, type);
    }


    public interface OnClick {
        void click(int x, int y, ClickType type);
    }
}
