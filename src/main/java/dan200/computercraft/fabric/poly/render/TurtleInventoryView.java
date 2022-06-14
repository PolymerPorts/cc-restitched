package dan200.computercraft.fabric.poly.render;

import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.DefaultFonts;

public class TurtleInventoryView extends ScreenElement {
    private final TileTurtle turtle;

    public TurtleInventoryView(int x, int y, TileTurtle turtle) {
        super(x, y);
        this.turtle = turtle;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick) {
        DefaultFonts.VANILLA.drawText(canvas, "Inventory:", this.x + 1, this.y, 8, CanvasColor.GRAY_LOWEST);
        DefaultFonts.VANILLA.drawText(canvas, "Inventory:", this.x, this.y, 8, CanvasColor.WHITE_HIGH);
        for (int i = 0; i < this.turtle.getContainerSize(); i++) {
            var item = this.turtle.getItem(i);

            String text;
            CanvasColor canvasColor;

            if (item.isEmpty()) {
                text = "» [Empty]";
                canvasColor = CanvasColor.GRAY_HIGH;
            } else {
                text = "» " + item.getCount() + " × " + item.getItemHolder().unwrapKey().get().location();
                canvasColor = CanvasColor.WHITE_GRAY_HIGH;
            }

            DefaultFonts.VANILLA.drawText(canvas, text, this.x + 5, this.y + i * 9 + 9 + 1, 8, CanvasColor.GRAY_LOWEST);
            DefaultFonts.VANILLA.drawText(canvas, text, this.x + 4, this.y + i * 9 + 9, 8, canvasColor);
        }
    }

    @Override
    public int width() {
        return 256;
    }

    @Override
    public int height() {
        return 512;
    }
}
