package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.CanvasFont;

public class CenteredTextView extends ScreenElement {
    public CanvasFont font;
    public int width;
    public int size;
    public String text;
    public CanvasColor color;

    public CenteredTextView(int x, int y, int width, String text, CanvasFont font, int size, CanvasColor color) {
        super(x, y);
        this.width = width;
        this.font = font;
        this.size = size;
        this.text = text;
        this.color = color;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY) {
        this.font.drawText(canvas, text, this.x + (this.width - this.font.getTextWidth(this.text, this.size)) / 2, this.y + 2, this.size, this.color);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.size + 4;
    }
}
