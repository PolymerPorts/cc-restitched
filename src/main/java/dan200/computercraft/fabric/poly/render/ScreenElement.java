package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.DrawableCanvas;

public abstract class ScreenElement {

    public int x;
    public int y;
    public int zIndex;

    public ScreenElement(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY);

    public abstract int width();
    public abstract int height();

    public boolean isIn(int x, int y) {
        return this.x <= x && this.x + this.width() > x && this.y <= y && this.y + this.height() > y;
    }

    public static boolean isIn(int x, int y, int boxX1, int boxY1, int boxX2, int boxY2) {
        return boxX1 <= x && boxX2 > x && boxY1 <= y && boxY2 > y;
    }

    public void click(int x, int y, ClickType type) { }

    public enum ClickType {
        LEFT_UP,
        LEFT_DOWN,
        RIGHT_UP,
        RIGHT_DOWN;
    }
}
