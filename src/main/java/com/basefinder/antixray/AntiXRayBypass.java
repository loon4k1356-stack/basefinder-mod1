package com.basefinder.antixray;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiXRayBypass {

    private final Map<BlockPos, BlockState> confirmedBlocks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> validatedChunks = ConcurrentHashMap.newKeySet();

    private long lastScanTime = 0;
    private int scanCount = 0;
    private final Random random = new Random();

    private int minDelayMs = 30;
    private int maxDelayMs = 150;
    private int burstLimit = 10;
    private int burstCooldownMs = 500;
    private long burstStartTime = 0;
    private int burstCount = 0;

    private static final Set<String> COMMON_COVERS = Set.of(
        "minecraft:stone", "minecraft:deepslate", "minecraft:granite",
        "minecraft:diorite", "minecraft:andesite", "minecraft:tuff",
        "minecraft:netherrack", "minecraft:basalt", "minecraft:blackstone"
    );

    public AntiXRayBypass() {
        BaseFinderClient.LOGGER.info("[BaseFinder] Anti-XRay bypass initialized for HolyWorld Lite");
    }

    public void recordBlockUpdate(BlockPos pos, BlockState state) {
        confirmedBlocks.put(pos.toImmutable(), state);
        validatedChunks.add(new ChunkPos(pos));
    }

    public boolean isBlockReal(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        String blockId = Registries.BLOCK.getId(block).toString();

        BlockState confirmed = confirmedBlocks.get(pos);
        if (confirmed != null) return true;

        ChunkPos chunkPos = new ChunkPos(pos);
        if (validatedChunks.contains(chunkPos)) return true;

        if (isSuspiciousPosition(world, pos, blockId)) return false;
        if (isHiddenByAntiXRay(world, pos, blockId)) return false;

        return true;
    }

    private boolean isSuspiciousPosition(World world, BlockPos pos, String blockId) {
        if (!blockId.contains("ore") && !blockId.contains("ancient_debris")) return false;

        int coverNeighbors = 0;
        int totalNeighbors = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    totalNeighbors++;
                    BlockPos neighborPos = pos.add(dx, dy, dz);
                    BlockState neighborState = world.getBlockState(neighborPos);
                    String neighborId = Registries.BLOCK.getId(neighborState.getBlock()).toString();
                    if (COMMON_COVERS.contains(neighborId)) coverNeighbors
