package dan200.computercraft.fabric.poly.textures;

import eu.pb4.mapcanvas.api.core.CanvasImage;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.nio.file.Files;

public class GuiTextures {
    public static CanvasImage CLOSE_ICON;
    public static CanvasImage CLOSE_ICON_HOVER;
    public static CanvasImage CLOSE_ICON_ACTIVE;
    public static CanvasImage CLOSE_ICON_ACTIVE_HOVER;
    public static CanvasImage TERMINATE;
    public static CanvasImage TERMINATE_HOVER;

    public static ComputerTexture ADVANCED_COMPUTER;
    public static ComputerTexture COMPUTER;
    public static ComputerTexture COMMAND_COMPUTER;

    public static PrintedPageTexture PRINTED_PAGE;

    static {
        try {
            var texturePath = FabricLoader.getInstance().getModContainer("computercraft").get().getPath("assets/computercraft/textures/");
            {
                var buttons = CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/buttons.png"))));

                CLOSE_ICON = buttons.copy(0, 0, 14, 14);
                CLOSE_ICON_HOVER = buttons.copy(0, 14, 14, 14);
                CLOSE_ICON_ACTIVE = buttons.copy(14, 0, 14, 14);
                CLOSE_ICON_ACTIVE_HOVER = buttons.copy(14, 14, 14, 14);
                TERMINATE = buttons.copy(28, 0, 14, 14);
                TERMINATE_HOVER = buttons.copy(28, 14, 14, 14);

                ADVANCED_COMPUTER = ComputerTexture.from(CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/corners_advanced.png")))));
                COMPUTER = ComputerTexture.from(CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/corners_normal.png")))));
                COMMAND_COMPUTER = ComputerTexture.from(CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/corners_command.png")))));

                PRINTED_PAGE = PrintedPageTexture.from(CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/printout.png")))));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
