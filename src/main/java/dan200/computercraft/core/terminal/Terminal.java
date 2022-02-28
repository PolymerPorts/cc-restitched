/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import dan200.computercraft.fabric.poly.Fonts;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

public class Terminal {
    private static final String base16 = "0123456789abcdef";

    private int cursorX = 0;
    private int cursorY = 0;
    private boolean cursorBlink = false;
    private int cursorColour = 0;
    private int cursorBackgroundColour = 15;

    private int width;
    private int height;

    private TextBuffer[] text;
    private TextBuffer[] textColour;
    private TextBuffer[] backgroundColour;

    private final Palette palette = new Palette();

    private CanvasImage canvasImage;
    private long lastCanvasUpdate = -1;

    private final Runnable onChanged;

    public Terminal(int width, int height) {
        this(width, height, null);
    }

    public Terminal(int width, int height, Runnable changedCallback) {
        this.width = width;
        this.height = height;
        onChanged = changedCallback;

        text = new TextBuffer[height];
        textColour = new TextBuffer[height];
        backgroundColour = new TextBuffer[height];
        for (int i = 0; i < this.height; i++) {
            text[i] = new TextBuffer(' ', this.width);
            textColour[i] = new TextBuffer(base16.charAt(cursorColour), this.width);
            backgroundColour[i] = new TextBuffer(base16.charAt(cursorBackgroundColour), this.width);
        }

        this.canvasImage = new CanvasImage(width * Fonts.FONT_WIDTH, height * Fonts.FONT_HEIGHT);
    }

