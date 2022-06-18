package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

public class TextButton extends ScreenElement {
    private final int height;
    private OnClick callback;
    private int clickTick = -1;

    public TextButton(int x, int y, int width, int height, String text, CanvasFont font, int size, CanvasColor color, OnClick callback) {
        super(x, y);
        this.height = height;
        this.width = width;
        this.font = font;
        this.size = size;
        this.text = text;
        this.color = color;
        this.callback = callback;
    }

    public CanvasFont font;
    public int width;
    public int size;
    public String text;
    public CanvasColor color;

    @Override
    public void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY) {
        var hover = ScreenElement.isIn(mouseX, mouseY, this.x, this.y, this.x + this.width, this.y + this.height);

        var isHeld = this.clickTick + 10 > tick;

        var color = isHeld
            ? CanvasColor.GRAY_HIGH
            : hover
            ? CanvasColor.WHITE_GRAY_NORMAL : CanvasColor.WHITE_GRAY_HIGH;

        var color2 = isHeld
            ? CanvasColor.GRAY_LOW
            : hover ? CanvasColor.WHITE_GRAY_LOW : CanvasColor.WHITE_GRAY_LOW;

        var a = isHeld ? 1 : 0;

        if (!isHeld) {
            CanvasUtils.fill(canvas, this.x, this.y, this.x + this.width, this.y + this.height, color2);
        }
        CanvasUtils.fill(canvas, this.x + a, this.y + a, this.x + width + a - 1, this.y + this.height + a - 1, color);

        this.font.drawText(canvas, text, this.x + (this.width - this.font.getTextWidth(this.text, this.size)) / 2, this.y + (this.height - this.size) / 2, this.size, this.color);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public void click(int x, int y, ClickType type) {
        this.callback.click(x, y, type);
    }

    public interface OnClick {
        void click(int x, int y, ClickType type);
    }
}
