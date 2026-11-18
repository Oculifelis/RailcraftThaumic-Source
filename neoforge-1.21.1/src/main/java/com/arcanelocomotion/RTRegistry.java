package com.arcanelocomotion;

import com.arcanelocomotion.block.ArcaneBoosterBlockEntity;
import com.arcanelocomotion.block.ArcaneBoosterRailBlock;
import com.arcanelocomotion.block.EldritchRailBlock;
import com.arcanelocomotion.block.EssentiaLoaderBlock;
import com.arcanelocomotion.block.EssentiaLoaderBlockEntity;
import com.arcanelocomotion.block.GolemStationBlock;
import com.arcanelocomotion.block.GolemStationBlockEntity;
import com.arcanelocomotion.block.RuneSignalBlock;
import com.arcanelocomotion.block.RuneSignalBlockEntity;
import com.arcanelocomotion.block.SpectralRailBlock;
import com.arcanelocomotion.entity.AuraLocomotive;
import com.arcanelocomotion.entity.EldritchCart;
import com.arcanelocomotion.entity.EssentiaTankCart;
import com.arcanelocomotion.entity.ThaumicSteamLocomotive;
import com.arcanelocomotion.item.BoilerEssentia;
import com.arcanelocomotion.item.SelfRepair;
import com.arcanelocomotion.item.SpectralRailRodItem;
import com.arcanelocomotion.item.ThaumicCrowbarItem;
import com.arcanelocomotion.item.ThaumicLocomotiveItem;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.content.equipment.GearWarp;
import com.leclowndu93150.thaumaturge.content.equipment.TTMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import mods.railcraft.tags.RailcraftTags;
import mods.railcraft.world.item.CartItem;
import mods.railcraft.world.item.LocomotiveItem;
import mods.railcraft.world.item.component.LocomotiveColorComponent;
import mods.railcraft.world.item.component.LocomotiveWhistlePitchComponent;
import mods.railcraft.world.item.component.RailcraftDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RTRegistry {
    private static final String MODID = ArcaneLocomotion.MODID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Every item for the creative tab, in order. */
    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SelfRepair>> SELF_REPAIR = COMPONENTS.registerComponentType("self_repair",
            b -> b.persistent(SelfRepair.CODEC).networkSynchronized(SelfRepair.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoilerEssentia>> BOILER_ESSENTIA = COMPONENTS.registerComponentType("boiler_essentia",
            b -> b.persistent(BoilerEssentia.CODEC).networkSynchronized(BoilerEssentia.STREAM_CODEC));
    /** The essentia an essentia tank cart item carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AspectInstance>> CART_ESSENTIA = COMPONENTS.registerComponentType("cart_essentia",
            b -> b.persistent(AspectInstance.CODEC).networkSynchronized(AspectInstance.STREAM_CODEC));

    // Same handling values as Railcraft Reborn's steel and diamond crowbars, on Thaumaturge's tool materials.
    public static final DeferredItem<ThaumicCrowbarItem> THAUMIUM_CROWBAR = tab(ITEMS.register("thaumium_crowbar", () -> new ThaumicCrowbarItem(TTMaterials.TOOL_THAUMIUM,
            new Item.Properties().attributes(DiggerItem.createAttributes(TTMaterials.TOOL_THAUMIUM, 2.5f, -2.6f)))));
    public static final DeferredItem<ThaumicCrowbarItem> VOID_CROWBAR = tab(ITEMS.register("void_crowbar", () -> new ThaumicCrowbarItem(TTMaterials.TOOL_VOID,
            GearWarp.with(new Item.Properties().attributes(DiggerItem.createAttributes(TTMaterials.TOOL_VOID, 2.5f, -2.4f))
                    .rarity(Rarity.UNCOMMON), 1)
                    .component(SELF_REPAIR, new SelfRepair(20, 1)))));

    public static final DeferredHolder<EntityType<?>, EntityType<ThaumicSteamLocomotive>> THAUMIC_STEAM_LOCOMOTIVE = ENTITIES.register("thaumic_steam_locomotive",
            () -> EntityType.Builder.<ThaumicSteamLocomotive>of(ThaumicSteamLocomotive::new, MobCategory.MISC)
                    .clientTrackingRange(256).updateInterval(2).sized(0.98f, 1f)
                    .build(MODID + ":thaumic_steam_locomotive"));
    public static final DeferredItem<ThaumicLocomotiveItem> THAUMIC_STEAM_LOCOMOTIVE_ITEM = tab(ITEMS.register("thaumic_steam_locomotive",
            () -> new ThaumicLocomotiveItem(ThaumicSteamLocomotive::create, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(RailcraftDataComponents.LOCOMOTIVE_COLOR, new LocomotiveColorComponent(DyeColor.PURPLE, DyeColor.BLACK))
                    .component(RailcraftDataComponents.LOCOMOTIVE_WHISTLE_PITCH, LocomotiveWhistlePitchComponent.NO_WHISTLE))));

    // --- Carts and the aura locomotive ---

    public static final DeferredHolder<EntityType<?>, EntityType<EssentiaTankCart>> ESSENTIA_TANK_CART = ENTITIES.register("essentia_tank_cart",
            () -> EntityType.Builder.<EssentiaTankCart>of(EssentiaTankCart::new, MobCategory.MISC)
                    .clientTrackingRange(8).sized(0.98f, 0.7f)
                    .build(MODID + ":essentia_tank_cart"));
    public static final DeferredItem<CartItem> ESSENTIA_TANK_CART_ITEM = tab(ITEMS.register("essentia_tank_cart",
            () -> new CartItem(EssentiaTankCart::create, new Item.Properties().stacksTo(1))));

    public static final DeferredHolder<EntityType<?>, EntityType<AuraLocomotive>> AURA_LOCOMOTIVE = ENTITIES.register("aura_locomotive",
            () -> EntityType.Builder.<AuraLocomotive>of(AuraLocomotive::new, MobCategory.MISC)
                    .clientTrackingRange(256).updateInterval(2).sized(0.98f, 1f)
                    .build(MODID + ":aura_locomotive"));
    public static final DeferredItem<LocomotiveItem> AURA_LOCOMOTIVE_ITEM = tab(ITEMS.register("aura_locomotive",
            () -> new LocomotiveItem(AuraLocomotive::create, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(RailcraftDataComponents.LOCOMOTIVE_COLOR, new LocomotiveColorComponent(DyeColor.CYAN, DyeColor.WHITE))
                    .component(RailcraftDataComponents.LOCOMOTIVE_WHISTLE_PITCH, LocomotiveWhistlePitchComponent.NO_WHISTLE))));

    public static final DeferredHolder<EntityType<?>, EntityType<EldritchCart>> ELDRITCH_CART = ENTITIES.register("eldritch_cart",
            () -> EntityType.Builder.<EldritchCart>of(EldritchCart::new, MobCategory.MISC)
                    .clientTrackingRange(8).sized(0.98f, 0.7f).fireImmune()
                    .build(MODID + ":eldritch_cart"));
    public static final DeferredItem<CartItem> ELDRITCH_CART_ITEM = tab(ITEMS.register("eldritch_cart",
            () -> new CartItem(EldritchCart::create, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant())));

    // --- Blocks ---

    public static final DeferredBlock<EssentiaLoaderBlock> ESSENTIA_LOADER = block("essentia_loader", EssentiaLoaderBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f));
    public static final DeferredBlock<RuneSignalBlock> RUNE_SIGNAL = block("rune_signal", RuneSignalBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).lightLevel(s -> s.getValue(RuneSignalBlock.POWERED) ? 7 : 0));
    public static final DeferredBlock<GolemStationBlock> GOLEM_STATION = block("golem_station", GolemStationBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final DeferredBlock<ArcaneBoosterRailBlock> ARCANE_BOOSTER_TRACK = block("arcane_booster_track", ArcaneBoosterRailBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POWERED_RAIL));
    public static final DeferredBlock<EldritchRailBlock> ELDRITCH_RAIL = block("eldritch_rail", EldritchRailBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL).sound(SoundType.METAL));
    /** Laid only by the Spectral Rail Rod, so it has no item. */
    public static final DeferredBlock<SpectralRailBlock> SPECTRAL_RAIL = BLOCKS.registerBlock("spectral_rail", SpectralRailBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL).noLootTable().noOcclusion().lightLevel(s -> 7)
                    .sound(SoundType.AMETHYST).pushReaction(PushReaction.DESTROY).instabreak());

    public static final DeferredItem<SpectralRailRodItem> SPECTRAL_RAIL_ROD = tab(ITEMS.register("spectral_rail_rod",
            () -> new SpectralRailRodItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaLoaderBlockEntity>> ESSENTIA_LOADER_BE = BLOCK_ENTITIES.register("essentia_loader",
            () -> BlockEntityType.Builder.of(EssentiaLoaderBlockEntity::new, ESSENTIA_LOADER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneSignalBlockEntity>> RUNE_SIGNAL_BE = BLOCK_ENTITIES.register("rune_signal",
            () -> BlockEntityType.Builder.of(RuneSignalBlockEntity::new, RUNE_SIGNAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemStationBlockEntity>> GOLEM_STATION_BE = BLOCK_ENTITIES.register("golem_station",
            () -> BlockEntityType.Builder.of(GolemStationBlockEntity::new, GOLEM_STATION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneBoosterBlockEntity>> ARCANE_BOOSTER_BE = BLOCK_ENTITIES.register("arcane_booster_track",
            () -> BlockEntityType.Builder.of(ArcaneBoosterBlockEntity::new, ARCANE_BOOSTER_TRACK.get()).build(null));

    // --- Decorative sets: block, slab, stairs, wall ---

    public record DecoSet(String name, DeferredBlock<Block> block, DeferredBlock<SlabBlock> slab, DeferredBlock<StairBlock> stairs,
            DeferredBlock<WallBlock> wall) {
    }

    public static final List<DecoSet> DECO = List.of(
            deco("arcane_steel_plating", Blocks.IRON_BLOCK),
            deco("void_steel_plating", Blocks.IRON_BLOCK),
            deco("greatwood_sleepers", Blocks.OAK_PLANKS),
            deco("runed_abyssal_bricks", Blocks.DEEPSLATE_BRICKS),
            deco("runed_quarried_bricks", Blocks.STONE_BRICKS),
            deco("thaumium_banded_bricks", Blocks.STONE_BRICKS));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcanelocomotion"))
            .icon(() -> new ItemStack(THAUMIUM_CROWBAR.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private RTRegistry() {
    }

    private static <T extends Supplier<? extends Item>> T tab(T item) {
        TAB_ITEMS.add(item);
        return item;
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> ctor,
            Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, ctor, props.get());
        tab(ITEMS.registerSimpleBlockItem(name, block));
        return block;
    }

    private static DecoSet deco(String name, Block like) {
        Supplier<BlockBehaviour.Properties> props = () -> BlockBehaviour.Properties.ofFullCopy(like);
        DeferredBlock<Block> base = block(name, Block::new, props);
        DeferredBlock<SlabBlock> slab = block(name + "_slab", SlabBlock::new, props);
        DeferredBlock<StairBlock> stairs = block(name + "_stairs", p -> new StairBlock(base.get().defaultBlockState(), p), props);
        DeferredBlock<WallBlock> wall = block(name + "_wall", WallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(like).forceSolidOn());
        return new DecoSet(name, base, slab, stairs, wall);
    }

    private static void oldIds(DeferredRegister<?> reg) {
        for (var holder : reg.getEntries()) {
            reg.addAlias(ResourceLocation.fromNamespaceAndPath("railcraftthaumic", holder.getId().getPath()), holder.getId());
        }
    }

    static void register(IEventBus bus) {
        // Worlds from before the rename (mod id railcraftthaumic) keep their things: alias every old id to the new one.
        oldIds(COMPONENTS);
        oldIds(ITEMS);
        oldIds(ENTITIES);
        oldIds(TABS);
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }
}
