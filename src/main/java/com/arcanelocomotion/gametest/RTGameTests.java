package com.arcanelocomotion.gametest;

import com.arcanelocomotion.ArcaneLocomotion;
import com.arcanelocomotion.RTDataMaps;
import com.arcanelocomotion.RTRegistry;
import com.arcanelocomotion.block.ArcaneBoosterBlockEntity;
import com.arcanelocomotion.block.EssentiaLoaderBlock;
import com.arcanelocomotion.block.GolemStationBlock;
import com.arcanelocomotion.block.GolemStationBlockEntity;
import com.arcanelocomotion.block.RuneSignalBlock;
import com.arcanelocomotion.block.RuneSignalBlockEntity;
import com.arcanelocomotion.entity.AuraLocomotive;
import com.arcanelocomotion.entity.EldritchCart;
import com.arcanelocomotion.entity.EssentiaTankCart;
import com.arcanelocomotion.item.SpectralRailRodItem;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.function.Consumer;
import mods.railcraft.world.entity.vehicle.locomotive.Locomotive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * In-world tests for each feature. Run them all with {@code gradlew runGameTestServer}. Each test lives in a 9 x 6 x 9
 * arena (stone floor at y = 0) or the 17 x 6 x 17 large one.
 */
public final class RTGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ArcaneLocomotion.MODID);
    private static final Identifier ARENA = ArcaneLocomotion.id("arena");
    private static final Identifier ARENA_LARGE = ArcaneLocomotion.id("arena_large");

    record Test(String name, Consumer<GameTestHelper> body, int maxTicks, Identifier arena) {
        Test(String name, Consumer<GameTestHelper> body, int maxTicks) {
            this(name, body, maxTicks, ARENA);
        }
    }

    private static final List<Test> TESTS = List.of(
            new Test("tank_cart_holds_one_aspect_and_keeps_it_as_an_item", RTGameTests::tankCart, 20),
            new Test("essentia_loaders_move_essentia_between_carts", RTGameTests::loaders, 80),
            new Test("rune_signal_sees_tank_and_chest_carts", RTGameTests::runeSignal, 60),
            new Test("golem_station_holds_cart_until_empty", RTGameTests::golemStation, 60),
            new Test("booster_throws_cart_and_spends_motus", RTGameTests::booster, 80, ARENA_LARGE),
            new Test("spectral_rod_lays_rails_that_fade", RTGameTests::spectral, 20),
            new Test("spectral_rod_needs_wand_vis_in_survival", RTGameTests::spectralCost, 20),
            new Test("eldritch_cart_is_fast_fireproof_and_warps", RTGameTests::eldritch, 20),
            new Test("aura_locomotive_runs_on_vis", RTGameTests::auraLoco, 20),
            new Test("every_block_has_a_loot_table", RTGameTests::lootTables, 20));

    static {
        for (Test t : TESTS) {
            FUNCTIONS.register(t.name(), () -> t.body());
        }
    }

    private RTGameTests() {
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(RTGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ArcaneLocomotion.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (Test t : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ArcaneLocomotion.id(t.name()));
            event.registerTest(ArcaneLocomotion.id(t.name()), new FunctionGameTestInstance(fn, new TestData<>(env, t.arena(), t.maxTicks(), 0, true)));
        }
    }

    // --- helpers ---

    private static Holder<IAspect> aspect(GameTestHelper h, ResourceKey<IAspect> key) {
        return h.getLevel().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(key);
    }

    private static <T extends net.minecraft.world.entity.vehicle.minecart.AbstractMinecart> T cartOn(GameTestHelper h, EntityType<T> type, BlockPos rail) {
        T cart = h.spawn(type, rail);
        cart.setDeltaMovement(Vec3.ZERO);
        return cart;
    }

    /**
     * A survival-mode player that never joins the server's player list, so no login packets go out (the vanilla mock
     * joins, and Curios then tries to sync to it and throws).
     */
    private static ServerPlayer survival(GameTestHelper h) {
        ServerPlayer player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "arcane-tester"));
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.setPos(at.x, at.y, at.z);
        player.getAbilities().instabuild = false;
        return player;
    }

    private static ServerPlayer creative(GameTestHelper h) {
        ServerPlayer player = survival(h);
        player.getAbilities().instabuild = true;
        return player;
    }

    // --- essentia tank cart ---

    private static void tankCart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 1, 4), Blocks.RAIL);
        EssentiaTankCart cart = cartOn(h, RTRegistry.ESSENTIA_TANK_CART.get(), new BlockPos(4, 1, 4));
        Holder<IAspect> motus = aspect(h, TTAspects.MOTUS);
        Holder<IAspect> aer = aspect(h, TTAspects.AER);
        h.assertValueEqual(cart.fill(motus, 30, false), 30, "fills with Motus");
        h.assertValueEqual(cart.fill(aer, 5, false), 0, "a tank holds one aspect at a time");
        h.assertValueEqual(cart.fill(motus, 10_000, false), EssentiaTankCart.CAPACITY - 30, "fills only to capacity");
        h.assertValueEqual(cart.drain(null, EssentiaTankCart.CAPACITY - 40, false), EssentiaTankCart.CAPACITY - 40, "drains");
        h.assertValueEqual(cart.amount(), 40, "40 left");
        ItemStack item = cart.getPickResult();
        cart.hurtServer(level, level.damageSources().generic(), 1000f);
        // The dropped item carries what was inside; placing it brings the essentia back.
        AspectInstance carried = h.getEntities(EntityType.ITEM).stream().map(e -> e.getItem().get(RTRegistry.CART_ESSENTIA.get()))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        h.assertTrue(carried != null && carried.amount() == 40, "the dropped cart keeps its 40 Motus, got " + carried);
        item.set(RTRegistry.CART_ESSENTIA.get(), carried);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 1, 4.5));
        EssentiaTankCart back = EssentiaTankCart.create(item, level, at.x, at.y, at.z);
        h.assertValueEqual(back.amount(), 40, "a placed cart starts with the item's essentia");
        h.assertTrue(motus.equals(back.aspect()), "and the same aspect");
        h.succeed();
    }

    // --- essentia loader ---

    private static void loaders(GameTestHelper h) {
        // Two loaders side by side, each under a rail with a tank cart: the left one unloads, the right one loads.
        BlockPos from = new BlockPos(3, 1, 4);
        BlockPos to = new BlockPos(4, 1, 4);
        h.setBlock(from, RTRegistry.ESSENTIA_LOADER.get().defaultBlockState().setValue(EssentiaLoaderBlock.UNLOAD, true));
        h.setBlock(to, RTRegistry.ESSENTIA_LOADER.get().defaultBlockState().setValue(EssentiaLoaderBlock.UNLOAD, false));
        h.setBlock(from.above(), Blocks.RAIL);
        h.setBlock(to.above(), Blocks.RAIL);
        EssentiaTankCart a = cartOn(h, RTRegistry.ESSENTIA_TANK_CART.get(), from.above());
        EssentiaTankCart b = cartOn(h, RTRegistry.ESSENTIA_TANK_CART.get(), to.above());
        a.fill(aspect(h, TTAspects.MOTUS), 24, false);
        h.assertTrue(h.getLevel().getCapability(com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities.TRANSPORT, h.absolutePos(to), Direction.WEST) != null,
                "a loader is an essentia transport for tubes");
        h.succeedWhen(() -> {
            h.assertValueEqual(b.amount(), 24, "the loading cart got all the Motus");
            h.assertValueEqual(a.amount(), 0, "the unloading cart is empty");
        });
    }

    // --- rune signal ---

    private static void runeSignal(GameTestHelper h) {
        BlockPos sig = new BlockPos(2, 1, 4);
        BlockPos sig2 = new BlockPos(6, 1, 4);
        h.setBlock(sig, RTRegistry.RUNE_SIGNAL.get());
        h.setBlock(sig2, RTRegistry.RUNE_SIGNAL.get());
        h.setBlock(sig.above(), Blocks.RAIL);
        h.setBlock(sig2.above(), Blocks.RAIL);
        RuneSignalBlockEntity tank = (RuneSignalBlockEntity) h.getBlockEntity(sig, RuneSignalBlockEntity.class);
        RuneSignalBlockEntity chest = (RuneSignalBlockEntity) h.getBlockEntity(sig2, RuneSignalBlockEntity.class);
        tank.tune(TTAspects.MOTUS);
        // Tune the second by an item, as a player would: to the largest aspect a feather has.
        h.assertTrue(chest.tuneFrom(new ItemStack(Items.FEATHER)), "a feather has aspects to tune to");
        EssentiaTankCart cart = cartOn(h, RTRegistry.ESSENTIA_TANK_CART.get(), sig.above());
        cart.fill(aspect(h, TTAspects.AER), 10, false);
        MinecartChest box = cartOn(h, EntityType.CHEST_MINECART, sig2.above());
        box.setItem(5, new ItemStack(Items.FEATHER, 3));
        int[] step = {0};
        h.succeedWhen(() -> {
            if (step[0] == 0) {
                h.assertTrue(h.getBlockState(sig2).getValue(RuneSignalBlock.POWERED), "a chest cart carrying feathers lights its signal");
                h.assertTrue(!h.getBlockState(sig).getValue(RuneSignalBlock.POWERED), "Aer essentia does not light a Motus signal");
                cart.drain(null, 10, false);
                cart.fill(aspect(h, TTAspects.MOTUS), 10, false);
                step[0] = 1;
            }
            h.assertTrue(h.getBlockState(sig).getValue(RuneSignalBlock.POWERED), "Motus essentia lights the Motus signal");
            h.assertValueEqual(h.getLevel().getBestNeighborSignal(h.absolutePos(sig).east()), 15, "with full strength");
        });
    }

    // --- golem station ---

    private static void golemStation(GameTestHelper h) {
        BlockPos st = new BlockPos(4, 1, 4);
        h.setBlock(st, RTRegistry.GOLEM_STATION.get().defaultBlockState().setValue(GolemStationBlock.UNLOAD, true));
        h.setBlock(st.above(), Blocks.RAIL);
        h.setBlock(st.above().west(), Blocks.RAIL);
        h.setBlock(st.above().east(), Blocks.RAIL);
        MinecartChest box = cartOn(h, EntityType.CHEST_MINECART, st.above());
        box.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        box.setDeltaMovement(0.1, 0, 0);
        GolemStationBlockEntity be = (GolemStationBlockEntity) h.getBlockEntity(st, GolemStationBlockEntity.class);
        int[] step = {0};
        h.succeedWhen(() -> {
            if (step[0] == 0) {
                h.assertTrue(be.heldCart() == box, "the station holds the cart standing on it");
                h.assertTrue(box.getDeltaMovement().lengthSqr() < 1e-6, "and stops it");
                var items = h.getLevel().getCapability(Capabilities.Item.BLOCK, h.absolutePos(st), Direction.NORTH);
                h.assertTrue(items != null && items.size() == box.getContainerSize(), "golems reach the cart's slots through the station");
                h.assertValueEqual(items.getAmountAsLong(0), 16L, "and see its cobblestone");
                box.clearContent();
                step[0] = 1;
            }
            h.assertTrue(be.heldCart() == null, "an emptied cart is let go");
            h.assertTrue(box.getDeltaMovement().x > 0.05, "with the motion it came in with");
        });
    }

    // --- arcane booster track ---

    private static void booster(GameTestHelper h) {
        int z = 8;
        for (int x = 1; x <= 15; x++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.RAIL);
        }
        BlockPos at = new BlockPos(5, 1, z);
        h.setBlock(at.north(), Blocks.REDSTONE_BLOCK);
        h.setBlock(at, RTRegistry.ARCANE_BOOSTER_TRACK.get());
        ArcaneBoosterBlockEntity be = (ArcaneBoosterBlockEntity) h.getBlockEntity(at, ArcaneBoosterBlockEntity.class);
        be.setMotus(10);
        int cost = RTDataMaps.rail(RTRegistry.ARCANE_BOOSTER_TRACK.get()).essentiaPerBoost();
        float boost = RTDataMaps.rail(RTRegistry.ARCANE_BOOSTER_TRACK.get()).boost();
        var cart = h.spawn(EntityType.MINECART, new BlockPos(2, 1, z));
        cart.setDeltaMovement(0.4, 0, 0);
        double[] best = {0};
        double[] far = {0};
        h.onEachTick(() -> {
            best[0] = Math.max(best[0], cart.getDeltaMovement().horizontalDistance());
            far[0] = Math.max(far[0], h.relativeVec(cart.position()).x);
        });
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(at).getValue(PoweredRailBlock.POWERED), "the booster is powered");
            h.assertValueEqual(be.motus(), 10 - cost, "one boost's Motus spent (cart got to x " + far[0] + ", best speed " + best[0] + ")");
            h.assertTrue(best[0] > boost * 0.8, "the cart was thrown to about " + boost + ", got " + best[0]);
        });
    }

    // --- spectral rails ---

    private static void spectral(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = creative(h);
        BlockPos start = h.absolutePos(new BlockPos(1, 0, 4));
        int laid = SpectralRailRodItem.lay(level, player, start, Direction.EAST);
        h.assertTrue(laid >= 5, "lays a run of rails, got " + laid);
        h.assertTrue(laid <= SpectralRailRodItem.MAX_RAILS, "at most " + SpectralRailRodItem.MAX_RAILS);
        BlockPos first = start.above();
        Block rail = RTRegistry.SPECTRAL_RAIL.get();
        h.assertTrue(level.getBlockState(first).is(rail), "the first rail sits right above the clicked block");
        h.assertTrue(level.getBlockTicks().hasScheduledTick(first, rail), "each rail is set to fade");
        h.assertValueEqual(RTDataMaps.rail(rail).lifetime(), 1200, "after a minute");
        // Lift one into the air: it stays, held by nothing.
        BlockPos floating = h.absolutePos(new BlockPos(4, 3, 1));
        level.setBlock(floating, rail.defaultBlockState(), Block.UPDATE_ALL);
        level.getBlockState(floating).tick(level, floating, level.getRandom());
        h.assertTrue(level.getBlockState(floating).isAir(), "when its time is up it fades away");
        h.assertTrue(level.getBlockState(first).is(rail), "and the others stay until theirs");
        h.succeed();
    }

    private static void spectralCost(GameTestHelper h) {
        ServerPlayer player = survival(h);
        BlockPos start = h.absolutePos(new BlockPos(1, 0, 2));
        h.assertValueEqual(SpectralRailRodItem.lay(h.getLevel(), player, start, Direction.EAST), 0, "no wand, no rails");
        player.discard();
        h.succeed();
    }

    // --- eldritch line ---

    private static void eldritch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(2, 1, 4), Blocks.RAIL);
        h.setBlock(new BlockPos(6, 1, 4), RTRegistry.ELDRITCH_RAIL.get());
        EldritchCart plain = cartOn(h, RTRegistry.ELDRITCH_CART.get(), new BlockPos(2, 1, 4));
        EldritchCart dark = cartOn(h, RTRegistry.ELDRITCH_CART.get(), new BlockPos(6, 1, 4));
        double normal = plain.topSpeed(level);
        double fast = dark.topSpeed(level);
        h.assertTrue(fast > normal * 1.5, "on eldritch rail it runs far faster: " + fast + " vs " + normal);
        h.assertValueEqual(fast, (double) RTDataMaps.rail(RTRegistry.ELDRITCH_RAIL.get()).maxSpeed(), "at the rail's tuned speed");
        h.assertTrue(!dark.hurtServer(level, level.damageSources().lava(), 10f), "fire and lava cannot hurt it");
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 6));
        h.assertTrue(!dark.hurtServer(level, level.damageSources().mobAttack(zombie), 10f), "creatures cannot break it");
        h.assertTrue(dark.getDisplayBlockState().is(RTRegistry.DECO.get(3).slab().get()), "it is floored with runed abyssal brick");
        ServerPlayer rider = survival(h);
        int before = WarpHelper.getWarp(rider).get(WarpType.TEMPORARY);
        EldritchCart.applyWarp(rider);
        h.assertValueEqual(WarpHelper.getWarp(rider).get(WarpType.TEMPORARY), before + 1, "riding the line leaves temporary warp");
        rider.discard();
        h.succeed();
    }

    // --- aura locomotive ---

    private static void auraLoco(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 1, 4), Blocks.RAIL);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 1, 4.5));
        AuraLocomotive loco = AuraLocomotive.create(new ItemStack(RTRegistry.AURA_LOCOMOTIVE_ITEM.get()), level, at.x, at.y, at.z);
        level.addFreshEntity(loco);
        BlockPos here = loco.blockPosition();
        AuraHelper.addVis(level, here, 50f);
        float vis = AuraHelper.getVis(level, here);
        h.assertTrue(vis > 1f, "the chunk has vis to draw on, " + vis);
        h.assertTrue(!loco.needsFuel(), "it never asks for charge");
        int fuel = loco.retrieveFuel();
        h.assertTrue(fuel > 0, "it draws fuel from the aura");
        h.assertTrue(AuraHelper.getVis(level, here) < vis, "and the aura pays for it");
        loco.setSpeed(Locomotive.Speed.MAX);
        h.assertValueEqual(loco.getSpeed(), Locomotive.Speed.NORMAL, "it tops out at normal speed");
        h.assertTrue(loco.getPickResult().is(RTRegistry.AURA_LOCOMOTIVE_ITEM.get()), "it picks as its own item");
        loco.discard();
        h.succeed();
    }

    // --- data ---

    private static void lootTables(GameTestHelper h) {
        var tables = h.getLevel().getServer().reloadableRegistries();
        for (var holder : RTRegistry.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (block == RTRegistry.SPECTRAL_RAIL.get()) {
                continue;
            }
            var key = block.getLootTable().orElseThrow();
            h.assertTrue(tables.getLootTable(key) != LootTable.EMPTY, "loot table for " + holder.getId());
        }
        h.succeed();
    }
}
