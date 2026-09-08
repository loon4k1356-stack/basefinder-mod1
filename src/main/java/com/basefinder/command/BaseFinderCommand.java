package com.basefinder.command;

import com.basefinder.BaseFinderClient;
import com.basefinder.scanner.BlockScanner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class BaseFinderCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var root = ClientCommandManager.literal("basefinder")
                .then(ClientCommandManager.literal("scan").executes(context -> {
                    if (BaseFinderClient.scanner == null) {
                        context.getSource().sendFeedback(Text.literal("§c[BaseFinder] Scanner not initialized!"));
                        return 0;
                    }
                    if (!BaseFinderClient.scanner.isRunning()) {
                        BaseFinderClient.scanner.startScan();
                        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Scan started!"));
                    } else {
                        context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Already scanning!"));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(context -> {
                    if (BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning()) {
                        BaseFinderClient.scanner.stopScan();
                        context.getSource().sendFeedback(Text.literal("§c[BaseFinder] Scan stopped."));
                    } else {
                        context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Not running."));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(context -> {
                    if (BaseFinderClient.scanner == null) {
                        context.getSource().sendFeedback(Text.literal("§cScanner null"));
                        return 0;
                    }
                    boolean running = BaseFinderClient.scanner.isRunning();
                    int found = BaseFinderClient.scanner.getSelectedBlocks().size(); // Используем список блоков как пример
                    context.getSource().sendFeedback(Text.literal("§fStatus: " + (running ? "§aRunning" : "§cStopped") + " | Blocks: " + found));
                    return 1;
                }))
                .then(ClientCommandManager.literal("radius")
                        .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(10, 200)).executes(context -> {
                            int radius = IntegerArgumentType.getInteger(context, "radius");
                            if (BaseFinderClient.scanner != null) {
                                BaseFinderClient.scanner.setScanRadius(radius);
                                context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Radius set to " + radius));
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.literal("clear").executes(context -> {
                    if (BaseFinderClient.scanner != null) {
                        BaseFinderClient.scanner.clearSelectedBlocks();
                        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Blocks cleared."));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("lite")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            if (BaseFinderClient.scanner != null) {
                                BaseFinderClient.scanner.setLiteMode(enabled);
                                context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Lite mode " + (enabled ? "enabled" : "disabled")));
                            }
                            return 1;
                        })));

        dispatcher.register(root);
    }
}
