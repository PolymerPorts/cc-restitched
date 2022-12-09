/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.fabric.poly.PolymerAutoTexturedItem;
import dan200.computercraft.fabric.poly.PolymerSetup;
import dan200.computercraft.fabric.poly.textures.HeadTextures;
import dan200.computercraft.fabric.poly.textures.PolymerAutoTexturedBlockItem;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.BlockPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.BlockSpeaker;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import dan200.computercraft.shared.turtle.upgrades.TurtleTool;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerHeadBlockItem;
import dan200.computercraft.shared.util.Colour;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

import java.util.Collection;
import java.util.function.BiFunction;


public final class Registry {
    public static final String MOD_ID = ComputerCraft.MOD_ID;

    public static void init() {
        // Touch each static class to force static initializers to run
        // Maybe there's a better way to do this :/
        Object[] o = {
            ModBlockEntities.CABLE,
            ModBlocks.CABLE,
            ModItems.CABLE,
            ModEntities.TURTLE_PLAYER
        };

        TurtleUpgrades.registerTurtleUpgrades();
        PocketUpgrades.registerPocketUpgrades();

        CauldronInteraction.WATER.put(ModItems.TURTLE_NORMAL, ItemTurtle.CAULDRON_INTERACTION);
        CauldronInteraction.WATER.put(ModItems.TURTLE_ADVANCED, ItemTurtle.CAULDRON_INTERACTION);
    }

    public static final class ModBlocks
    {
        public static <T extends Block> T register( String id, T value )
        {
            return net.minecraft.core.Registry.register( BuiltInRegistries.BLOCK, new ResourceLocation( MOD_ID, id ), value );
        }

        public static final BlockMonitor MONITOR_NORMAL =
            register("monitor_normal", new BlockMonitor(properties(), () -> ModBlockEntities.MONITOR_NORMAL, Blocks.SMOOTH_STONE));

        public static final BlockMonitor MONITOR_ADVANCED =
            register("monitor_advanced", new BlockMonitor(properties(), () -> ModBlockEntities.MONITOR_ADVANCED, Blocks.GOLD_BLOCK));
        public static final BlockComputer<TileComputer> COMPUTER_NORMAL =
            register("computer_normal", new BlockComputer<>(computerProperties(), ComputerFamily.NORMAL, () -> ModBlockEntities.COMPUTER_NORMAL));
        public static final BlockComputer<TileComputer> COMPUTER_ADVANCED =
            register("computer_advanced", new BlockComputer<>(computerProperties(), ComputerFamily.ADVANCED, () -> ModBlockEntities.COMPUTER_ADVANCED));
        public static final BlockComputer<TileCommandComputer> COMPUTER_COMMAND =
            register("computer_command", new BlockComputer<>(computerProperties().strength(-1, 6000000.0F), ComputerFamily.COMMAND, () -> ModBlockEntities.COMPUTER_COMMAND));
        public static final BlockTurtle TURTLE_NORMAL =
            register("turtle_normal", new BlockTurtle(turtleProperties(), ComputerFamily.NORMAL, () -> ModBlockEntities.TURTLE_NORMAL));
        public static final BlockTurtle TURTLE_ADVANCED =
            register("turtle_advanced", new BlockTurtle(turtleProperties(), ComputerFamily.ADVANCED, () -> ModBlockEntities.TURTLE_ADVANCED));
        public static final BlockSpeaker SPEAKER =
            register("speaker", new BlockSpeaker(properties()));
        public static final BlockDiskDrive DISK_DRIVE =
            register("disk_drive", new BlockDiskDrive(properties()));
        public static final BlockPrinter PRINTER =
            register("printer", new BlockPrinter(properties()));
        public static final BlockWirelessModem WIRELESS_MODEM_NORMAL =
            register("wireless_modem_normal", new BlockWirelessModem(properties(), () -> ModBlockEntities.WIRELESS_MODEM_NORMAL, HeadTextures.WIRELESS_MODEM));
        public static final BlockWirelessModem WIRELESS_MODEM_ADVANCED =
            register("wireless_modem_advanced", new BlockWirelessModem(properties(), () -> ModBlockEntities.WIRELESS_MODEM_ADVANCED, HeadTextures.ENDER_MODEM));
        public static final BlockWiredModemFull WIRED_MODEM_FULL =
            register("wired_modem_full", new BlockWiredModemFull(modemProperties()));
        public static final BlockCable CABLE =
            register("cable", new BlockCable(modemProperties()));

