package dan200.computercraft.fabric.poly.textures;

import eu.pb4.mapcanvas.api.core.CanvasImage;

public record PrintedPageTexture(
    CanvasImage centerPage,
    CanvasImage leftPageSideNoBorder,
    CanvasImage leftPageSide,
    CanvasImage rightPageSideNoBorder,
    CanvasImage rightPageSide,
    CanvasImage pageConnection,
    CanvasImage leatherTop,
    CanvasImage leatherBottom,
    CanvasImage leatherLeft,
    CanvasImage leatherRight
) {


    public static PrintedPageTexture from(CanvasImage image) {
        return new PrintedPageTexture(
            image.copy(24, 0, 172, 209),
            image.copy(12, 0, 12, 209),
            image.copy(0, 0, 12, 209),
            image.copy(196 , 0, 12, 209),
            image.copy(196 + 12, 0, 12, 209),
            image.copy(244, 0, 12, 209),
            image.copy(0, 209, 220, 12),
            image.copy(0, 221, 220, 12),
            image.copy(220, 0, 12, 225),
            image.copy(232, 0, 12, 225)
            );
    }
}
