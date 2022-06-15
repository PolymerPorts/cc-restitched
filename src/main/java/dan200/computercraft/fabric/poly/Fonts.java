package dan200.computercraft.fabric.poly;

import eu.pb4.mapcanvas.api.font.BitmapFontBuilder;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.polymer.api.utils.PolymerUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.nio.file.Files;

public class Fonts {
    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;

    public static final CanvasFont TERMINAL_FONT;
    public static final CanvasFont TERMINAL_BACKGROUND_FONT;

    static {
        {
            CanvasFont font;
            CanvasFont fontBack;

            var texturePath = FabricLoader.getInstance().getModContainer("computercraft").get().getPath("assets/computercraft/textures/");
            var emptyGlyph = BitmapFontBuilder.Glyph.of(1, 1);
            try {
                var builder = BitmapFontBuilder.create();
                var builderBack = BitmapFontBuilder.create();

                var image = ImageIO.read(Files.newInputStream(texturePath.resolve("gui/term_font.png")));

                for (int i = 0; i < 256; i++) {
                    int column = i % 16;
                    int row = i / 16;

                    int xStart = 0 + column * (FONT_WIDTH + 2);
                    int yStart = 0 + row * (FONT_HEIGHT + 2);

                    var glyph = BitmapFontBuilder.Glyph.of(FONT_WIDTH, FONT_HEIGHT).logicalHeight(FONT_HEIGHT).charWidth(FONT_WIDTH);
                    var glyphBack = BitmapFontBuilder.Glyph.of(FONT_WIDTH, FONT_HEIGHT).logicalHeight(FONT_HEIGHT).charWidth(FONT_WIDTH);

                    for (int x = 0; x < FONT_WIDTH; x++) {
                        for (int y = 0; y < FONT_HEIGHT; y++) {
                            if (image.getRGB(xStart + x, yStart + y) != 0) {
                                glyph.set(x, y);
                            }
                            if (image.getRGB(xStart + x + 128, yStart + y) != 0) {
                                glyphBack.set(x, y);
                            }
                        }
                    }
                    builder.put(i, glyph);
                    builderBack.put(i, glyphBack);
                }

                font = builder.defaultGlyph(emptyGlyph).build();
                fontBack = builderBack.defaultGlyph(emptyGlyph).build();
            } catch (Throwable e) {
                e.printStackTrace();
                font = DefaultFonts.VANILLA;
                fontBack = BitmapFontBuilder.create().defaultGlyph(emptyGlyph).build();
            }

            TERMINAL_FONT = font;
            TERMINAL_BACKGROUND_FONT = fontBack;
        }
    }
}
