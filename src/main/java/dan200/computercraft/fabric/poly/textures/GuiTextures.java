package dan200.computercraft.fabric.poly.textures;

import eu.pb4.mapcanvas.api.core.CanvasImage;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.nio.file.Files;

public class GuiTextures {
    public static ButtonTexture CLOSE_ICON;
    public static ButtonTexture SHUTDOWN_ICON;
    public static ButtonTexture SHUTDOWN_ACTIVE;
    public static ButtonTexture TERMINATE;

    public static ComputerTexture ADVANCED_COMPUTER;
    public static ComputerTexture COMPUTER;
    public static ComputerTexture COMMAND_COMPUTER;

    public static PrintedPageTexture PRINTED_PAGE;

    static {
        try {
            var texturePath = FabricLoader.getInstance().getModContainer("computercraft").get().getPath("assets/computercraft/textures/");
            {
                var buttons = CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/buttons.png"))));

                SHUTDOWN_ICON = new ButtonTexture(buttons.copy(0, 0, 14, 14), buttons.copy(0, 14, 14, 14));
                SHUTDOWN_ACTIVE = new ButtonTexture(buttons.copy(14, 0, 14, 14), buttons.copy(14, 14, 14, 14));
                TERMINATE = new ButtonTexture(buttons.copy(28, 0, 14, 14), buttons.copy(28, 14, 14, 14));

                var buttons2 = CanvasImage.from(ImageIO.read(Files.newInputStream(texturePath.resolve("gui/poly_buttons.png"))));
                CLOSE_ICON = new ButtonTexture(buttons2.copy(0, 0, 14, 14), buttons2.copy(0, 14, 14, 14));

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
