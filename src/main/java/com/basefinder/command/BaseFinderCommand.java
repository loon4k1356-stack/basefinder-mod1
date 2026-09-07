package com.basefinder.command;

import com.basefinder.BaseFinderClient;
import com.basefinder.config.ConfigManager;
import com.basefinder.scanner.BlockScanner;
import com.basefinder.scanner.ScanResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public class BaseFinderCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("basefinder")
                .then(ClientCommandManager.literal("start")
                    .executes(BaseFinderCommand::startScanner))
                .then(ClientCommandManager.literal("stop")
                    .executes(BaseFinderCommand::stopScanner))
                .then(ClientCommandManager.literal("mode")
                    .then(ClientCommandManager.literal("lite")
                        .executes(BaseFinderCommand::loadLite))
                    .then(ClientCommandManager.literal("full")
                        .executes(BaseFinderCommand::loadFull)))
                .then(ClientCommandManager.literal("list")
                    .executes(BaseFinderCommand::listFound))
                .then(ClientCommandManager.literal("radius")
                    .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(16, 2000))
                        .executes(BaseFinderCommand::setRadius)))
                .then(ClientCommandManager.literal("clear")
                    .executes(BaseFinderCommand::clearFound))
                .then(ClientCommandManager.literal("status")
                    .executes(BaseFinderCommand::showStatus))
                .then(ClientCommandManager.literal("cfg")
                    .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::saveNamedConfig)))
                    .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::loadNamedConfig)))
                    .then(ClientCommandManager.literal("list")
                        .executes(BaseFinderCommand::listConfigs))
                    .then(ClientCommandManager.literal("delete")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::deleteConfig)))
                    .then(ClientCommandManager.literal("current")
                        .executes(BaseFinderCommand::showCurrentConfig)))
                .then(ClientCommandManager.literal("help")
                    .executes(BaseFinderCommand::showHelp))
                .executes(BaseFinderCommand::showHelp)
        );

        dispatcher.register(
            ClientCommandManager.literal("bf")
                .then(ClientCommandManager.literal("start")
                    .executes(BaseFinderCommand::startScanner))
                .then(ClientCommandManager.literal("stop")
                    .executes(BaseFinderCommand::stopScanner))
                .then(ClientCommandManager.literal("mode")
                    .then(ClientCommandManager.literal("lite")
                        .executes(BaseFinderCommand::loadLite))
                    .then(ClientCommandManager.literal("full")
                        .executes(BaseFinderCommand::loadFull)))
                .then(ClientCommandManager.literal("list")
                    .executes(BaseFinderCommand::listFound))
                .then(ClientCommandManager.literal("radius")
                    .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(16, 2000))
                        .executes(BaseFinderCommand::setRadius)))
                .then(ClientCommandManager.literal("cfg")
                    .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::saveNamedConfig)))
                    .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::loadNamedConfig)))
                    .then(ClientCommandManager.literal("list")
                        .executes(BaseFinderCommand::listConfigs))
                    .then(ClientCommandManager.literal("delete")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(BaseFinderCommand::deleteConfig))))
                .executes(BaseFinderCommand::showHelp)
        );
    }

    private static int startScanner(CommandContext<FabricClientCommandSource> context) {
        BlockScanner scanner = BaseFinderClient.scanner;
        if (scanner.getSelectedBlocks().isEmpty()) {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Нет выбранных блоков! Нажмите [O]."));
            return 0;
        }
        if (scanner.isRunning()) {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Сканер уже запущен!"));
            return 0;
        }
        scanner.start();
        String mode = scanner.isLiteMode() ? "LITE (Y < " + scanner.getLiteHeightLimit() + ")" : "FULL";
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Сканер запущен! Режим: " + mode + ". Радиус: " + scanner.getScanRadius()));
        return 1;
    }

    private static int stopScanner(CommandContext<FabricClientCommandSource> context) {
        BlockScanner scanner = BaseFinderClient.scanner;
        if (!scanner.isRunning()) {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Сканер не запущен."));
            return 0;
        }
        scanner.stop();
        context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Сканер остановлен. Найдено: " + scanner.getFoundBlocks().size()));
        return 1;
    }

    private static int loadLite(CommandContext<FabricClientCommandSource> context) {
        BaseFinderClient.scanner.setLiteMode(true);
        BaseFinderClient.scanner.setLiteHeightLimit(30);
        context.getSource().sendFeedback(Text.literal("§b[BaseFinder] LITE режим! Поиск ниже Y=30."));
        return 1;
    }

    private static int loadFull(CommandContext<FabricClientCommandSource> context) {
        BaseFinderClient.scanner.setLiteMode(false);
        context.getSource().sendFeedback(Text.literal("§b[BaseFinder] FULL режим!"));
        return 1;
    }

    private static int listFound(CommandContext<FabricClientCommandSource> context) {
        List<ScanResult> found = BaseFinderClient.scanner.getFoundBlocks();
        if (found.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Ничего не найдено."));
            return 1;
        }
        context.getSource().sendFeedback(Text.literal("§6[BaseFinder] === Найдено " + found.size() + " блоков ==="));
        int limit = Math.min(found.size(), 20);
        for (int i = 0; i < limit; i++) {
            context.getSource().sendFeedback(Text.literal("  §f" + (i + 1) + ". " + found.get(i).getFormattedInfo()));
        }
        if (found.size() > 20) {
            context.getSource().sendFeedback(Text.literal("  §7... и ещё " + (found.size() - 20)));
        }
        return 1;
    }

    private static int setRadius(CommandContext<FabricClientCommandSource> context) {
        int radius = IntegerArgumentType.getInteger(context, "blocks");
        BaseFinderClient.scanner.setScanRadius(radius);
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Радиус: " + radius + " блоков"));
        return 1;
    }

    private static int clearFound(CommandContext<FabricClientCommandSource> context) {
        BaseFinderClient.scanner.getFoundBlocks().clear();
        context.getSource().sendFeedback(Text.literal("§e[BaseFinder] Результаты очищены."));
        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        BlockScanner scanner = BaseFinderClient.scanner;
        String status = scanner.isRunning() ? "§aРАБОТАЕТ" : "§cОСТАНОВЛЕН";
        String mode = scanner.isLiteMode() ? "§bLITE (Y<" + scanner.getLiteHeightLimit() + ")" : "§bFULL";
        context.getSource().sendFeedback(Text.literal("§6[BaseFinder] === Статус ==="));
        context.getSource().sendFeedback(Text.literal("  §fСтатус: " + status));
        context.getSource().sendFeedback(Text.literal("  §fРежим: " + mode));
        context.getSource().sendFeedback(Text.literal("  §fРадиус: " + scanner.getScanRadius()));
        context.getSource().sendFeedback(Text.literal("  §fБлоков выбрано: " + scanner.getSelectedBlocks().size()));
        context.getSource().sendFeedback(Text.literal("  §fНайдено: " + scanner.getFoundBlocks().size()));
        context.getSource().sendFeedback(Text.literal("  §fЧанков: " + scanner.getScannedChunks() + "/" + scanner.getTotalChunks()));
        context.getSource().sendFeedback(Text.literal("  §fКонфиг: §e" + ConfigManager.getCurrentConfigName()));
        return 1;
    }

    private static int saveNamedConfig(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        if (ConfigManager.saveConfig(name)) {
            ConfigManager.setCurrentConfigName(name);
            context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Конфиг сохранён как '§e" + name + "§a'"));
        } else {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Ошибка сохранения!"));
        }
        return 1;
    }

    private static int loadNamedConfig(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        if (ConfigManager.loadConfig(name)) {
            context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Конфиг '§e" + name + "§a' загружен!"));
            context.getSource().sendFeedback(Text.literal("§7  Радиус: " + BaseFinderClient.scanner.getScanRadius() + " | Блоков: " + BaseFinderClient.scanner.getSelectedBlocks().size()));
        } else {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Конфиг '§e" + name + "§c' не найден!"));
        }
        return 1;
    }

    private static int listConfigs(CommandContext<FabricClientCommandSource> context) {
        List<String> configs = ConfigManager.listConfigs();
        String currentName = ConfigManager.getCurrentConfigName();
        context.getSource().sendFeedback(Text.literal("§6[BaseFinder] === Конфиги ==="));
        if (configs.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("  §7Нет конфигов"));
        } else {
            for (String name : configs) {
                if (name.equals(currentName)) {
                    context.getSource().sendFeedback(Text.literal("  §a▶ " + name + " §7(текущий)"));
                } else {
                    context.getSource().sendFeedback(Text.literal("  §f  " + name));
                }
            }
        }
        context.getSource().sendFeedback(Text.literal("§7/bf cfg load <имя>"));
        return 1;
    }

    private static int deleteConfig(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        if (name.equals("default")) {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Нельзя удалить default!"));
            return 0;
        }
        if (ConfigManager.deleteConfig(name)) {
            context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Конфиг '§e" + name + "§a' удалён!"));
        } else {
            context.getSource().sendError(Text.literal("§c[BaseFinder] Конфиг не найден!"));
        }
        return 1;
    }

    private static int showCurrentConfig(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§a[BaseFinder] Текущий конфиг: §e" + ConfigManager.getCurrentConfigName()));
        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6═══════ BaseFinder ═══════"));
        context.getSource().sendFeedback(Text.literal("§e[Управление]"));
        context.getSource().sendFeedback(Text.literal("  §f[O] §7- GUI выбора блоков"));
        context.getSource().sendFeedback(Text.literal("  §f[H] §7- Старт/Стоп сканера"));
        context.getSource().sendFeedback(Text.literal("§e[Команды]"));
        context.getSource().sendFeedback(Text.literal("  §f/bf start §7- Запустить"));
        context.getSource().sendFeedback(Text.literal("  §f/bf stop §7- Остановить"));
        context.getSource().sendFeedback(Text.literal("  §f/bf mode lite §7- Режим Y<30"));
        context.getSource().sendFeedback(Text.literal("  §f/bf mode full §7- Полный режим"));
        context.getSource().sendFeedback(Text.literal("  §f/bf list §7- Найденные блоки"));
        context.getSource().sendFeedback(Text.literal("  §f/bf radius <N> §7- Радиус"));
        context.getSource().sendFeedback(Text.literal("  §f/bf status §7- Статус"));
        context.getSource().sendFeedback(Text.literal("§e[Конфиги]"));
        context.getSource().sendFeedback(Text.literal("  §f/bf cfg save <имя> §7- Сохранить"));
        context.getSource().sendFeedback(Text.literal("  §f/bf cfg load <имя> §7- Загрузить"));
        context.getSource().sendFeedback(Text.literal("  §f/bf cfg list §7- Список"));
        context.getSource().sendFeedback(Text.literal("  §f/bf cfg delete <имя> §7- Удалить"));
        context.getSource().sendFeedback(Text.literal("§7Текущий: §e" + ConfigManager.getCurrentConfigName()));
        return 1;
    }
}
