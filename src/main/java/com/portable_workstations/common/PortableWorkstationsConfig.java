package com.portable_workstations.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class PortableWorkstationsConfig {
    private static final Path PATH = Path.of("config", "portable_workstations.properties");
    private static final String KEY_ALLOWED_BLOCKS = "allowed_blocks";
    private static final Set<String> DEFAULT_ALLOWED_BLOCKS = Set.of(
            "minecraft:furnace", "minecraft:crafting_table", "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil",
            "minecraft:enchanting_table", "minecraft:stonecutter", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand",
            "minecraft:ender_chest", "minecraft:loom", "minecraft:grindstone", "minecraft:smithing_table", "minecraft:cartography_table"
    );
    private static Set<String> allowedBlocks = DEFAULT_ALLOWED_BLOCKS;

    private PortableWorkstationsConfig() {
    }

    public static void load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (!Files.exists(PATH)) {
                Files.writeString(PATH, KEY_ALLOWED_BLOCKS + "=" + String.join(",", DEFAULT_ALLOWED_BLOCKS) + System.lineSeparator(), StandardCharsets.UTF_8);
                allowedBlocks = DEFAULT_ALLOWED_BLOCKS;
                return;
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String configuredBlocks = properties.getProperty(KEY_ALLOWED_BLOCKS);
            if (configuredBlocks == null) {
                allowedBlocks = DEFAULT_ALLOWED_BLOCKS;
                return;
            }
            allowedBlocks = Arrays.stream(configuredBlocks.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException ignored) {
            allowedBlocks = DEFAULT_ALLOWED_BLOCKS;
        }
    }

    public static boolean isAllowed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Block block = Block.byItem(stack.getItem());
        return allowedBlocks.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
    }
}
