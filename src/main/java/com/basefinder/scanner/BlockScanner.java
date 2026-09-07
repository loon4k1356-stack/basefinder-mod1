package com.basefinder.scanner;

import com.basefinder.BaseFinderClient;
import com.basefinder.antixray.AntiXRayBypass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main block scanner engine.
 * Scans chunks around the player for selected blocks.
 * Supports full mode (300 block radius) and lite mode (30 height limit).
 */
public class BlockScanner {
    // Scanner state
    private boolean running = false;
    private boolean liteMode = false;

    // Configuration
    private int scanRadius = 300;        // Default radius: 300 blocks
    private int liteHeightLimit = 30;    // Lite mode: only scan below Y=30
    private int scanDelay = 50;          // Delay between chunk scans (ms) - anti-xray bypass
    private int blocksPerTick = 500;     // Max blocks to scan per tick

    // Selected blocks to search for
    private final Set<Block> selectedBlocks = Collections.synchronizedSet(new LinkedHashSet<>());

    // Results
    private final List<ScanResult> foundBlocks = new CopyOnWriteArrayList<>();

    // Scan tracking
    private final Queue<ChunkPos> chunkQueue = new ConcurrentLinkedQueue<>();
    private int scannedChunks = 0;
    private int totalChunks = 0;
    private long lastScanTime = 0;

    // Anti-XRay bypass
    private final AntiXRayBypass antiXRayBypass = new AntiXRayBypass();

    public BlockScanner() {
        BaseFinderClient.LOGGER.info("[BaseFinder] Scanner initialized with radius: {}", scanRadius);
    }

    /**
     * Start scanning for selected blocks
     */
    public void start() {
        if (selectedBlocks.isEmpty()) {
            BaseFinderClient.LOGGER.warn("[BaseFinder] No blocks selected! Open GUI with [O] to select blocks.");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            BaseFinderClient.LOGGER.warn("[BaseFinder] Not in a world!");
            return;
        }

        running = true;
        foundBlocks.clear();
        scannedChunks = 0;

        // Build chunk queue with anti-xray randomization
        buildChunkQueue(client);

        String mode = liteMode ? "LITE (height < " + liteHeightLimit + ")" : "FULL";
        BaseFinderClient.LOGGER.info("[BaseFinder] Scanner started in {} mode. Radius: {} blocks. Chunks to scan: {}",
                mode, scanRadius, totalChunks);
    }

    /**
     * Stop scanning
     */
    public void stop() {
        running = false;
        chunkQueue.clear();
        BaseFinderClient.LOGGER.info("[BaseFinder] Scanner stopped. Found {} blocks in {} chunks.",
                foundBlocks.size(), scannedChunks);
    }

    /**
     * Build the queue of chunks to scan, with anti-xray bypass randomization
     */
    private void buildChunkQueue(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        BlockPos playerPos = player.getBlockPos();
        int chunkRadius = (scanRadius / 16) + 1;
        ChunkPos playerChunk = new ChunkPos(playerPos);

        List<ChunkPos> chunks = new ArrayList<>();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                int distX = (chunkPos.x - playerChunk.x) * 16;
                int distZ = (chunkPos.z - playerChunk.z) * 16;
                double dist = Math.sqrt(distX * distX + distZ * distZ);

                if (dist <= scanRadius) {
                    chunks.add(chunkPos);
                }
            }
        }

        // Anti-XRay: Randomize chunk order to avoid detection patterns
        chunks = antiXRayBypass.randomizeChunkOrder(chunks);

        chunkQueue.addAll(chunks);
        totalChunks = chunks.size();
    }

    /**
     * Called every client tick to process scanning
     */
    public void tick() {
        if (!running) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            stop();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScanTime < scanDelay) return;
        lastScanTime = currentTime;

        // Anti-XRay: Check if we should delay this scan
        if (!antiXRayBypass.shouldScanNow()) return;

        World world = client.world;
        ClientPlayerEntity player = client.player;
        BlockPos playerPos = player.getBlockPos();
        int blocksScannedThisTick = 0;

        // Process chunks from queue
        while (!chunkQueue.isEmpty() && blocksScannedThisTick < blocksPerTick) {
            ChunkPos chunkPos = chunkQueue.poll();
            if (chunkPos == null) break;

            Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;

            // Determine Y range based on mode
            int minY = world.getBottomY();
            int maxY;

            if (liteMode) {
                // Lite mode: only scan below height limit
                maxY = Math.min(liteHeightLimit, world.getTopY());
            } else {
                maxY = world.getTopY();
            }

            // Scan blocks in chunk
            for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
                for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                    for (int y = minY; y < maxY; y++) {
                        if (blocksScannedThisTick >= blocksPerTick) break;

                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = chunk.getBlockState(pos);
                        Block block = state.getBlock();

                        // Anti-XRay: Validate block is not a fake block
                        if (!antiXRayBypass.isBlockReal(world, pos, state)) {
                            continue;
                        }

                        // Check if block matches our selection
                        if (selectedBlocks.contains(block)) {
                            int distance = (int) Math.sqrt(
                                    Math.pow(pos.getX() - playerPos.getX(), 2) +
                                    Math.pow(pos.getY() - playerPos.getY(), 2) +
                                    Math.pow(pos.getZ() - playerPos.getZ(), 2)
                            );

                            ScanResult result = new ScanResult(pos, block, distance);

                            // Avoid duplicates
                            if (!foundBlocks.contains(result)) {
                                foundBlocks.add(result);
                                BaseFinderClient.LOGGER.info("[BaseFinder] Found {} at {} ({} blocks away)",
                                        block.getName().getString(),
                                        result.getFormattedCoords(),
                                        distance);
                            }
                        }

                        blocksScannedThisTick++;
                    }
                }
            }

            scannedChunks++;
        }

        // If queue is empty, scanning is complete
        if (chunkQueue.isEmpty()) {
            running = false;
            BaseFinderClient.LOGGER.info("[BaseFinder] Scan complete! Found {} blocks total.", foundBlocks.size());

            // Print summary
            if (!foundBlocks.isEmpty()) {
                BaseFinderClient.LOGGER.info("[BaseFinder] === SCAN RESULTS ===");
                for (ScanResult result : foundBlocks) {
                    BaseFinderClient.LOGGER.info("[BaseFinder] {}", result.getFormattedInfo());
                }
            }
        }
    }

    // === Getters and Setters ===

    public boolean isRunning() {
        return running;
    }

    public boolean isLiteMode() {
        return liteMode;
    }

    public void setLiteMode(boolean liteMode) {
        this.liteMode = liteMode;
    }

    public int getScanRadius() {
        return scanRadius;
    }

    public void setScanRadius(int radius) {
        this.scanRadius = radius;
    }

    public int getLiteHeightLimit() {
        return liteHeightLimit;
    }

    public void setLiteHeightLimit(int limit) {
        this.liteHeightLimit = limit;
    }

    public Set<Block> getSelectedBlocks() {
        return selectedBlocks;
    }

    public void addSelectedBlock(Block block) {
        selectedBlocks.add(block);
    }

    public void removeSelectedBlock(Block block) {
        selectedBlocks.remove(block);
    }

    public void clearSelectedBlocks() {
        selectedBlocks.clear();
    }

    public List<ScanResult> getFoundBlocks() {
        return Collections.unmodifiableList(foundBlocks);
    }

    public int getScannedChunks() {
        return scannedChunks;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public AntiXRayBypass getAntiXRayBypass() {
        return antiXRayBypass;
    }
}