    public synchronized void reset() {
        cursorColour = 0;
        cursorBackgroundColour = 15;
        cursorX = 0;
        cursorY = 0;
        cursorBlink = false;
        clear();
        setChanged();
        palette.resetColours();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public synchronized void resize(int width, int height) {
        if (width == this.width && height == this.height) {
            return;
        }

        int oldHeight = this.height;
        int oldWidth = this.width;
        TextBuffer[] oldText = text;
        TextBuffer[] oldTextColour = textColour;
        TextBuffer[] oldBackgroundColour = backgroundColour;

        this.width = width;
        this.height = height;

        this.canvasImage = new CanvasImage(width * Fonts.FONT_WIDTH, height * Fonts.FONT_HEIGHT);
        this.lastCanvasUpdate = -1;
        text = new TextBuffer[height];
        textColour = new TextBuffer[height];
        backgroundColour = new TextBuffer[height];
        for (int i = 0; i < this.height; i++) {
            if (i >= oldHeight) {
                text[i] = new TextBuffer(' ', this.width);
                textColour[i] = new TextBuffer(base16.charAt(cursorColour), this.width);
                backgroundColour[i] = new TextBuffer(base16.charAt(cursorBackgroundColour), this.width);
            } else if (this.width == oldWidth) {
                text[i] = oldText[i];
                textColour[i] = oldTextColour[i];
                backgroundColour[i] = oldBackgroundColour[i];
            } else {
                text[i] = new TextBuffer(' ', this.width);
                textColour[i] = new TextBuffer(base16.charAt(cursorColour), this.width);
                backgroundColour[i] = new TextBuffer(base16.charAt(cursorBackgroundColour), this.width);
                text[i].write(oldText[i]);
                textColour[i].write(oldTextColour[i]);
                backgroundColour[i].write(oldBackgroundColour[i]);
            }
        }
        setChanged();
    }

    public void setCursorPos(int x, int y) {
        if (cursorX != x || cursorY != y) {
            cursorX = x;
            cursorY = y;
            setChanged();
        }
    }

    public void setCursorBlink(boolean blink) {
        if (cursorBlink != blink) {
            cursorBlink = blink;
            setChanged();
        }
    }

    public void setTextColour(int colour) {
        if (cursorColour != colour) {
            cursorColour = colour;
            setChanged();
        }
    }

    public void setBackgroundColour(int colour) {
        if (cursorBackgroundColour != colour) {
            cursorBackgroundColour = colour;
            setChanged();
        }
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public boolean getCursorBlink() {
        return cursorBlink;
    }

    public int getTextColour() {
        return cursorColour;
    }

    public int getBackgroundColour() {
        return cursorBackgroundColour;
    }

    @Nonnull
    public Palette getPalette() {
        return palette;
    }

    public synchronized void blit(String text, String textColour, String backgroundColour) {
        int x = cursorX;
        int y = cursorY;
        if (y >= 0 && y < height) {
            this.text[y].write(text, x);
            this.textColour[y].write(textColour, x);
            this.backgroundColour[y].write(backgroundColour, x);
            setChanged();
        }
    }

    public synchronized void write(String text) {
        int x = cursorX;
        int y = cursorY;
        if (y >= 0 && y < height) {
            this.text[y].write(text, x);
            textColour[y].fill(base16.charAt(cursorColour), x, x + text.length());
            backgroundColour[y].fill(base16.charAt(cursorBackgroundColour), x, x + text.length());
            setChanged();
        }
    }

    public synchronized void scroll(int yDiff) {
        if (yDiff != 0) {
            TextBuffer[] newText = new TextBuffer[height];
            TextBuffer[] newTextColour = new TextBuffer[height];
            TextBuffer[] newBackgroundColour = new TextBuffer[height];
            for (int y = 0; y < height; y++) {
                int oldY = y + yDiff;
                if (oldY >= 0 && oldY < height) {
                    newText[y] = text[oldY];
                    newTextColour[y] = textColour[oldY];
                    newBackgroundColour[y] = backgroundColour[oldY];
                } else {
                    newText[y] = new TextBuffer(' ', width);
                    newTextColour[y] = new TextBuffer(base16.charAt(cursorColour), width);
                    newBackgroundColour[y] = new TextBuffer(base16.charAt(cursorBackgroundColour), width);
                }
            }
            text = newText;
            textColour = newTextColour;
            backgroundColour = newBackgroundColour;
            setChanged();
        }
    }

    public synchronized void clear() {
        for (int y = 0; y < height; y++) {
            text[y].fill(' ');
            textColour[y].fill(base16.charAt(cursorColour));
            backgroundColour[y].fill(base16.charAt(cursorBackgroundColour));
        }
        setChanged();
    }

    public synchronized void clearLine() {
        int y = cursorY;
        if (y >= 0 && y < height) {
            text[y].fill(' ');
            textColour[y].fill(base16.charAt(cursorColour));
            backgroundColour[y].fill(base16.charAt(cursorBackgroundColour));
            setChanged();
        }
    }

    public synchronized TextBuffer getLine(int y) {
        if (y >= 0 && y < height) {
            return text[y];
        }
        return null;
    }

    public synchronized void setLine(int y, String text, String textColour, String backgroundColour) {
        this.text[y].write(text);
        this.textColour[y].write(textColour);
        this.backgroundColour[y].write(backgroundColour);
        setChanged();
    }

    public synchronized TextBuffer getTextColourLine(int y) {
        if (y >= 0 && y < height) {
            return textColour[y];
        }
        return null;
    }

    public synchronized TextBuffer getBackgroundColourLine(int y) {
        if (y >= 0 && y < height) {
            return backgroundColour[y];
        }
        return null;
    }

    public final void setChanged() {
        if (onChanged != null) onChanged.run();
    }

    public synchronized void write(FriendlyByteBuf buffer) {
        buffer.writeInt(cursorX);
        buffer.writeInt(cursorY);
        buffer.writeBoolean(cursorBlink);
        buffer.writeByte(cursorBackgroundColour << 4 | cursorColour);

        for (int y = 0; y < height; y++) {
            TextBuffer text = this.text[y];
            TextBuffer textColour = this.textColour[y];
            TextBuffer backColour = backgroundColour[y];

            for (int x = 0; x < width; x++) {
                buffer.writeByte(text.charAt(x) & 0xFF);
                buffer.writeByte(getColour(
                    backColour.charAt(x), Colour.BLACK) << 4 |
                    getColour(textColour.charAt(x), Colour.WHITE)
                );
            }
        }

        palette.write(buffer);
    }

    public synchronized void read(FriendlyByteBuf buffer) {
        cursorX = buffer.readInt();
        cursorY = buffer.readInt();
        cursorBlink = buffer.readBoolean();

        byte cursorColour = buffer.readByte();
        cursorBackgroundColour = (cursorColour >> 4) & 0xF;
        this.cursorColour = cursorColour & 0xF;

        for (int y = 0; y < height; y++) {
            TextBuffer text = this.text[y];
            TextBuffer textColour = this.textColour[y];
            TextBuffer backColour = backgroundColour[y];

            for (int x = 0; x < width; x++) {
                text.setChar(x, (char) (buffer.readByte() & 0xFF));

                byte colour = buffer.readByte();
                backColour.setChar(x, base16.charAt((colour >> 4) & 0xF));
                textColour.setChar(x, base16.charAt(colour & 0xF));
            }
        }

        palette.read(buffer);
        setChanged();
    }

    public synchronized CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putInt("term_cursorX", cursorX);
        nbt.putInt("term_cursorY", cursorY);
        nbt.putBoolean("term_cursorBlink", cursorBlink);
        nbt.putInt("term_textColour", cursorColour);
        nbt.putInt("term_bgColour", cursorBackgroundColour);
        for (int n = 0; n < height; n++) {
            nbt.putString("term_text_" + n, text[n].toString());
            nbt.putString("term_textColour_" + n, textColour[n].toString());
            nbt.putString("term_textBgColour_" + n, backgroundColour[n].toString());
        }

        palette.writeToNBT(nbt);
        return nbt;
    }

    public synchronized void readFromNBT(CompoundTag nbt) {
        cursorX = nbt.getInt("term_cursorX");
        cursorY = nbt.getInt("term_cursorY");
        cursorBlink = nbt.getBoolean("term_cursorBlink");
        cursorColour = nbt.getInt("term_textColour");
        cursorBackgroundColour = nbt.getInt("term_bgColour");

        for (int n = 0; n < height; n++) {
            text[n].fill(' ');
            if (nbt.contains("term_text_" + n)) {
                text[n].write(nbt.getString("term_text_" + n));
            }
            textColour[n].fill(base16.charAt(cursorColour));
            if (nbt.contains("term_textColour_" + n)) {
                textColour[n].write(nbt.getString("term_textColour_" + n));
            }
            backgroundColour[n].fill(base16.charAt(cursorBackgroundColour));
            if (nbt.contains("term_textBgColour_" + n)) {
                backgroundColour[n].write(nbt.getString("term_textBgColour_" + n));
            }
        }

        palette.readFromNBT(nbt);
        setChanged();
    }

    public static int getColour(char c, Colour def) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return 15 - def.ordinal();
    }

