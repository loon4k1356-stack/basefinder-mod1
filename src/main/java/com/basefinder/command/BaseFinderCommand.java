package com.basefinder.command;

import com.basefinder.BaseFinderClient;
import com.basefinder.scanner.BlockScanner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BaseFinderCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, ServerCommandSource.RegistrationEnvironment environment) {
        dispatcher.register(literal("basefinder")
            .requires(source -> source.hasPermissionLevel(2))
            .then(literal("start").executes(context -> {
                BaseFinderClient.scanner.start();
                context.getSource().sendFeedback(() -> Text.literal("§a[BaseFinder] Scanner started!"), false);
                return 1;
            }))
            .then(literal("stop").executes(context -> {
                BaseFinderClient.scanner.stop();
                context.getSource().sendFeedback(() -> Text.literal("§c[BaseFinder] Scanner stopped!"), false);
                return 1;
            }))
            .then(literal("radius").argument("value", IntegerArgumentType.integer(10, 200)).executes(context -> {
                int radius = IntegerArgumentType.getInteger(context, "value");
                BaseFinderClient.scanner.setScanRadius(radius);
                context.getSource().sendFeedback(() -> Text.literal("§7[BaseFinder] Radius set to: " + radius), false);
                return 1;
            }))
            .then(literal("lite").argument("enabled", BoolArgumentType.bool()).executes(context -> {
                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                BaseFinderClient.scanner.setLiteMode(enabled);
                context.getSource().sendFeedback(() -> Text.literal("§7[BaseFinder] Lite mode: " + (enabled ? "ON" : "OFF")), false);
                return 1;
            }))
            .then(literal("status").executes(context -> {
                BlockScanner scanner = BaseFinderClient.scanner;
                boolean running = scanner.isRunning();
                String status = running ? "§aRUNNING" : "§cIDLE";
                context.getSource().sendFeedback(() -> Text.literal("§7--- BaseFinder Status ---")
                    .append(Text.literal("\n§7Status: " + status))
                    .append(Text.literal("\n§7Mode: " + (scanner.isLiteMode() ? "LITE" : "FULL")))
                    .append(Text.literal("\n§7Radius: " + scanner.getScanRadius()))
                    .append(Text.literal("\n§7Found: " + scanner.getFoundBlocks().size()))
                    .append(Text.literal("\n§7Progress: " + scanner.getScannedChunks() + "/" + scanner.getTotalChunks())), false);
                return 1;
            }))
        );
    }
}
