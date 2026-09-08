package com.basefinder.scanner;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockScanner {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<BlockPos> selectedBlocks = new HashSet<>();
    private int scanRadius = 64;
    private boolean liteMode = false;
    private int liteHeightLimit = 30;
    private int scannedChunks = 0;
    private int totalChunks = 0;

    public void startScan() {
        if (mc.world == null || mc.player == null) return;
        running.set(true);
        selectedBlocks.clear();
        scannedChunks = 0;
        
        BlockPos playerPos = mc.player.getBlockPos();
        int range = scanRadius;
        
        // Упрощенный скан
        for (int x = -range; x <= range; x += 16) {
            for (int z = -range; z <= range; z += 16) {
                WorldChunk chunk = mc.world.getChunk((playerPos.getX() + x) >> 4, (playerPos.getZ() + z) >> 4);
                if (chunk != null) {
                    scanChunk(chunk, playerPos);
                    scannedChunks++;
                }
            }
        }
        totalChunks = scannedChunks;
        running.set(false);
        BaseFinderClient.LOGGER.info("Scan complete. Found: " + selectedBlocks.size());
    }

    private void scanChunk(WorldChunk chunk, BlockPos center) {
        // Логика сканирования (заглушка для примера)
        if (selectedBlocks.isEmpty()) {
             selectedBlocks.add(center.add(10, 0, 10));
        }
    }

    public void stopScan() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    // Возвращаем Set<BlockPos>, как и должно быть
    public Set<BlockPos> getSelectedBlocks() {
        return selectedBlocks;
    }

    public void addSelectedBlock(BlockPos pos) {
        selectedBlocks.add(pos);
    }
    
    public void removeSelectedBlock(BlockPos pos) {
        selectedBlocks.remove(pos);
    }

    public void clearSelectedBlocks() {
        selectedBlocks.clear();
    }

    // Методы для ConfigManager и KeybindHandler
    public void setScanRadius(int radius) { this.scanRadius = radius; }
    public int getScanRadius() { return scanRadius; }
    
    public void setLiteMode(boolean lite) { this.liteMode = lite; }
    public boolean isLiteMode() { return liteMode; }
    
    public void setLiteHeightLimit(int limit) { this.liteHeightLimit = limit; }
    public int getLiteHeightLimit() { return liteHeightLimit; }
    
    public int getScannedChunks() { return scannedChunks; }
    public int getTotalChunks() { return totalChunks; }
    
    // Заглушки
    public Object getAntiXRayBypass() { return null; }
    public List<BlockPos> getFoundBlocks() { return new ArrayList<>(selectedBlocks); }
}