        private static BlockBehaviour.Properties properties()
        {
            return BlockBehaviour.Properties.of( Material.STONE ).strength( 2F );
        }

        private static BlockBehaviour.Properties computerProperties()
        {
            return properties().noOcclusion()
                .isRedstoneConductor( ( BlockState state, BlockGetter getter, BlockPos pos ) -> false );
        }

        private static BlockBehaviour.Properties turtleProperties()
        {
            return BlockBehaviour.Properties.of( Material.STONE ).strength( 2.5f );
        }

        private static BlockBehaviour.Properties modemProperties() {
            return BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.5f);
        }

    }

    public static class ModBlockEntities
    {
        private static <T extends BlockEntity> BlockEntityType<T> ofBlock( Block block, String id, BiFunction<BlockPos, BlockState, T> factory )
        {
            BlockEntityType<T> blockEntityType = FabricBlockEntityTypeBuilder.create( factory::apply, block ).build();
            net.minecraft.core.Registry.register( BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation( MOD_ID, id ), blockEntityType );
            PolymerBlockUtils.registerBlockEntity(blockEntityType);
            return blockEntityType;
        }

        public static final BlockEntityType<TileMonitor> MONITOR_NORMAL =
            ofBlock(ModBlocks.MONITOR_NORMAL, "monitor_normal", (blockPos, blockState) -> new TileMonitor(ModBlockEntities.MONITOR_NORMAL, blockPos, blockState, false));

        public static final BlockEntityType<TileMonitor> MONITOR_ADVANCED =
            ofBlock(ModBlocks.MONITOR_ADVANCED, "monitor_advanced", (blockPos, blockState) -> new TileMonitor(ModBlockEntities.MONITOR_ADVANCED, blockPos, blockState, true));

        public static final BlockEntityType<TileComputer> COMPUTER_NORMAL =
            ofBlock(ModBlocks.COMPUTER_NORMAL, "computer_normal", (blockPos, blockState) -> new TileComputer(ModBlockEntities.COMPUTER_NORMAL, blockPos, blockState, ComputerFamily.NORMAL));

        public static final BlockEntityType<TileComputer> COMPUTER_ADVANCED =
            ofBlock(ModBlocks.COMPUTER_ADVANCED, "computer_advanced", (blockPos, blockState) -> new TileComputer(ModBlockEntities.COMPUTER_ADVANCED, blockPos, blockState, ComputerFamily.ADVANCED));

        public static final BlockEntityType<TileCommandComputer> COMPUTER_COMMAND =
            ofBlock(ModBlocks.COMPUTER_COMMAND, "computer_command", (blockPos, blockState) -> new TileCommandComputer(ModBlockEntities.COMPUTER_COMMAND, blockPos, blockState));

        public static final BlockEntityType<TileTurtle> TURTLE_NORMAL =
            ofBlock(ModBlocks.TURTLE_NORMAL, "turtle_normal", (blockPos, blockState) -> new TileTurtle(ModBlockEntities.TURTLE_NORMAL, blockPos, blockState, ComputerFamily.NORMAL));

        public static final BlockEntityType<TileTurtle> TURTLE_ADVANCED =
            ofBlock(ModBlocks.TURTLE_ADVANCED, "turtle_advanced", (blockPos, blockState) -> new TileTurtle(ModBlockEntities.TURTLE_ADVANCED, blockPos, blockState, ComputerFamily.ADVANCED));

        public static final BlockEntityType<TileSpeaker> SPEAKER =
            ofBlock(ModBlocks.SPEAKER, "speaker", (blockPos, blockState) -> new TileSpeaker(ModBlockEntities.SPEAKER, blockPos, blockState));

        public static final BlockEntityType<TileDiskDrive> DISK_DRIVE =
            ofBlock(ModBlocks.DISK_DRIVE, "disk_drive", (blockPos, blockState) -> new TileDiskDrive(ModBlockEntities.DISK_DRIVE, blockPos, blockState));

        public static final BlockEntityType<TilePrinter> PRINTER =
            ofBlock(ModBlocks.PRINTER, "printer", (blockPos, blockState) -> new TilePrinter(ModBlockEntities.PRINTER, blockPos, blockState));

        public static final BlockEntityType<TileWiredModemFull> WIRED_MODEM_FULL =
            ofBlock(ModBlocks.WIRED_MODEM_FULL, "wired_modem_full", (blockPos, blockState) -> new TileWiredModemFull(ModBlockEntities.WIRED_MODEM_FULL, blockPos, blockState));

        public static final BlockEntityType<TileCable> CABLE =
            ofBlock(ModBlocks.CABLE, "cable", (blockPos, blockState) -> new TileCable(ModBlockEntities.CABLE, blockPos, blockState));

        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_NORMAL =
            ofBlock(ModBlocks.WIRELESS_MODEM_NORMAL, "wireless_modem_normal", (blockPos, blockState) -> new TileWirelessModem(ModBlockEntities.WIRELESS_MODEM_NORMAL, blockPos, blockState, false));

        public static final BlockEntityType<TileWirelessModem> WIRELESS_MODEM_ADVANCED =
            ofBlock(ModBlocks.WIRELESS_MODEM_ADVANCED, "wireless_modem_advanced", (blockPos, blockState) -> new TileWirelessModem(ModBlockEntities.WIRELESS_MODEM_ADVANCED, blockPos, blockState, true));
    }

    public static final class ModItems
    {
        public static final ItemComputer COMPUTER_NORMAL =
            ofBlock(ModBlocks.COMPUTER_NORMAL, ItemComputer::new);

        public static final ItemComputer COMPUTER_ADVANCED =
            ofBlock(ModBlocks.COMPUTER_ADVANCED, ItemComputer::new);

        public static final ItemComputer COMPUTER_COMMAND =
            ofBlock(ModBlocks.COMPUTER_COMMAND, ItemComputer::new);

        public static final ItemPocketComputer POCKET_COMPUTER_NORMAL =
            register("pocket_computer_normal", new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.NORMAL));

        public static final ItemPocketComputer POCKET_COMPUTER_ADVANCED =
            register("pocket_computer_advanced", new ItemPocketComputer(properties().stacksTo(1), ComputerFamily.ADVANCED));

        public static final ItemTurtle TURTLE_NORMAL =
            ofBlock(ModBlocks.TURTLE_NORMAL, ItemTurtle::new);

        public static final ItemTurtle TURTLE_ADVANCED =
            ofBlock(ModBlocks.TURTLE_ADVANCED, ItemTurtle::new);

        public static final ItemDisk DISK =
            register("disk", new ItemDisk(properties().stacksTo(1)));

        public static final ItemTreasureDisk TREASURE_DISK =
            register("treasure_disk", new ItemTreasureDisk(properties().stacksTo(1)));

        public static final ItemPrintout PRINTED_PAGE =
            register("printed_page", new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGE));

        public static final ItemPrintout PRINTED_PAGES =
            register("printed_pages", new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.PAGES));

        public static final ItemPrintout PRINTED_BOOK =
            register("printed_book", new ItemPrintout(properties().stacksTo(1), ItemPrintout.Type.BOOK));

        public static final BlockItem SPEAKER =
            ofBlock(ModBlocks.SPEAKER, PolymerHeadBlockItem::new);

        public static final BlockItem DISK_DRIVE =
            ofBlock(ModBlocks.DISK_DRIVE, PolymerHeadBlockItem::new);

        public static final BlockItem PRINTER =
            ofBlock(ModBlocks.PRINTER, PolymerHeadBlockItem::new);

        public static final BlockItem MONITOR_NORMAL =
            ofBlock(ModBlocks.MONITOR_NORMAL, (block, properties) -> new PolymerAutoTexturedBlockItem(block, properties, block.getPolymerBlock(block.defaultBlockState()).asItem()));

        public static final BlockItem MONITOR_ADVANCED =
            ofBlock(ModBlocks.MONITOR_ADVANCED, (block, properties) -> new PolymerAutoTexturedBlockItem(block, properties, block.getPolymerBlock(block.defaultBlockState()).asItem()));

        public static final BlockItem WIRELESS_MODEM_NORMAL =
            ofBlock(ModBlocks.WIRELESS_MODEM_NORMAL, PolymerHeadBlockItem::new);

        public static final BlockItem WIRELESS_MODEM_ADVANCED =
            ofBlock(ModBlocks.WIRELESS_MODEM_ADVANCED, PolymerHeadBlockItem::new);

        public static final BlockItem WIRED_MODEM_FULL =
            ofBlock(ModBlocks.WIRED_MODEM_FULL, PolymerHeadBlockItem::new);

        public static final ItemBlockCable.Cable CABLE =
            register("cable", new ItemBlockCable.Cable(ModBlocks.CABLE, properties()));

        public static final ItemBlockCable.WiredModem WIRED_MODEM =
            register("wired_modem", new ItemBlockCable.WiredModem(ModBlocks.CABLE, properties()));

        private static final CreativeModeTab mainItemGroup = PolymerItemGroupUtils.builder( new ResourceLocation( MOD_ID, "main" ) )
            .title(Component.translatable("itemGroup.computercraft.main"))
            .icon( () -> new ItemStack( ModBlocks.COMPUTER_NORMAL ) )
            .displayItems( ( featureFlagSet, output, operator ) -> {
                output.accept( COMPUTER_NORMAL );
                output.accept( COMPUTER_ADVANCED );

                if ( operator )
                {
                    output.accept( COMPUTER_COMMAND );
                }

                output.accept( POCKET_COMPUTER_NORMAL.create( -1, null, -1, null ) );
                dan200.computercraft.shared.PocketUpgrades.getVanillaUpgrades().map( x -> POCKET_COMPUTER_NORMAL.create( -1, null, -1, x ) ).forEach( output::accept );

                output.accept( POCKET_COMPUTER_ADVANCED.create( -1, null, -1, null ) );
                dan200.computercraft.shared.PocketUpgrades.getVanillaUpgrades().map( x -> POCKET_COMPUTER_ADVANCED.create( -1, null, -1, x ) ).forEach( output::accept );


                output.accept( TURTLE_NORMAL.create( -1, null, -1, null, null, 0, null ) );
                dan200.computercraft.shared.TurtleUpgrades.getVanillaUpgrades()
                    .map( x -> TURTLE_NORMAL.create( -1, null, -1, null, x, 0, null ) )
                    .forEach( output::accept );

                output.accept( TURTLE_ADVANCED.create( -1, null, -1, null, null, 0, null ) );
                dan200.computercraft.shared.TurtleUpgrades.getVanillaUpgrades()
                    .map( x -> TURTLE_ADVANCED.create( -1, null, -1, null, x, 0, null ) )
                    .forEach( output::accept );

                for( int colour = 0; colour < 16; colour++ )
                {
                    output.accept( DISK.createFromIDAndColour( -1, null, Colour.VALUES[colour].getHex() ) );
                }

                output.accept( PRINTED_PAGE );
                output.accept( PRINTED_PAGES );
                output.accept( PRINTED_BOOK );

                output.accept( SPEAKER );
                output.accept( DISK_DRIVE );
                output.accept( PRINTER );
                output.accept( MONITOR_NORMAL );
                output.accept( MONITOR_ADVANCED );
                output.accept( WIRELESS_MODEM_NORMAL );
                output.accept( WIRELESS_MODEM_ADVANCED );
                output.accept( WIRED_MODEM_FULL );
                output.accept( WIRED_MODEM );
                output.accept( CABLE );
            } )
            .build();

        private static <B extends Block, I extends Item> I ofBlock( B parent, BiFunction<B, Item.Properties, I> supplier )
        {
            var id = BuiltInRegistries.BLOCK.getKey( parent );
            var item = supplier.apply( parent, properties() );
            if (item instanceof PolymerAutoTexturedItem) {
                PolymerSetup.requestModel(new ResourceLocation(MOD_ID, "item/" + id.getPath()), item);
            }
            return net.minecraft.core.Registry.register( BuiltInRegistries.ITEM, id, item );
        }

        private static Item.Properties properties()
        {
            return new Item.Properties();
        }

        private static <T extends Item> T register( String id, T item )
        {
            if (item instanceof PolymerAutoTexturedItem) {
                PolymerSetup.requestModel(new ResourceLocation(MOD_ID, "item/" + id), item);
            }
            return net.minecraft.core.Registry.register( BuiltInRegistries.ITEM, new ResourceLocation( MOD_ID, id ), item );
        }
    }

    public static class ModEntities {
        public static final EntityType<TurtlePlayer> TURTLE_PLAYER =
            net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(MOD_ID, "turtle_player"),
                EntityType.Builder.<TurtlePlayer>createNothing(MobCategory.MISC).noSave().noSummon().sized(0, 0).build(ComputerCraft.MOD_ID + ":turtle_player"));

        static {
            PolymerEntityUtils.registerType(TURTLE_PLAYER);
        }
    }

    public static final class TurtleUpgrades {
        public static TurtleModem wirelessModemNormal =
            new TurtleModem(new ResourceLocation(ComputerCraft.MOD_ID, "wireless_modem_normal"), new ItemStack(ModItems.WIRELESS_MODEM_NORMAL), false);

        public static TurtleModem wirelessModemAdvanced =
            new TurtleModem(new ResourceLocation(ComputerCraft.MOD_ID, "wireless_modem_advanced"), new ItemStack(ModItems.WIRELESS_MODEM_ADVANCED), true);

        public static TurtleSpeaker speaker =
            new TurtleSpeaker(new ResourceLocation(ComputerCraft.MOD_ID, "speaker"), new ItemStack(ModItems.SPEAKER));

        public static TurtleCraftingTable craftingTable =
            new TurtleCraftingTable(new ResourceLocation("minecraft", "crafting_table"), new ItemStack(Items.CRAFTING_TABLE));

        public static TurtleTool diamondSword =
            new TurtleTool(new ResourceLocation("minecraft", "diamond_sword"), Items.DIAMOND_SWORD, 9.0f, ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE);

        public static TurtleTool diamondShovel =
            new TurtleTool(new ResourceLocation("minecraft", "diamond_shovel"), Items.DIAMOND_SHOVEL, 1.0f, ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE);

        public static TurtleTool diamondPickaxe =
            new TurtleTool(new ResourceLocation("minecraft", "diamond_pickaxe"), Items.DIAMOND_PICKAXE, 1.0f, null);

        public static TurtleTool diamondAxe =
            new TurtleTool(new ResourceLocation("minecraft", "diamond_axe"), Items.DIAMOND_AXE, 6.0f, null);

        public static TurtleTool diamondHoe =
            new TurtleTool(new ResourceLocation("minecraft", "diamond_hoe"), Items.DIAMOND_HOE, 1.0f, ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE);

        public static TurtleTool netheritePickaxe =
            new TurtleTool(new ResourceLocation("minecraft", "netherite_pickaxe"), Items.NETHERITE_PICKAXE, 1.0f, null);

        public static void registerTurtleUpgrades() {
            ComputerCraftAPI.registerTurtleUpgrade(wirelessModemNormal);
            ComputerCraftAPI.registerTurtleUpgrade(wirelessModemAdvanced);
            ComputerCraftAPI.registerTurtleUpgrade(speaker);
            ComputerCraftAPI.registerTurtleUpgrade(craftingTable);

            ComputerCraftAPI.registerTurtleUpgrade(diamondSword);
            ComputerCraftAPI.registerTurtleUpgrade(diamondShovel);
            ComputerCraftAPI.registerTurtleUpgrade(diamondPickaxe);
            ComputerCraftAPI.registerTurtleUpgrade(diamondAxe);
            ComputerCraftAPI.registerTurtleUpgrade(diamondHoe);
            ComputerCraftAPI.registerTurtleUpgrade(netheritePickaxe);
        }
    }

    public static final class PocketUpgrades {
        public static PocketModem wirelessModemNormal = new PocketModem(new ResourceLocation(ComputerCraft.MOD_ID, "wireless_modem_normal"), new ItemStack(ModItems.WIRELESS_MODEM_NORMAL), false);

        public static PocketModem wirelessModemAdvanced = new PocketModem(new ResourceLocation(ComputerCraft.MOD_ID, "wireless_modem_advanced"), new ItemStack(ModItems.WIRELESS_MODEM_ADVANCED), true);

        public static PocketSpeaker speaker = new PocketSpeaker(new ResourceLocation(ComputerCraft.MOD_ID, "speaker"), new ItemStack(ModItems.SPEAKER));

        public static void registerPocketUpgrades() {
            ComputerCraftAPI.registerPocketUpgrade(wirelessModemNormal);
            ComputerCraftAPI.registerPocketUpgrade(wirelessModemAdvanced);
            ComputerCraftAPI.registerPocketUpgrade(speaker);
        }
    }
}
