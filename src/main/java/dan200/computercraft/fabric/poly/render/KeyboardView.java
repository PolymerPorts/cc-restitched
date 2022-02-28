package dan200.computercraft.fabric.poly.render;

import com.google.common.base.Supplier;
import dan200.computercraft.fabric.poly.ComputerGui;
import dan200.computercraft.fabric.poly.Keys;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;

public class KeyboardView extends ScreenElement {
    private static final Key[][] KEYS = new Key[][] {
        new Key[] { k("ESC", Keys.ESCAPE), e(15), k("F1", Keys.F1), k("F2", Keys.F2), k("F3", Keys.F3), k("F4", Keys.F4), e(10), k("F5", Keys.F5), k("F6", Keys.F6), k("F7", Keys.F7), k("F8", Keys.F8), e(10), k("F9", Keys.F9), k("F10", Keys.F10), k("F11", Keys.F11), k("F12", Keys.F12),  },
        new Key[] { k("~\n`", Keys.GRAVE_ACCENT), k("!\n1", Keys.NUM_1), k("@\n2", Keys.NUM_2), k("#\n3", Keys.NUM_3), k("$\n4", Keys.NUM_4), k("%\n5", Keys.NUM_5), k("^\n6", Keys.NUM_6), k("&\n7", Keys.NUM_7), k("*\n8", Keys.NUM_8), k("(\n9", Keys.NUM_9), k(")\n0", Keys.NUM_0), k("_\n-", Keys.MINUS), k("+\n=", Keys.EQUAL), k("<--", Keys.BACKSPACE, 44) },
        new Key[] { k("Tab", Keys.TAB, 32), k("Q", Keys.Q), k("W", Keys.W), k("E", Keys.E), k("R", Keys.R), k("T", Keys.T), k("Y", Keys.Y), k("U", Keys.U), k("I", Keys.I), k("O", Keys.O), k("P", Keys.P), k("{\n[", Keys.LEFT_BRACKET), k("}\n]", Keys.RIGHT_BRACKET), k("Enter", Keys.ENTER, 32),  },
        new Key[] { k("Caps", Keys.CAPS_LOCK, 38), k("A", Keys.A), k("S", Keys.S), k("D", Keys.D), k("F", Keys.F), k("G", Keys.G), k("H", Keys.H), k("J", Keys.J), k("K", Keys.K), k("L", Keys.L), k(":\n;", Keys.SEMICOLON), k("\"\n'", Keys.APOSTROPHE), k("|\n\\", Keys.BACKSLASH), k("", Keys.ENTER, 26) },
        new Key[] { k("Shift", Keys.LEFT_SHIFT, 54), k("Z", Keys.Z), k("X", Keys.X), k("C", Keys.C), k("V", Keys.V), k("B", Keys.B), k("N", Keys.N), k("M", Keys.M), k("<\n,", Keys.COMMA), k(">\n.", Keys.PERIOD), k("?\n/", Keys.SLASH), k("Shift", Keys.RIGHT_SHIFT, 54) },
        new Key[] { k("Ctrl", Keys.LEFT_CONTROL, 35), k("⛏", Keys.MENU, 30), k("Alt", Keys.LEFT_ALT, 30), k(" ", Keys.SPACE, 24 * 6), k("Alt", Keys.RIGHT_ALT, 30), k("Ctrl", Keys.RIGHT_CONTROL, 36) }
    };

    public static final int KEYBOARD_WIDTH = ((Supplier<Integer>)() -> {
        int longest = 0;

        for (var keyLine : KEYS) {
            int length = 0;
            for (var key : keyLine) {
                length += key.width() + 2;
            }

            longest = Math.max(longest, length);
        }

        return longest;
    }).get();

    public static final int[] LINE_WIDTH = ((Supplier<int[]>)() -> {
        var array = new int[KEYS.length];

        for (int i = 0; i < KEYS.length; i++) {
            int length = 0;
            for (var key : KEYS[i]) {
                length += key.width() + 2;
            }

            array[i] = length;
        }

        return array;
    }).get();

    public static final int[] KEY_SPACING = ((Supplier<int[]>)() -> {
        var array = new int[KEYS.length];

        for (int i = 0; i < KEYS.length; i++) {
            int length = 0;
            for (var key : KEYS[i]) {
                length += key.width();
            }

            array[i] = (KEYBOARD_WIDTH - length) / KEYS[i].length;
        }

        return array;
    }).get();

