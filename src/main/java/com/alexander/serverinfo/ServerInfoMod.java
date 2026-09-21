package com.alexander.serverinfo;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ItemName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-only mod that lists installed Fabric mods and active datapacks
 * through a plain vanilla chest GUI, so unmodded clients can view it.
 */
public class ServerInfoMod implements ModInitializer {
    public static final String MOD_ID = "server-info";

    private static final int PAGE_SIZE = 36;

    // Tabs
    private static final int TAB_MODS = 0;
    private static final int TAB_DATAPACKS = 1;

    // Text colors
    private static final String GOLD = "§6";
    private static final String GREEN = "§a";
    private static final String AQUA = "§b";
    private static final String YELLOW = "§e";
    private static final String GRAY = "§7";
    private static final String WHITE = "§f";
    private static final String BOLD = "§l";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            registerCommands(dispatcher)
        );
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("serverinfo")
            .executes(ctx -> open(ctx, TAB_MODS, 0))
            .then(Commands.literal("mods")
                .executes(ctx -> open(ctx, TAB_MODS, 0)))
            .then(Commands.literal("datapacks")
                .executes(ctx -> open(ctx, TAB_DATAPACKS, 0)))
        );
    }

    private static int open(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, int tab, int page) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Only players can use /serverinfo."));
            return 0;
        }

        player.openMenu(new InfoMenuProvider(tab, page));
        return 1;
    }

    private static List<String> mods() {
        return FabricLoader.getInstance().getAllMods().stream()
            .sorted(Comparator.comparing(m -> m.getMetadata().getId(), String.CASE_INSENSITIVE_ORDER))
            .map(m -> {
                var metadata = m.getMetadata();
                return metadata.getId() + GRAY + "  •  " + WHITE + metadata.getName()
                    + GRAY + "  •  " + WHITE + metadata.getVersion().getFriendlyString();
            })
            .toList();
    }

    private static List<String> datapacks(MinecraftServer server) {
        return server.getPackRepository().getSelectedIds().stream()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private static class InfoMenuProvider implements MenuConstructor {
        private final int tab;
        private final int page;

        InfoMenuProvider(int tab, int page) {
            this.tab = tab;
            this.page = page;
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                 net.minecraft.world.entity.player.Player player) {
            return new InfoMenu(containerId, inventory, tab, page);
        }
    }

    private static class InfoMenu extends ChestMenu {
        private final int tab;
        private final int page;

        InfoMenu(int id, net.minecraft.world.entity.player.Inventory inventory, int tab, int page) {
            super(MenuType.GENERIC_6x9, id, inventory, new SimpleContainer(54), 6);
            this.tab = tab;
            this.page = page;
            populate(inventory.player);
        }

        private void populate(net.minecraft.world.entity.player.Player player) {
            Container container = getContainer();
            for (int i = 0; i < 54; i++) {
                container.setItem(i, ItemStack.EMPTY);
            }

            List<String> entries = tab == TAB_MODS
                ? mods()
                : datapacks(((ServerPlayer) player).server);

            int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            int clampedPage = Math.min(page, pages - 1);
            int start = clampedPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, entries.size());

            placeHeader();
            placeTabs();
            placeEntries(container, entries, tab, start, end);
            placeFooter(entries.size(), clampedPage, pages);
        }

        private void placeHeader() {
            Container container = getContainer();
            container.setItem(0, named(Items.BOOK, GOLD + BOLD + "Server Information",
                GRAY + "Server-side only",
                GRAY + "No client mod required"));
        }

        private void placeTabs() {
            Container container = getContainer();
            container.setItem(3, named(Items.COMPASS, GREEN + BOLD + "Installed Mods",
                GRAY + "Run /serverinfo mods to view"));
            container.setItem(5, named(Items.KNOWLEDGE_BOOK, AQUA + BOLD + "Active Datapacks",
                GRAY + "Run /serverinfo datapacks to view"));
        }

        private void placeEntries(Container container, List<String> entries, int tab, int start, int end) {
            Item entryIcon = tab == TAB_MODS ? Items.PAPER : Items.BOOK;
            for (int i = start; i < end; i++) {
                int slot = 9 + (i - start);
                container.setItem(slot, named(entryIcon, WHITE + entries.get(i)));
            }
        }

        private void placeFooter(int total, int page, int pages) {
            Container container = getContainer();

            if (page > 0) {
                container.setItem(45, named(Items.ARROW, YELLOW + "Previous Page",
                    GRAY + "Go to page " + page + " / " + pages));
            }

            container.setItem(49, named(Items.NETHER_STAR,
                GOLD + BOLD + (tab == TAB_MODS ? "MODS" : "DATAPACKS"),
                GRAY + total + " total",
                GRAY + "Page " + (page + 1) + " / " + pages));

            if (page + 1 < pages) {
                container.setItem(53, named(Items.ARROW, YELLOW + "Next Page",
                    GRAY + "Go to page " + (page + 2) + " / " + pages));
            }

            container.setItem(47, named(Items.IRON_SWORD, GREEN + "Mods",
                GRAY + "Use " + WHITE + "/serverinfo mods"));
            container.setItem(51, named(Items.BOOK, AQUA + "Datapacks",
                GRAY + "Use " + WHITE + "/serverinfo datapacks"));
        }

        private ItemStack named(Item item, String name, String... lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(ItemName.NAME, Component.literal(name));
            if (lore.length > 0) {
                List<Component> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(Component.literal(line));
                }
                stack.set(ItemLore.LORE, new ItemLore(lines));
            }
            return stack;
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }
}
