package com.alexander.serverinfo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ItemName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServerInfoMod implements ModInitializer {
    public static final String MOD_ID = "server-info";
    private static final int PAGE_SIZE = 36;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            registerCommands(dispatcher)
        );
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("serverinfo")
            .executes(ctx -> open(ctx, 0, 0))
            .then(Commands.literal("mods")
                .executes(ctx -> open(ctx, 0, 0)))
            .then(Commands.literal("datapacks")
                .executes(ctx -> open(ctx, 1, 0)))
        );
    }

    private static int open(CommandContext<CommandSourceStack> ctx, int tab, int page) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Only players can use /serverinfo."));
            return 0;
        }

        InfoMenuProvider provider = new InfoMenuProvider(tab, page);
        player.openMenu(provider);
        return 1;
    }

    private static List<String> mods() {
        return FabricLoader.getInstance().getAllMods().stream()
            .sorted(Comparator.comparing(m -> m.getMetadata().getId(), String.CASE_INSENSITIVE_ORDER))
            .map(m -> {
                var md = m.getMetadata();
                return md.getId() + "  •  " + md.getName() + "  •  " + md.getVersion().getFriendlyString();
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
                                                net.minecraft.world.entity.player player) {
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
            Container c = getContainer();
            for (int i = 0; i < 54; i++) c.setItem(i, ItemStack.EMPTY);

            // Header
            c.setItem(0, named(Items.BOOK, "§6§lServer Information",
                "§7Server-side only", "§7No client mod required"));

            c.setItem(3, named(Items.COMPASS, "§a§lInstalled Mods",
                "§7Click /serverinfo mods in chat to switch"));
            c.setItem(5, named(Items.KNOWLEDGE_BOOK, "§b§lActive Datapacks",
                "§7Click /serverinfo datapacks in chat to switch"));

            List<String> list = tab == 0
                ? mods()
                : datapacks(((ServerPlayer) player).server);

            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, list.size());

            for (int i = start; i < end; i++) {
                String value = list.get(i);
                int slot = 9 + (i - start);
                ItemStack item = named(tab == 0 ? Items.PAPER : Items.BOOK,
                    "§f" + value);
                c.setItem(slot, item);
            }

            int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);

            if (page > 0) {
                c.setItem(45, named(Items.ARROW, "§ePrevious Page",
                    "§7Page " + page + " / " + pages));
            }
            c.setItem(49, named(Items.NETHER_STAR, "§6§l" + (tab == 0 ? "MODS" : "DATAPACKS"),
                "§7" + list.size() + " total", "§7Page " + (page + 1) + " / " + pages));
            if (page + 1 < pages) {
                c.setItem(53, named(Items.ARROW, "§eNext Page",
                    "§7Page " + (page + 2) + " / " + pages));
            }

            c.setItem(47, named(Items.IRON_SWORD, "§aMods",
                "§7Use §f/serverinfo mods"));
            c.setItem(51, named(Items.BOOK, "§bDatapacks",
                "§7Use §f/serverinfo datapacks"));
        }

        private ItemStack named(net.minecraft.world.item.Item item, String name, String... lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(ItemName.NAME, Component.literal(name.replace("§", "§")));
            if (lore.length > 0) {
                List<Component> lines = new ArrayList<>();
                for (String line : lore) lines.add(Component.literal(line));
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
