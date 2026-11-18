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
import java.util.UUID;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import mods.railcraft.world.entity.vehicle.locomotive.Locomotive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * In-world tests for each feature. Run them all with {@code gradlew runGameTestServer}. Each test lives in a 9 x 6 x 9
 * arena (stone floor at helper y = 1) or the 17 x 6 x 17 large one.
 */
@GameTestHolder(ArcaneLocomotion.MODID)
@PrefixGameTestTemplate(false)
public final class RTGameTests {
    private RTGameTests() {
    }

    // --- helpers ---

    private static Holder<IAspect> aspect(GameTestHelper h, ResourceKey<IAspect> key) {
        return h.getLevel().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(key);
    }

    private static <T extends net.minecraft.world.entity.vehicle.AbstractMinecart> T cartOn(GameTestHelper h, EntityType<T> type, BlockPos rail) {
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
        Vec3 at = h.absoluteVec(new Vec3(1.5, 2, 1.5));
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

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void tankCart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 2, 4), Blocks.RAIL);
        EssentiaTankCart cart = cartOn(h, RTRegistry.ESSENTIA_TANK_CART.get(), new BlockPos(4, 2, 4));
        Holder<IAspect> motus = aspect(h, TTAspects.MOTUS);
        Holder<IAspect> aer = aspect(h, TTAspects.AER);
        h.assertValueEqual(cart.fill(motus, 30, false), 30, "fills with Motus");
        h.assertValueEqual(cart.fill(aer, 5, false), 0, "a tank holds one aspect at a time");
        h.assertValueEqual(cart.fill(motus, 10_000, false), EssentiaTankCart.CAPACITY - 30, "fills only to capacity");
        h.assertValueEqual(cart.drain(null, EssentiaTankCart.CAPACITY - 40, false), EssentiaTankCart.CAPACITY - 40, "drains");
        h.assertValueEqual(cart.amount(), 40, "40 left");
        ItemStack item = cart.getPickResult();
        cart.hurt(level.damageSources().generic(), 1000f);
        // The dropped item carries what was inside; placing it brings the essentia back.
        AspectInstance carried = h.getEntities(EntityType.ITEM).stream().map(e -> e.getItem().get(RTRegistry.CART_ESSENTIA.get()))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        h.assertTrue(carried != null && carried.amount() == 40, "the dropped cart keeps its 40 Motus, got " + carried);
        item.set(RTRegistry.CART_ESSENTIA.get(), carried);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
        EssentiaTankCart back = EssentiaTankCart.create(item, at.x, at.y, at.z, level);
        h.assertValueEqual(back.amount(), 40, "a placed cart starts with the item's essentia");
        h.assertTrue(motus.equals(back.aspect()), "and the same aspect");
        h.succeed();
    }

    // --- essentia loader ---

    @GameTest(template = "arena", timeoutTicks = 80)
    public static void loaders(GameTestHelper h) {
        // Two loaders side by side, each under a rail with a tank cart: the left one unloads, the right one loads.
        BlockPos from = new BlockPos(3, 2, 4);
        BlockPos to = new BlockPos(4, 2, 4);
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

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void runeSignal(GameTestHelper h) {
        BlockPos sig = new BlockPos(2, 2, 4);
        BlockPos sig2 = new BlockPos(6, 2, 4);
        h.setBlock(sig, RTRegistry.RUNE_SIGNAL.get());
        h.setBlock(sig2, RTRegistry.RUNE_SIGNAL.get());
        h.setBlock(sig.above(), Blocks.RAIL);
        h.setBlock(sig2.above(), Blocks.RAIL);
        RuneSignalBlockEntity tank = h.getBlockEntity(sig);
        RuneSignalBlockEntity chest = h.getBlockEntity(sig2);
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

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void golemStation(GameTestHelper h) {
        BlockPos st = new BlockPos(4, 2, 4);
        h.setBlock(st, RTRegistry.GOLEM_STATION.get().defaultBlockState().setValue(GolemStationBlock.UNLOAD, true));
        h.setBlock(st.above(), Blocks.RAIL);
        h.setBlock(st.above().west(), Blocks.RAIL);
        h.setBlock(st.above().east(), Blocks.RAIL);
        MinecartChest box = cartOn(h, EntityType.CHEST_MINECART, st.above());
        box.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        box.setDeltaMovement(0.1, 0, 0);
        GolemStationBlockEntity be = h.getBlockEntity(st);
        int[] step = {0};
        h.succeedWhen(() -> {
            if (step[0] == 0) {
                h.assertTrue(be.heldCart() == box, "the station holds the cart standing on it");
                h.assertTrue(box.getDeltaMovement().lengthSqr() < 1e-6, "and stops it");
                var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(st), Direction.NORTH);
                h.assertTrue(items != null && items.getSlots() == box.getContainerSize(), "golems reach the cart's slots through the station");
                h.assertValueEqual(items.getStackInSlot(0).getCount(), 16, "and see its cobblestone");
                box.clearContent();
                step[0] = 1;
            }
            h.assertTrue(be.heldCart() == null, "an emptied cart is let go");
            h.assertTrue(box.getDeltaMovement().x > 0.05, "with the motion it came in with");
        });
    }

    // --- arcane booster track ---

    @GameTest(template = "arena_large", timeoutTicks = 80)
    public static void booster(GameTestHelper h) {
        int z = 8;
        for (int x = 1; x <= 15; x++) {
            h.setBlock(new BlockPos(x, 2, z), Blocks.RAIL);
        }
        BlockPos at = new BlockPos(5, 2, z);
        h.setBlock(at.north(), Blocks.REDSTONE_BLOCK);
        h.setBlock(at, RTRegistry.ARCANE_BOOSTER_TRACK.get());
        ArcaneBoosterBlockEntity be = h.getBlockEntity(at);
        be.setMotus(10);
        int cost = RTDataMaps.rail(RTRegistry.ARCANE_BOOSTER_TRACK.get()).essentiaPerBoost();
        float boost = RTDataMaps.rail(RTRegistry.ARCANE_BOOSTER_TRACK.get()).boost();
        var cart = h.spawn(EntityType.MINECART, new BlockPos(2, 2, z));
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

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void spectral(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = creative(h);
        BlockPos start = h.absolutePos(new BlockPos(1, 1, 4));
        int laid = SpectralRailRodItem.lay(level, player, start, Direction.EAST);
        h.assertTrue(laid >= 5, "lays a run of rails, got " + laid);
        h.assertTrue(laid <= SpectralRailRodItem.MAX_RAILS, "at most " + SpectralRailRodItem.MAX_RAILS);
        BlockPos first = start.above();
        Block rail = RTRegistry.SPECTRAL_RAIL.get();
        h.assertTrue(level.getBlockState(first).is(rail), "the first rail sits right above the clicked block");
        h.assertTrue(level.getBlockTicks().hasScheduledTick(first, rail), "each rail is set to fade");
        h.assertValueEqual(RTDataMaps.rail(rail).lifetime(), 1200, "after a minute");
        // Lift one into the air: it stays, held by nothing.
        BlockPos floating = h.absolutePos(new BlockPos(4, 4, 1));
        level.setBlock(floating, rail.defaultBlockState(), Block.UPDATE_ALL);
        level.neighborChanged(floating, Blocks.AIR, floating.below());
        h.assertTrue(level.getBlockState(floating).is(rail), "floating rails survive neighbour changes without support");
        level.getBlockState(floating).tick(level, floating, level.getRandom());
        h.assertTrue(level.getBlockState(floating).isAir(), "when its time is up it fades away");
        h.assertTrue(level.getBlockState(first).is(rail), "and the others stay until theirs");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void spectralCost(GameTestHelper h) {
        ServerPlayer player = survival(h);
        BlockPos start = h.absolutePos(new BlockPos(1, 1, 2));
        h.assertValueEqual(SpectralRailRodItem.lay(h.getLevel(), player, start, Direction.EAST), 0, "no wand, no rails");
        player.discard();
        h.succeed();
    }

    // --- eldritch line ---

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void eldritch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(2, 2, 4), Blocks.RAIL);
        h.setBlock(new BlockPos(6, 2, 4), RTRegistry.ELDRITCH_RAIL.get());
        EldritchCart plain = cartOn(h, RTRegistry.ELDRITCH_CART.get(), new BlockPos(2, 2, 4));
        EldritchCart dark = cartOn(h, RTRegistry.ELDRITCH_CART.get(), new BlockPos(6, 2, 4));
        double normal = plain.topSpeed(level);
        double fast = dark.topSpeed(level);
        h.assertTrue(fast > normal * 1.5, "on eldritch rail it runs far faster: " + fast + " vs " + normal);
        h.assertValueEqual(fast, (double) RTDataMaps.rail(RTRegistry.ELDRITCH_RAIL.get()).maxSpeed(), "at the rail's tuned speed");
        h.assertTrue(!dark.hurt(level.damageSources().lava(), 10f), "fire and lava cannot hurt it");
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        h.assertTrue(!dark.hurt(level.damageSources().mobAttack(zombie), 10f), "creatures cannot break it");
        h.assertTrue(dark.getDisplayBlockState().is(RTRegistry.DECO.get(3).slab().get()), "it is floored with runed abyssal brick");
        ServerPlayer rider = survival(h);
        int before = WarpHelper.getWarp(rider).get(WarpType.TEMPORARY);
        EldritchCart.applyWarp(rider);
        h.assertValueEqual(WarpHelper.getWarp(rider).get(WarpType.TEMPORARY), before + 1, "riding the line leaves temporary warp");
        rider.discard();
        h.succeed();
    }

    // --- aura locomotive ---

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void auraLoco(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 2, 4), Blocks.RAIL);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
        AuraLocomotive loco = AuraLocomotive.create(new ItemStack(RTRegistry.AURA_LOCOMOTIVE_ITEM.get()), at.x, at.y, at.z, level);
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

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void lootTables(GameTestHelper h) {
        long recipes = h.getLevel().getRecipeManager().getRecipeIds()
                .filter(id -> id.getNamespace().equals(ArcaneLocomotion.MODID)).count();
        h.assertValueEqual(recipes, 54L, "all crafting, stonecutting and arcane recipes load");
        var tables = h.getLevel().getServer().reloadableRegistries();
        for (var holder : RTRegistry.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (block == RTRegistry.SPECTRAL_RAIL.get()) {
                continue;
            }
            var key = block.getLootTable();
            h.assertTrue(tables.getLootTable(key) != LootTable.EMPTY, "loot table for " + holder.getId());
        }
        h.succeed();
    }
}
