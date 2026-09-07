package com.basefinder.command;

import com.basefinder.BaseFinderClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class BaseFinderCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /basefinder start
            dispatcher.register(ClientCommandManager.literal("basefinder")
                    .then(ClientCommandManager.literal("start")
                            .executes(BaseFinderCommand::startScanner))
                    .then(ClientCommandManager.literal("stop")
                            .executes(BaseFinderCommand::stopScanner))
                    .then(ClientCommandManager.literal("list")
                            .executes(BaseFinderCommand::listFound))
                    .then(ClientCommandManager.literal("clear")
                            .executes(BaseFinderCommand::clearResults))
                    .then(ClientCommandManager.literal("radius")
                            .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(16, 1000))
                                    .executes(BaseFinderCommand::setRadius)))
                    .then(ClientCommandManager.literal("load")
                            .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                    .executes(BaseFinderCommand::loadMode)))
                    .then(ClientCommandManager.literal("status")
                            .executes(BaseFinderCommand::showStatus))
            );

            // Short alias /bf
            dispatcher.register(ClientCommandManager.literal("bf")
                    .then(ClientCommandManager.literal("start")
                            .executes(BaseFinderCommand::startScanner))
                    .then(ClientCommandManager.literal("stop")
                            .executes(BaseFinderCommand::stopScanner))
                    .then(ClientCommandManager.literal("list")
                            .executes(BaseFinderCommand::listFound))
                    .then(ClientCommandManager.literal("load")
                            .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                    .executes(BaseFinderCommand::loadMode)))
            );
        });
    }

    private static int startScanner(CommandContext<?> context) {
        BaseFinderClient.toggleScanner();
        sendFeedback("§a[BaseFinder] Scanner " + 
                (BaseFinderClient.scanner.isRunning() ? "STARTED" : "STOPPED"));
        return 1;
    }

    private static int stopScanner(CommandContext<?> context) {
        BaseFinderClient.scanner.stop();
        sendFeedback("§c[BaseFinder] Scanner stopped. Found: " + 
                BaseFinderClient.scanner.getFoundBlocks().size() + " blocks");
        return 1;
    }

    private static int listFound(CommandContext<?> context) {
        var found = BaseFinderClient.scanner.getFoundBlocks();
        if (found.isEmpty()) {
            sendFeedback("§e[BaseFinder] No blocks found yet.");
            return 1;
        }
        sendFeedback("§a[BaseFinder] === Found " + found.size() + " blocks ===");
        for (var result : found) {
            sendFeedback("§f" + result.getFormattedInfo());
        }
        return 1;
    }

    private static int clearResults(CommandContext<?> context) {
        BaseFinderClient.scanner.getFoundBlocks().clear();
        sendFeedback("§e[BaseFinder] Results cleared.");
        return 1;
    }

    private static int setRadius(CommandContext<?> context) {
        int radius = IntegerArgumentType.getInteger(context, "blocks");
        BaseFinderClient.scanner.setScanRadius(radius);
        sendFeedback("§a[BaseFinder] Scan radius set to " + radius + " blocks.");
        return 1;
    }

    private static int loadMode(CommandContext<?> context) {
        String mode = StringArgumentType.getString(context, "mode");
        if (mode.equalsIgnoreCase("lite")) {
            BaseFinderClient.scanner.setLiteMode(true);
            BaseFinderClient.scanner.setLiteHeightLimit(30);
            sendFeedback("§a[BaseFinder] LITE mode enabled. Scanning only below Y=30");
        } else if (mode.equalsIgnoreCase("full")) {
            BaseFinderClient.scanner.setLiteMode(false);
            sendFeedback("§a[BaseFinder] FULL mode enabled. Scanning all heights.");
        } else {
            sendFeedback("§c[BaseFinder] Unknown mode: " + mode + ". Use 'lite' or 'full'.");
        }
        return 1;
    }

    private static int showStatus(CommandContext<?> context) {
        var scanner = BaseFinderClient.scanner;
        String status = scanner.isRunning() ? "§aRUNNING" : "§cSTOPPED";
        String mode = scanner.isLiteMode() ? "LITE (Y<" + scanner.getLiteHeightLimit() + ")" : "FULL";
        sendFeedback("§6[BaseFinder] Status: " + status);
        sendFeedback("§6[BaseFinder] Mode: " + mode);
        sendFeedback("§6[BaseFinder] Radius: " + scanner.getScanRadius() + " blocks");
        sendFeedback("§6[BaseFinder] Selected blocks: " + scanner.getSelectedBlocks().size());
        sendFeedback("§6[BaseFinder] Found: " + scanner.getFoundBlocks().size() + " blocks");
        sendFeedback("§6[BaseFinder] Chunks scanned: " + scanner.getScannedChunks() + "/" + scanner.getTotalChunks());
        sendFeedback("§6[BaseFinder] Anti-XRay confirmed: " + scanner.getAntiXRayBypass().getConfirmedBlockCount() + " blocks");
        return 1;
    }

    private static void sendFeedback(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}
