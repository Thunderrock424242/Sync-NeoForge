package com.thunder.syncneoforge.block.entity;

import dev.kir.sync.block.SyncBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class SyncBlockEntities {
    public static final BlockEntityType<ShellStorageBlockEntity> SHELL_STORAGE;
    public static final BlockEntityType<ShellConstructorBlockEntity> SHELL_CONSTRUCTOR;
    public static final BlockEntityType<TreadmillBlockEntity> TREADMILL;

    static {
        SHELL_STORAGE = register(ShellStorageBlockEntity::new, SyncBlocks.SHELL_STORAGE);
        SHELL_CONSTRUCTOR = register(ShellConstructorBlockEntity::new, SyncBlocks.SHELL_CONSTRUCTOR);
        TREADMILL = register(TreadmillBlockEntity::new, SyncBlocks.TREADMILL);
    }

    public static void init() { }

    private static <T extends BlockEntity> BlockEntityType<T> register(BlockEntityType.BlockEntityFactory<T> factory, Block block) {
        Identifier id = Registry.BLOCK.getId(block);
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.create(factory, block).build(null));
    }
}
