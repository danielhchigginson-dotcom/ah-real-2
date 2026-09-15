// Kelps Auction House + Shop - Modified for persistence + /pay
// Original by Orca / modified. Prices and GUIs kept the same.
// Added: persistent balances + AH listings (survives server restart)
// Added: /pay <player> <amount>

package com.orcaengine.kelps_ah;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ChestMenu;
import java.util.HashMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Locale;
import java.util.function.BiFunction;
import net.minecraft.world.scores.ScoreAccess;
import java.util.Iterator;
import net.minecraft.world.scores.Objective;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class KelpsAhMod implements ModInitializer
{
    private static final List<Listing> LISTINGS = new ArrayList<>();
    private static final Map<UUID, Integer> BALANCES = new ConcurrentHashMap<>();
    private static long nextId = 1L;
    private static final Map<Item, Integer> SELL_PRICES = new HashMap<>();
    private static Path dataDir;
    private static MinecraftServer currentServer;
    private static int saveCounter = 0;

    private static void putSell(final Item item, final int price) {
        if (item != null) {
            SELL_PRICES.put(item, price);
        }
    }

    private static int getSellPrice(final Item item) {
        return SELL_PRICES.getOrDefault(item, 10);
    }

    private static List<ShopOffer> buildShopOffers() {
        final List<ShopOffer> list = new ArrayList<>();
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.COOKED_BEEF, 64), 2500, "Stack of Steak"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.TOTEM_OF_UNDYING, 1), 5000, "Totem of Undying"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.EXPERIENCE_BOTTLE, 64), 10000, "Stack of XP Bottles"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.IRON_HELMET, 1), 2000, "Iron Helmet"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.IRON_CHESTPLATE, 1), 2000, "Iron Chestplate"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.IRON_LEGGINGS, 1), 2000, "Iron Leggings"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.IRON_BOOTS, 1), 2000, "Iron Boots"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.DIAMOND_HELMET, 1), 20000, "Diamond Helmet"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.DIAMOND_CHESTPLATE, 1), 20000, "Diamond Chestplate"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.DIAMOND_LEGGINGS, 1), 20000, "Diamond Leggings"));
        list.add(new ShopOffer(new ItemStack((ItemLike)Items.DIAMOND_BOOTS, 1), 20000, "Diamond Boots"));
        return list;
    }

    public void onInitialize() {
        // Initialize sell prices (same as original)
        putSell(Items.ELYTRA, 200000000);
        putSell(Items.ENCHANTED_GOLDEN_APPLE, 2000000);
        putSell(Items.NETHERITE_INGOT, 1500000);
        putSell(Items.HEART_OF_THE_SEA, 1200000);
        putSell(Items.DRAGON_EGG, 50000000);
        putSell(Items.NETHERITE_BLOCK, 13500000);
        putSell(Items.DRAGON_HEAD, 1000000);
        putSell(Items.NETHER_STAR, 400000);
        putSell(Items.BEACON, 200000);
        putSell(Items.TOTEM_OF_UNDYING, 10000);
        putSell(Items.EXPERIENCE_BOTTLE, 156);
        putSell(Items.GOLDEN_APPLE, 2500);
        putSell(Items.SHULKER_BOX, 50000);
        putSell(Items.SHULKER_SHELL, 22000);
        putSell(Items.GUNPOWDER, 600);
        putSell(Items.BLAZE_ROD, 500);
        putSell(Items.GHAST_TEAR, 8500);
        putSell(Items.PHANTOM_MEMBRANE, 4000);
        putSell(Items.TURTLE_SCUTE, 25000);
        putSell(Items.COOKED_BEEF, 39);
        putSell(Items.COOKED_PORKCHOP, 39);
        putSell(Items.COOKED_CHICKEN, 50);
        putSell(Items.GOLDEN_CARROT, 350);
        putSell(Items.BREAD, 30);
        putSell(Items.SUGAR_CANE, 25);
        putSell(Items.PAPER, 30);
        putSell(Items.BOOK, 120);
        putSell(Items.WHEAT, 15);
        putSell(Items.CARROT, 12);
        putSell(Items.POTATO, 12);
        putSell(Items.PUMPKIN, 20);
        putSell(Items.MELON_SLICE, 5);
        putSell(Items.DRIED_KELP, 4);
        putSell(Items.DRIED_KELP_BLOCK, 40);
        putSell(Items.ENDER_PEARL, 250);
        putSell(Items.BONE, 20);
        putSell(Items.BONE_MEAL, 8);
        putSell(Items.STRING, 15);
        putSell(Items.ARROW, 10);
        putSell(Items.SLIME_BALL, 80);
        putSell(Items.MAGMA_CREAM, 95);
        putSell(Items.SPIDER_EYE, 15);
        putSell(Items.ROTTEN_FLESH, 5);
        putSell(Items.NAUTILUS_SHELL, 1500);
        putSell(Items.GLOW_INK_SAC, 60);
        putSell(Items.INK_SAC, 20);
        putSell(Items.DIAMOND_BLOCK, 450000);
        putSell(Items.DIAMOND, 50000);
        putSell(Items.NETHERITE_SCRAP, 350000);
        putSell(Items.GOLD_BLOCK, 18000);
        putSell(Items.GOLD_INGOT, 2000);
        putSell(Items.IRON_BLOCK, 9000);
        putSell(Items.IRON_INGOT, 1000);
        putSell(Items.QUARTZ, 350);
        putSell(Items.QUARTZ_BLOCK, 1400);
        putSell(Items.LAPIS_LAZULI, 600);
        putSell(Items.REDSTONE, 80);
        putSell(Items.COPPER_INGOT, 50);
        putSell(Items.COAL, 120);
        putSell(Items.COAL_BLOCK, 1100);
        putSell(Items.AMETHYST_SHARD, 250);
        putSell(Items.OBSIDIAN, 1500);
        putSell(Items.CRYING_OBSIDIAN, 3000);
        putSell(Items.COBBLESTONE, 5);
        putSell(Items.DIRT, 4);
        putSell(Items.SAND, 8);
        putSell(Items.GRAVEL, 6);
        putSell(Items.STONE, 10);
        putSell(Items.DEEPSLATE, 12);
        putSell(Items.BLACKSTONE, 15);
        putSell(Items.END_STONE, 20);
        putSell(Items.OAK_LOG, 30);
        putSell(Items.SPRUCE_LOG, 30);
        putSell(Items.BIRCH_LOG, 30);
        putSell(Items.JUNGLE_LOG, 40);
        putSell(Items.MANGROVE_LOG, 40);
        putSell(Items.CHERRY_LOG, 40);
        putSell(Items.OAK_PLANKS, 7);
        putSell(Items.SPRUCE_PLANKS, 7);
        putSell(Items.BIRCH_PLANKS, 7);
        putSell(Items.JUNGLE_PLANKS, 7);
        putSell(Items.ACACIA_PLANKS, 7);
        putSell(Items.DARK_OAK_PLANKS, 7);
        putSell(Items.MANGROVE_PLANKS, 7);
        putSell(Items.CHERRY_PLANKS, 7);
        putSell(Items.STONE_BRICKS, 20);
        putSell(Items.GLASS, 15);
        putSell(Items.TUFF, 8);
        putSell(Items.CALCITE, 10);
        putSell(Items.DIORITE, 5);
        putSell(Items.ANDESITE, 5);
        putSell(Items.GRANITE, 5);
        putSell(Items.HOPPER, 8500);
        putSell(Items.OBSERVER, 4500);
        putSell(Items.STICKY_PISTON, 2500);
        putSell(Items.PISTON, 1500);
        putSell(Items.REPEATER, 400);
        putSell(Items.COMPARATOR, 850);
        putSell(Items.DISPENSER, 600);
        putSell(Items.BUCKET, 3000);
        putSell(Items.LAVA_BUCKET, 4500);
        putSell(Items.WATER_BUCKET, 3500);
        putSell(Items.SADDLE, 15000);
        putSell(Items.NAME_TAG, 12000);
        putSell(Items.MUSIC_DISC_13, 5000);
        putSell(Items.MUSIC_DISC_CAT, 5000);
        putSell(Items.MUSIC_DISC_BLOCKS, 5000);
        putSell(Items.MUSIC_DISC_CHIRP, 5000);
        putSell(Items.MUSIC_DISC_FAR, 5000);
        putSell(Items.MUSIC_DISC_MALL, 5000);
        putSell(Items.MUSIC_DISC_MELLOHI, 5000);
        putSell(Items.MUSIC_DISC_STAL, 5000);
        putSell(Items.MUSIC_DISC_STRAD, 5000);
        putSell(Items.MUSIC_DISC_WARD, 5000);
        putSell(Items.MUSIC_DISC_11, 5000);
        putSell(Items.MUSIC_DISC_WAIT, 5000);
        putSell(Items.MUSIC_DISC_OTHERSIDE, 5000);
        putSell(Items.MUSIC_DISC_PIGSTEP, 5000);
        putSell(Items.MUSIC_DISC_5, 5000);
        putSell(Items.MUSIC_DISC_RELIC, 5000);

        CommandRegistrationCallback.EVENT.register((dispatcher, access, selection) -> {
            // /ah and subcommands (same as original)
            dispatcher.register(Commands.literal("ah")
                .executes(c -> openAuction(c.getSource().getPlayerOrException(), ""))
                .then(Commands.literal("sell")
                    .then(Commands.argument("price", IntegerArgumentType.integer(1))
                        .executes(c -> sellToAh(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "price")))))
                .then(Commands.literal("balance")
                    .executes(c -> balance(c.getSource().getPlayerOrException())))
                .then(Commands.argument("item", StringArgumentType.word())
                    .executes(c -> openAuction(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "item")))));

            dispatcher.register(Commands.literal("shop")
                .executes(c -> openShop(c.getSource().getPlayerOrException())));

            dispatcher.register(Commands.literal("sell")
                .executes(c -> openSellToServer(c.getSource().getPlayerOrException())));

            // NEW: /pay <player> <amount>
            dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(c -> pay(c.getSource().getPlayerOrException(),
                            StringArgumentType.getString(c, "player"),
                            IntegerArgumentType.getInteger(c, "amount"))))));
        });

        ServerTickEvents.END_SERVER_TICK.register(KelpsAhMod::tickScoreboard);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Auto-save every 5 minutes (6000 ticks)
            if (++saveCounter >= 6000) {
                saveCounter = 0;
                saveData();
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            dataDir = FabricLoader.getInstance().getConfigDir().resolve("kelps_ah");
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadData();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            saveData();
            currentServer = null;
        });
    }

    private static void loadData() {
        // Load balances
        Path balFile = dataDir.resolve("balances.txt");
        if (Files.exists(balFile)) {
            try (BufferedReader br = Files.newBufferedReader(balFile)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        try {
                            UUID uuid = UUID.fromString(parts[0]);
                            int amount = Integer.parseInt(parts[1]);
                            BALANCES.put(uuid, amount);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load nextId
        Path idFile = dataDir.resolve("nextid.txt");
        if (Files.exists(idFile)) {
            try {
                String s = Files.readString(idFile).trim();
                nextId = Long.parseLong(s);
            } catch (Exception ignored) {}
        }

        // Load listings (itemId:count:price:sellerUUID:sellerName)
        // Note: full NBT/enchants not preserved for simplicity & reliability
        Path listFile = dataDir.resolve("listings.txt");
        LISTINGS.clear();
        if (Files.exists(listFile)) {
            try (BufferedReader br = Files.newBufferedReader(listFile)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(":", 5);
                    if (parts.length >= 5) {
                        try {
                            String itemId = parts[0];
                            int count = Integer.parseInt(parts[1]);
                            int price = Integer.parseInt(parts[2]);
                            UUID seller = UUID.fromString(parts[3]);
                            String sellerName = parts[4];
                            ResourceLocation rl = ResourceLocation.tryParse(itemId);
                            if (rl != null) {
                                Item item = BuiltInRegistries.ITEM.get(rl);
                                if (item != null && item != Items.AIR) {
                                    ItemStack stack = new ItemStack(item, Math.max(1, Math.min(count, 64)));
                                    LISTINGS.add(new Listing(nextId++, seller, sellerName, stack, price));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("[KelpsAH] Loaded " + BALANCES.size() + " balances and " + LISTINGS.size() + " listings.");
    }

    private static void saveData() {
        if (dataDir == null) return;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Save balances
        Path balFile = dataDir.resolve("balances.txt");
        try (BufferedWriter bw = Files.newBufferedWriter(balFile)) {
            bw.write("# Kelps balances - UUID:amount\n");
            for (Map.Entry<UUID, Integer> e : BALANCES.entrySet()) {
                bw.write(e.getKey().toString() + ":" + e.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save nextId
        try {
            Files.writeString(dataDir.resolve("nextid.txt"), String.valueOf(nextId));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save ALL AH listings so they survive server restarts
        Path listFile = dataDir.resolve("listings.txt");
        try (BufferedWriter bw = Files.newBufferedWriter(listFile)) {
            bw.write("# AH listings - id:itemId:count:price:sellerUUID:sellerName\n");
            for (Listing l : LISTINGS) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(l.stack.getItem());
                String itemId = key != null ? key.toString() : "minecraft:air";
                String safeName = l.sellerName.replace(":", "_").replace("\n", " ");
                bw.write(l.id + ":" + itemId + ":" + l.stack.getCount() + ":" + l.price + ":" + l.seller.toString() + ":" + safeName + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[KelpsAH] Saved data (" + BALANCES.size() + " balances, " + LISTINGS.size() + " AH listings).");

    private static void tickScoreboard(final MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        final ServerScoreboard board = server.getScoreboard();
        Objective obj = board.getObjective("kelps_balance");
        if (obj == null) {
            obj = board.addObjective("kelps_balance", ObjectiveCriteria.DUMMY, (Component)Component.literal("Kelps").withStyle(ChatFormatting.GREEN), ObjectiveCriteria.RenderType.INTEGER, false, (NumberFormat)null);
        }
        board.setDisplayObjective(DisplaySlot.SIDEBAR, obj);
        for (final ServerPlayer p : server.getPlayerList().getPlayers()) {
            final ScoreAccess score = board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(p.getName().getString()), obj);
            score.set(get((Player)p));
        }
    }

    private static int get(final Player p) {
        return BALANCES.computeIfAbsent(p.getUUID(), id -> 1000);
    }

    private static void give(final Player p, final int n) {
        BALANCES.merge(p.getUUID(), n, Integer::sum);
        // Every balance change is kept in memory for ALL players (online + offline)
        // and written to disk on auto-save / stop / AH actions
    }

    private static boolean take(final Player p, final int n) {
        if (get(p) < n) {
            return false;
        }
        BALANCES.put(p.getUUID(), get(p) - n);
        return true;
    }

    private static int balance(final ServerPlayer p) {
        p.sendSystemMessage((Component)Component.literal("You have " + get((Player)p) + " Kelps.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    // NEW /pay command
    private static int pay(final ServerPlayer sender, final String targetName, final int amount) {
        if (amount <= 0) {
            sender.sendSystemMessage(Component.literal("Amount must be positive.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!take((Player)sender, amount)) {
            sender.sendSystemMessage(Component.literal("You don't have enough Kelps.").withStyle(ChatFormatting.RED));
            return 0;
        }
        ServerPlayer target = null;
        if (currentServer != null) {
            target = currentServer.getPlayerList().getPlayerByName(targetName);
        }
        if (target == null) {
            // Refund if player not online (or allow offline? for simplicity require online)
            give((Player)sender, amount);
            sender.sendSystemMessage(Component.literal("Player \"" + targetName + "\" is not online.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            give((Player)sender, amount);
            sender.sendSystemMessage(Component.literal("You cannot pay yourself.").withStyle(ChatFormatting.RED));
            return 0;
        }
        give((Player)target, amount);
        sender.sendSystemMessage(Component.literal("Paid " + amount + " Kelps to " + target.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.literal("You received " + amount + " Kelps from " + sender.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int sellToAh(final ServerPlayer p, final int price) {
        final ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty()) {
            p.sendSystemMessage((Component)Component.literal("Hold an item to list on the AH.").withStyle(ChatFormatting.RED));
            return 0;
        }
        final ItemStack copy = hand.copy();
        hand.setCount(0);
        LISTINGS.add(new Listing(nextId++, p.getUUID(), p.getName().getString(), copy, price));
        p.sendSystemMessage((Component)Component.literal("Listed for " + price + " Kelps on the Auction House.").withStyle(ChatFormatting.GREEN));
        saveData(); // persist immediately on list
        return 1;
    }

    private static int openAuction(final ServerPlayer p, final String query) {
        final String q = query.toLowerCase(Locale.ROOT);
        final List<Listing> found = LISTINGS.stream()
            .filter(l -> q.isEmpty() || BuiltInRegistries.ITEM.getKey(l.stack.getItem()).getPath().contains(q)
                || l.stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q))
            .sorted(Comparator.comparingInt(l -> l.price))
            .toList();
        p.openMenu((MenuProvider)new SimpleMenuProvider((id, inv, player) -> new AuctionMenu(id, inv, found),
            (Component)Component.literal(query.isEmpty() ? "Auction House" : ("Auction House: " + query))));
        return 1;
    }

    private static int openShop(final ServerPlayer p) {
        p.openMenu((MenuProvider)new SimpleMenuProvider((id, inv, player) -> new ServerShopMenu(id, inv),
            (Component)Component.literal("Server Shop - Buy Items")));
        return 1;
    }

    private static int openSellToServer(final ServerPlayer p) {
        p.openMenu((MenuProvider)new SimpleMenuProvider((id, inv, player) -> new SellToServerMenu(id, inv),
            (Component)Component.literal("Sell to Server")));
        return 1;
    }

    private static void buyFromShop(final ServerPlayer buyer, final ShopOffer offer) {
        if (!take((Player)buyer, offer.price)) {
            buyer.sendSystemMessage((Component)Component.literal("You need " + offer.price + " Kelps.").withStyle(ChatFormatting.RED));
            return;
        }
        final ItemStack copy = offer.stack.copy();
        if (!buyer.getInventory().add(copy)) {
            buyer.drop(copy, false);
        }
        buyer.sendSystemMessage((Component)Component.literal("Bought " + offer.displayName + " for " + offer.price + " Kelps.").withStyle(ChatFormatting.GREEN));
    }

    private static void buyAh(final ServerPlayer buyer, final Listing l) {
        if (!LISTINGS.contains(l)) {
            return;
        }
        // Clicking your OWN listing = cancel & return the item
        if (l.seller.equals(buyer.getUUID())) {
            LISTINGS.remove(l);
            final ItemStack copy = l.stack.copy();
            if (!buyer.getInventory().add(copy)) {
                buyer.drop(copy, false);
            }
            buyer.sendSystemMessage(Component.literal("Cancelled your listing. Item returned.").withStyle(ChatFormatting.YELLOW));
            buyer.closeContainer();
            saveData();
            return;
        }
        if (!take((Player)buyer, l.price)) {
            buyer.sendSystemMessage((Component)Component.literal("You need " + l.price + " Kelps.").withStyle(ChatFormatting.RED));
            return;
        }
        LISTINGS.remove(l);
        final ItemStack copy = l.stack.copy();
        if (!buyer.getInventory().add(copy)) {
            buyer.drop(copy, false);
        }
        final ServerPlayer seller = buyer.level().getServer().getPlayerList().getPlayer(l.seller);
        if (seller != null) {
            give((Player)seller, l.price);
            seller.sendSystemMessage((Component)Component.literal("Your AH listing sold for " + l.price + " Kelps!").withStyle(ChatFormatting.GREEN));
        } else {
            // Offline seller still gets the money - balance is saved for everyone
            BALANCES.merge(l.seller, l.price, Integer::sum);
        }
        buyer.sendSystemMessage((Component)Component.literal("Bought the item for " + l.price + " Kelps.").withStyle(ChatFormatting.GREEN));
        buyer.closeContainer();
        saveData(); // persist after sale (all balances including offline)
    }

    private static void sellContentsToServer(final ServerPlayer p, final Container c) {
        int total = 0;
        int sold = 0;
        for (int i = 0; i < 45; ++i) {
            final ItemStack stack = c.getItem(i);
            if (!stack.isEmpty()) {
                final int priceEach = getSellPrice(stack.getItem());
                final int count = stack.getCount();
                total += priceEach * count;
                sold += count;
                stack.setCount(0);
            }
        }
        if (total > 0) {
            give((Player)p, total);
            p.sendSystemMessage((Component)Component.literal("Sold " + sold + " item(s) to the server for " + total + " Kelps.").withStyle(ChatFormatting.GREEN));
        } else {
            p.sendSystemMessage((Component)Component.literal("No items to sell.").withStyle(ChatFormatting.RED));
        }
    }

    private static final class ShopOffer {
        final ItemStack stack;
        final int price;
        final String displayName;

        ShopOffer(final ItemStack stack, final int price, final String displayName) {
            this.stack = stack;
            this.price = price;
            this.displayName = displayName;
        }
    }

    private static final class Listing {
        final long id;
        final UUID seller;
        final String sellerName;
        final ItemStack stack;
        final int price;

        Listing(final long id, final UUID seller, final String sellerName, final ItemStack stack, final int price) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.stack = stack;
            this.price = price;
        }
    }

    // ========== GUIs kept EXACTLY the same as original ==========

    private static final class AuctionMenu extends ChestMenu {
        private final List<Listing> listings;

        private AuctionMenu(final int id, final Inventory inventory, final List<Listing> listings) {
            super(MenuType.GENERIC_9x6, id, inventory, makeContainer(listings, inventory.player.getUUID()), 6);
            this.listings = listings;
        }

        private static Container makeContainer(final List<Listing> listings, final UUID viewer) {
            final SimpleContainer c = new SimpleContainer(54);
            for (int i = 0; i < listings.size() && i < 54; ++i) {
                final Listing l = listings.get(i);
                final ItemStack s = l.stack.copy();
                if (l.seller.equals(viewer)) {
                    s.set(DataComponents.CUSTOM_NAME, (Object)Component.literal(l.stack.getHoverName().getString() + " • " + l.price + " Kelps  [YOUR LISTING - Click to CANCEL]").withStyle(ChatFormatting.YELLOW));
                } else {
                    s.set(DataComponents.CUSTOM_NAME, (Object)Component.literal(l.stack.getHoverName().getString() + " • " + l.price + " Kelps  (by " + l.sellerName + ")").withStyle(ChatFormatting.GOLD));
                }
                c.setItem(i, s);
            }
            return (Container)c;
        }

        public void clicked(final int slot, final int button, final ContainerInput input, final Player player) {
            if (slot >= 0 && slot < 54 && input == ContainerInput.PICKUP && player instanceof ServerPlayer) {
                final ServerPlayer sp = (ServerPlayer)player;
                if (slot < this.listings.size()) {
                    KelpsAhMod.buyAh(sp, this.listings.get(slot));
                    return;
                }
            }
            if (slot >= 0 && slot < 54) {
                return;
            }
            super.clicked(slot, button, input, player);
        }
    }

    private static final class ServerShopMenu extends ChestMenu {
        private final List<ShopOffer> offers;

        private ServerShopMenu(final int id, final Inventory inventory) {
            super(MenuType.GENERIC_9x6, id, inventory, makeContainer(), 6);
            this.offers = KelpsAhMod.buildShopOffers();
            final SimpleContainer c = (SimpleContainer)this.getContainer();
            for (int i = 0; i < this.offers.size() && i < 45; ++i) {
                final ShopOffer o = this.offers.get(i);
                final ItemStack s = o.stack.copy();
                s.set(DataComponents.CUSTOM_NAME, (Object)Component.literal(o.displayName + " • " + o.price + " Kelps").withStyle(ChatFormatting.AQUA));
                c.setItem(i, s);
            }
            final ItemStack info = new ItemStack((ItemLike)Items.PAPER);
            info.set(DataComponents.CUSTOM_NAME, (Object)Component.literal("Click an item to buy it from the server").withStyle(ChatFormatting.YELLOW));
            c.setItem(49, info);
        }

        private static Container makeContainer() {
            return (Container)new SimpleContainer(54);
        }

        public void clicked(final int slot, final int button, final ContainerInput input, final Player player) {
            if (slot >= 0 && slot < this.offers.size() && input == ContainerInput.PICKUP && player instanceof ServerPlayer) {
                final ServerPlayer sp = (ServerPlayer)player;
                KelpsAhMod.buyFromShop(sp, this.offers.get(slot));
                return;
            }
            if (slot >= 0 && slot < 54) {
                return;
            }
            super.clicked(slot, button, input, player);
        }
    }

    private static final class SellToServerMenu extends ChestMenu {
        private final SimpleContainer sellContainer;

        private SellToServerMenu(final int id, final Inventory inventory) {
            super(MenuType.GENERIC_9x6, id, inventory, (Container)new SimpleContainer(54), 6);
            this.sellContainer = (SimpleContainer)this.getContainer();
            final ItemStack confirm = new ItemStack((ItemLike)Items.EMERALD_BLOCK);
            confirm.set(DataComponents.CUSTOM_NAME, (Object)Component.literal("CLICK TO SELL ALL → Get Kelps").withStyle(ChatFormatting.GREEN));
            this.sellContainer.setItem(49, confirm);
            final ItemStack info = new ItemStack((ItemLike)Items.PAPER);
            info.set(DataComponents.CUSTOM_NAME, (Object)Component.literal("Put items above. Unlisted items = 10 Kelps each").withStyle(ChatFormatting.YELLOW));
            this.sellContainer.setItem(53, info);
        }

        public void clicked(final int slot, final int button, final ContainerInput input, final Player player) {
            if (slot == 49 && input == ContainerInput.PICKUP && player instanceof ServerPlayer) {
                final ServerPlayer sp = (ServerPlayer)player;
                KelpsAhMod.sellContentsToServer(sp, (Container)this.sellContainer);
                for (int i = 0; i < 45; ++i) {
                    this.sellContainer.setItem(i, ItemStack.EMPTY);
                }
                sp.closeContainer();
                return;
            }
            if (slot == 49 || slot == 53) {
                return;
            }
            super.clicked(slot, button, input, player);
        }

        public void removed(final Player player) {
            if (player instanceof final ServerPlayer sp) {
                for (int i = 0; i < 45; ++i) {
                    final ItemStack s = this.sellContainer.getItem(i);
                    if (!s.isEmpty()) {
                        if (!sp.getInventory().add(s.copy())) {
                            sp.drop(s.copy(), false);
                        }
                        this.sellContainer.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
            super.removed(player);
        }
    }
}