    private final ComputerGui gui;

    public KeyboardView(int x, int y, ComputerGui gui) {
        super(x, y);
        this.gui = gui;
    }

    @Override
    public void render(DrawableCanvas canvas, long tick) {
        int y = 0;
        for (int l = 0; l < KEYS.length; l++) {
            int x = (KEYBOARD_WIDTH - LINE_WIDTH[l]) / 2;
            for (var key : KEYS[l]) {
                if (key.key() != -1) {
                    var isHeld = this.gui.input.isKeyDown(key.key());

                    if (key.key() == Keys.ENTER && l == 3) {
                        CanvasUtils.fill(canvas, this.x + x, this.y + y * 16 - 3, this.x + x + key.width(), this.y + y * 16 + 14, isHeld ? CanvasColor.GRAY_HIGH : CanvasColor.WHITE_GRAY_HIGH);
                    } else {
                        CanvasUtils.fill(canvas, this.x + x, this.y + y * 16, this.x + x + key.width(), this.y + y * 16 + 14, isHeld ? CanvasColor.GRAY_HIGH : CanvasColor.WHITE_GRAY_HIGH);
                    }

                    var lines = key.display.split("\n");
                    if (lines.length == 1) {
                        var line = lines[0];
                        var width = DefaultFonts.VANILLA.getTextWidth(line, 8);
                        DefaultFonts.VANILLA.drawText(canvas, line, this.x + x + (key.width() - width) / 2, this.y + y * 16 + 4, 8, CanvasColor.BLACK_HIGH);
                    } else {
                        var merged = String.join("|", lines);

                        var offset = DefaultFonts.VANILLA.getTextWidth(merged, 8);
                        var widthChange = offset / (lines.length / 2);
                        var heightChange = 8 / lines.length + 1;
                        var startHeight = 4 / heightChange;
                        var startWidth = offset / 2;

                        for (int i = 0; i < lines.length; i++) {
                            var line = lines[i];
                            var width = DefaultFonts.VANILLA.getTextWidth(line, 8);
                            DefaultFonts.VANILLA.drawText(canvas, line, this.x + x + (key.width() - width + startWidth - widthChange * i) / 2, this.y + y * 16 + i * heightChange + startHeight + 1, 8, CanvasColor.BLACK_HIGH);
                        }
                    }
                }

                x += (key.width() + 2);
            }
            y++;
        }
    }

    @Override
    public void click(int x, int y, ClickType type) {
        var height = KEYS.length;
        for (int ly = 0; ly < height; ly++) {
            var lys = ly * 16;
            if (lys <= y && lys + 14 > y) {
                int lxs = (KEYBOARD_WIDTH - LINE_WIDTH[ly]) / 2;
                for (var key : KEYS[ly]) {
                    if (lxs <= x && lxs + key.width() > x) {
                        var id = key.key();
                        if (this.gui.input.isKeyDown(id)) {
                            if (type == ClickType.LEFT_DOWN) {
                                this.gui.input.keyUp(id);
                            }
                        } else {
                            this.gui.input.keyDown(id, type == ClickType.RIGHT_DOWN);

                            var character = this.gui.input.isKeyDown(Keys.LEFT_SHIFT) ? id : Character.toLowerCase(id);
                            if (character >= 32 && character <= 126 || character >= 160 && character <= 255) {
                                this.gui.input.queueEvent("char", new Object[]{Character.toString(character), false});
                            }

                            if (type == ClickType.LEFT_DOWN) {
                                this.gui.keysToReleaseNextTick.add(key.key());
                            }
                       }


                        return;
                    }

                    lxs += (key.width() + 2);
                }
                return;
            }
        }
    }

    @Override
    public int width() {
        return KEYBOARD_WIDTH;
    }

    @Override
    public int height() {
        return KEYS.length * 18;
    }


    static private Key k(String display, int key, int width) {
        return new Key(display, key, width);
    }
    static private Key k(String display, int key) {
        return new Key(display, key, 20);
    }
    static private Key e() {
        return new Key("", -1, 20);
    }
    static private Key e(int width) {
        return new Key("", -1, width);
    }

    private record Key(String display, int key, int width) {}
}
