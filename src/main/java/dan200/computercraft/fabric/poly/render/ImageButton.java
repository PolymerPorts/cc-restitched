package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.CanvasImage;

public class ImageButton extends ImageView {
    private OnClick callback;

    public ImageButton(int x, int y, CanvasImage image, OnClick callback) {
        super(x, y, image);
        this.callback = callback;
    }

    @Override
    public void click(int x, int y, ClickType type) {
        this.callback.click(x, y, type);
    }


    public interface OnClick {
        void click(int x, int y, ClickType type);
    }
}
