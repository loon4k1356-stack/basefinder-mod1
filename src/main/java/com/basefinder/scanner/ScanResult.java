package com.basefinder.scanner;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;

/**
 * Represents a found block with its position and type
 */
public class ScanResult {
    private final BlockPos position;
    private final Block block;
    private final long timestamp;
    private final int distance;

    public ScanResult(BlockPos position, Block block, int distance) {
        this.position = position;
        this.block = block;
        this.timestamp = System.currentTimeMillis();
        this.distance = distance;
    }

    public BlockPos getPosition() {
        return position;
    }

    public Block getBlock() {
        return block;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDistance() {
        return distance;
    }

    public String getFormattedCoords() {
        return String.format("X: %d, Y: %d, Z: %d", position.getX(), position.getY(), position.getZ());
    }

    public String getFormattedInfo() {
        return String.format("[%s] %s (distance: %d blocks)",
                getFormattedCoords(),
                block.getName().getString(),
                distance);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ScanResult other)) return false;
        return position.equals(other.position) && block.equals(other.block);
    }

    @Override
    public int hashCode() {
        return position.hashCode() * 31 + block.hashCode();
    }
}
