package com.basefinder.scanner;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockScanner {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean running = false;
    private boolean liteMode = false;
    private int scanRadius = 64;
    private int liteHeightLimit = 30;
    
    private final Set<Block> selectedBlocks = new HashSet<>();
    private final List<BlockPos> foundBlocks = new ArrayList<>();
    private final Queue<ChunkPos> chunkQueue = new ConcurrentLinkedQueue<>();
    
    private int scannedChunks = 0;
    private int totalChunks = 0;

    // Заглушка для AntiXRay, чтобы не было ошибок компиляции
    public Object getAntiXRayBypass() { return null; }

    public void addSelectedBlock(Block block) { selectedBlocks.add(block); }
    public void removeSelectedBlock(Block block) { selectedBlocks.remove(block); }
    public void clearSelectedBlocks() { selectedBlocks.clear(); }
    public Set<Block> getSelectedBlocks() { return selectedBlocks; }
    
    public void setScanRadius(int radius) { this.scanRadius = radius; }
    public int getScanRadius() { return scanRadius; }
    
    public void setLiteMode(boolean lite) { this.liteMode = lite; }
    public boolean isLiteMode() { return liteMode; }
    
    public void setLiteHeightLimit(int limit) { this.liteHeightLimit = limit; }
    public int getLiteHeightLimit() { return liteHeightLimit; }

    public boolean isRunning() { return running; }
    public List<BlockPos> getFoundBlocks() { return foundBlocks; }
    public int getScannedChunks() { return scannedChunks; }
    public int getTotalChunks() { return totalChunks; }

    // Метод start() который ждет BaseFinderCommand
    public void start() {
        startScan();
    }

    // Метод stop() который ждет BaseFinderCommand
    public void stop() {
        stopScan();
    }

    public void startScan() {
        if (selectedBlocks.isEmpty()) {
            BaseFinderClient.LOGGER.warn("[BaseFinder] No blocks selected!");
            return;
        }
        if (client.world == null || client.player == null) {
            BaseFinderClient.LOGGER.warn("[BaseFinder] Not in a world!");
            return;
        }

        running = true;
        foundBlocks.clear();
        scannedChunks = 0;
        chunkQueue.clear();
        
        buildChunkQueue(client);
        BaseFinderClient.LOGGER.info("[BaseFinder] Scanner started. Radius: {} blocks.", scanRadius);
        
        // Запускаем поток сканирования
        new Thread(this::scanLoop).start();
    }

    public void stopScan() {
        running = false;
        BaseFinderClient.LOGGER.info("[BaseFinder] Scanner stopped. Found {} blocks.", foundBlocks.size());
    }

    private void buildChunkQueue(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        
        int px = client.player.getBlockPos().getX() >> 4;
        int pz = client.player.getBlockPos().getZ() >> 4;
        int r = scanRadius >> 4; // Радиус в чанках

        for (int x = px - r; x <= px + r; x++) {
            for (int z = pz - r; z <= pz + r; z++) {
                chunkQueue.offer(new ChunkPos(x, z));
            }
        }
        totalChunks = chunkQueue.size();
    }

    private void scanLoop() {
        while (running && !chunkQueue.isEmpty()) {
            ChunkPos pos = chunkQueue.poll();
            if (pos == null) continue;

            WorldChunk chunk = client.world.getChunk(pos.x, pos.z);
            if (chunk == null) continue;

            scanChunk(chunk);
            scannedChunks++;
        }
        running = false;
        BaseFinderClient.LOGGER.info("[BaseFinder] Scan complete! Found {} blocks.", foundBlocks.size());
    }

    private void scanChunk(WorldChunk chunk) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        
        int maxY = liteMode ? liteHeightLimit : 255;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y < maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    
                    if (selectedBlocks.contains(state.getBlock())) {
                        // Проверка на дубликаты перед добавлением
                        if (!foundBlocks.contains(pos)) {
                            foundBlocks.add(pos);
                        }
                    }
                }
            }
        }
    }
}
