package dan200.computercraft.fabric.poly.render;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record CanvasRenderer(DrawableCanvas canvas, List<ScreenElement> elementList) {

    public static CanvasRenderer of(DrawableCanvas canvas) {
        return new CanvasRenderer(canvas, new ArrayList<>());
    }

    public void add(ScreenElement element) {
        this.elementList.add(element);
        this.sort();
    }

    public void remove(ScreenElement element) {
        this.elementList.remove(element);
    }

    public void sort() {
        this.elementList.sort(Comparator.comparing(e -> e.zIndex));
    }

    public void render(long tick, int mouseX, int mouseY) {
        CanvasUtils.clear(this.canvas, CanvasColor.CLEAR_FORCE);
        for (var element : this.elementList) {
            element.render(this.canvas, tick, mouseX, mouseY);
        }
    }

    public void click(int x, int y, ScreenElement.ClickType type) {
        for (var element : elementList) {
            if (element.isIn(x, y)) {
                element.click(x - element.x, y - element.y, type);
            }
        }
    }
}
