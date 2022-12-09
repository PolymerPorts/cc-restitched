package dan200.computercraft.fabric.poly;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.fabric.poly.textures.GuiTextures;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.IdentityHashMap;
import java.util.Map;

public class PolymerSetup {
    public static final Map<Item, PolymerModelData> MODELS = new IdentityHashMap<>();

    public static final GuiElementBuilder FILLER_ITEM = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).setName(Component.empty());
    public static final ResourceLocation GUI_FONT = new ResourceLocation(ComputerCraft.MOD_ID, "gui");

    public static void setup() {
        Fonts.TERMINAL_FONT.hashCode();
        GuiTextures.ADVANCED_COMPUTER.hashCode();

        PolymerResourcePackUtils.addModAssets(ComputerCraft.MOD_ID);

        FILLER_ITEM.setCustomModelData(PolymerResourcePackUtils.requestModel(Items.WHITE_STAINED_GLASS_PANE, new ResourceLocation(ComputerCraft.MOD_ID, "poly_gui/filler")).value());
    }

    public static void requestModel(ResourceLocation identifier, Item item) {
        MODELS.put(item, PolymerResourcePackUtils.requestModel(((PolymerItem) item).getPolymerItem(item.getDefaultInstance(), null), identifier));
    }
}
