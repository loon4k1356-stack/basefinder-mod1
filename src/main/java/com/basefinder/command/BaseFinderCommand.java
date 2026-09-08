package com.basefinder.command;

import com.basefinder.BaseFinderClient;
import com.basefinder.scanner.BlockScanner;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.List;

public class BaseFinderCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = LiteralArgumentBuilder.<ServerCommandSource>literal("basefinder")
            .requires(source -> source.hasPermissionLevel(0)) // Разрешение для всех
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("start")
                .executes(BaseFinderCommand::startScan))
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("stop")
                .executes(BaseFinderCommand::stopScan))
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("status")
                .executes(BaseFinderCommand::showStatus))
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("radius")
                .then(com.mojang.brigadier.arguments.IntegerArgumentType.argument("radius", 10, 200)
                    .executes(ctx -> setRadius(ctx, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("clear")
                .executes(BaseFinderCommand::clearBlocks))
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("lite")
                .then(com.mojang.brigadier.arguments.BoolArgumentType.argument("enabled", true)
                    .executes(ctx -> setLiteMode(ctx, com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "enabled")))));

        dispatcher.register(builder);
    }

    private static int startScan(CommandContext<ServerCommandSource> context) {
        if (BaseFinderClient.scanner == null) {
            context.getSource().sendFeedback(Text.literal("§c[BaseFinder] Scanner not initialized!"), false);
            return 0;
        }
        
        // В клиентских модах лучше запускать через клиентский поток, но для команды попробуем напрямую
        // Если это вызывает краш, значит команду нужно вызывать только из чата клиента
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
             if (!BaseFinderClient.scanner.isRunning()) {
                 BaseFinderClient.scanner.startScan(); // Используем наш новый метод
                 context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Scan started!"), false);
             } else {
                 context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Already scanning!"), false);
             }
        });
        return 1;
    }

    private static int stopScan(CommandContext<ServerCommandSource> context) {
        if (BaseFinderClient.scanner == null) return 0;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (BaseFinderClient.scanner.isRunning()) {
                BaseFinderClient.scanner.stopScan(); // Используем наш новый метод
                context.getSource().sendFeedback(Text.literal("§c[BaseFinder] Scan stopped."), false);
            } else {
                context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Not running."), false);
            }
        });
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        if (BaseFinderClient.scanner == null) return 0;
        
        BlockScanner scanner = BaseFinderClient.scanner;
        String status = scanner.isRunning() ? "§aRUNNING" : "§cIDLE";
        
        // Получаем количество блоков безопасно
        int foundCount = 0;
        if (scanner.getSelectedBlocks() != null) {
            foundCount = scanner.getSelectedBlocks().size();
        }

        context.getSource().sendFeedback(
            Text.literal("§8--- §bBaseFinder Status §8---\n")
            .append(Text.literal("  §fСтатус: " + status + "\n"))
            .append(Text.literal("  §fРежим: " + (scanner.isLiteMode() ? "LITE" : "FULL") + "\n"))
            .append(Text.literal("  §fРадиус: " + scanner.getScanRadius() + "\n"))
            .append(Text.literal("  §fНайдено блоков: " + foundCount)),
            false
        );
        return 1;
    }

    private static int setRadius(CommandContext<ServerCommandSource> context, int radius) {
        if (BaseFinderClient.scanner == null) return 0;
        BaseFinderClient.scanner.setScanRadius(radius);
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Radius set to " + radius), false);
        return 1;
    }

    private static int clearBlocks(CommandContext<ServerCommandSource> context) {
        if (BaseFinderClient.scanner == null) return 0;
        BaseFinderClient.scanner.clearSelectedBlocks();
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Blocks cleared."), false);
        return 1;
    }

    private static int setLiteMode(CommandContext<ServerCommandSource> context, boolean enabled) {
        if (BaseFinderClient.scanner == null) return 0;
        BaseFinderClient.scanner.setLiteMode(enabled);
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Lite mode " + (enabled ? "enabled" : "disabled")), false);
        return 1;
    }
}
