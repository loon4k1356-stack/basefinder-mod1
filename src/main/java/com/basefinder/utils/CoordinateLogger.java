package com.basefinder.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CoordinateLogger {
    private static final Path LOG_DIR = MinecraftClient.getInstance().runDirectory.toPath().resolve("freezdlc_logs");
    private static final List<String> currentSessionLogs = new ArrayList<>();

    public static void logCoordinate(String label, BlockPos pos) {
        String entry = String.format("[%s] %s: X:%d Y:%d Z:%d", new SimpleDateFormat("HH:mm:ss").format(new Date()), label, pos.getX(), pos.getY(), pos.getZ());
        currentSessionLogs.add(entry);
        System.out.println("[freezdlc] " + entry);
        
        // Автосохранение в файл сессии
        saveToFile("session_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log");
    }

    private static void saveToFile(String filename) {
        try {
            Files.createDirectories(LOG_DIR);
            Path filePath = LOG_DIR.resolve(filename);
            Files.write(filePath, currentSessionLogs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportToXaero() {
        try {
            Files.createDirectories(LOG_DIR);
            Path xaeroFile = LOG_DIR.resolve("waypoints_freezdlc.txt");
            
            StringBuilder content = new StringBuilder();
            for (String log : currentSessionLogs) {
                // Парсим лог: [Time] Label: X:x Y:y Z:z
                if (log.contains("X:") && log.contains("Y:") && log.contains("Z:")) {
                    String[] parts = log.split(": ");
                    if (parts.length >= 2) {
                        String label = parts[1].split(",")[0].trim();
                        String coordsPart = parts[1].substring(parts[1].indexOf("X:"));
                        String[] coords = coordsPart.replace("X:", "").replace("Y:", "").replace("Z:", "").split(",");
                        
                        int x = Integer.parseInt(coords[0].trim());
                        int y = Integer.parseInt(coords[1].trim());
                        int z = Integer.parseInt(coords[2].trim());

                        // Формат Xaero: Name|X|Y|Z|Color|Dimension|Ang|Icon
                        content.append(String.format("%s|%d|%d|%d|0:red|0|0|Waypoint\n", label, x, y, z));
                    }
                }
            }
            Files.writeString(xaeroFile, content.toString());
            System.out.println("[freezdlc] Экспорт в Xaero выполнен: " + xaeroFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
