package dan200.computercraft.fabric.poly.render;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.InputState;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

public final class TerminalView extends ScreenElement {
    public final Terminal terminal;
    public final InputState inputState;

    public TerminalView(int x, int y, Terminal terminal, InputState inputState) {
        super(x, y);
        this.terminal = terminal;
        this.inputState = inputState;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick, int mouseX, int mouseY) {
        CanvasUtils.draw(canvas, this.x, this.y, this.terminal.getRendered(tick));
    }

    @Override
    public int width() {
        return this.terminal.getRenderedWidth();
    }

    @Override
    public int height() {
        return this.terminal.getRenderedHeight();
    }

    @Override
    public void click(int x, int y, ClickType type) {
        int lx = x / 6 + 1;
        int ly = y / 9 + 1;

        if (type == ClickType.RIGHT_DOWN) {
            this.inputState.mouseClick(0, lx, ly);
        } else if (type == ClickType.LEFT_DOWN) {
            this.inputState.mouseClick(1, lx, ly);
        }
    }
}
