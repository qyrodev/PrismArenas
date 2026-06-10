package me.qyro.prismarenas.snapshot;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public record BlockSnapshot(int relativeX, int relativeY, int relativeZ, Material material, BlockData blockData) {
}