    public CanvasImage getRendered(long tick) {
        if (this.lastCanvasUpdate < tick) {
            CanvasUtils.clear(this.canvasImage, CanvasColor.BLACK_LOWEST);
            for (int y = 0; y < this.height; y++) {
                var line = this.getLine(y);
                var bgColor = this.getBackgroundColourLine(y);
                var color = this.getTextColourLine(y);
                for (int x = 0; x < this.width; x++) {
                    CanvasUtils.fill(this.canvasImage, x * Fonts.FONT_WIDTH, y * Fonts.FONT_HEIGHT, x * Fonts.FONT_WIDTH + Fonts.FONT_WIDTH, y * Fonts.FONT_HEIGHT + Fonts.FONT_HEIGHT, Colour.fromInt(15 - getColour(bgColor.charAt(x), Colour.BLACK)).canvasColor);
                    var character = line.charAt(x);

                    int charWidth = Fonts.TERMINAL_FONT.getGlyphWidth(character, 8, 0);

                    Fonts.TERMINAL_FONT.drawGlyph(this.canvasImage, line.charAt(x), x * Fonts.FONT_WIDTH + (Fonts.FONT_WIDTH - charWidth) / 2, y * Fonts.FONT_HEIGHT, 8, 0,
                        Colour.fromInt(15 - Terminal.getColour(color.charAt(x), Colour.BLACK)).canvasColor);
                }
            }

            if (this.cursorBlink && tick % 20 > 10) {
                CanvasUtils.fill(this.canvasImage,
                    this.cursorX * Fonts.FONT_WIDTH, this.cursorY * Fonts.FONT_HEIGHT,
                    this.cursorX * Fonts.FONT_WIDTH + Fonts.FONT_WIDTH, this.cursorY * Fonts.FONT_HEIGHT + Fonts.FONT_HEIGHT,
                    CanvasColor.WHITE_GRAY_HIGH
                );
            }

            this.lastCanvasUpdate = tick;

        }
        return this.canvasImage;
    }

    public int getRenderedHeight() {
        return this.canvasImage.getHeight();
    }

    public int getRenderedWidth() {
        return this.canvasImage.getWidth();
    }
}
