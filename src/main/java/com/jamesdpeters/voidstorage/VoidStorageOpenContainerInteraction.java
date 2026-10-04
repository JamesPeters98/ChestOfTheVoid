package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenContainerInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class VoidStorageOpenContainerInteraction extends OpenContainerInteraction {
    public static final BuilderCodec<VoidStorageOpenContainerInteraction> CODEC = BuilderCodec.builder(
                    VoidStorageOpenContainerInteraction.class, VoidStorageOpenContainerInteraction::new, OpenContainerInteraction.CODEC)
            .documentation("Opens the interacting player's soulbound void storage.")
            .build();

    @Override
    protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer, InteractionType type,
                                     InteractionContext context, @Nullable ItemStack itemInHand, Vector3i pos,
                                     CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        var storageType = VoidStoragePlugin.getVoidStorage();
        if (player == null || playerRef == null || storageType == null) {
            return;
        }

        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunks.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return;
        }
        Store<ChunkStore> chunkStore = chunks.getStore();
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(chunkStore, sectionRef, pos.x, pos.y, pos.z);
        if (blockRef == null || !blockRef.isValid()) {
            return;
        }
        ItemContainerBlock container = chunkStore.getComponent(blockRef, ItemContainerBlock.getComponentType());
        BlockSection section = chunkStore.getComponent(sectionRef, BlockSection.getComponentType());
        if (container == null || section == null) {
            return;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (blockType == null) {
            return;
        }

        var storage = commandBuffer.ensureAndGetComponent(ref, storageType);
        int rotation = section.getRotationIndex(pos.x, pos.y, pos.z);
        // The block's own container stays empty: breaking it cannot drop personal items.
        ContainerBlockWindow window = new ContainerBlockWindow(pos.x, pos.y, pos.z, rotation, blockType, storage.getItemContainer());
        Map<UUID, ContainerBlockWindow> windows = container.getWindows();
        UUID uuid = playerRef.getUuid();
        if (windows.putIfAbsent(uuid, window) != null) {
            return;
        }
        if (!player.getPageManager().setPageWithWindows(ref, ref.getStore(), Page.Bench, true, window)) {
            windows.remove(uuid, window);
            return;
        }

        // Copy the position and use the live store when closing, never the expired command buffer.
        Vector3i position = new Vector3i(pos);
        window.registerCloseEvent(event -> world.execute(() -> {
            windows.remove(uuid, window);
            closeWindow(world, ref, position, blockRef, container, blockType, windows);
        }));
        if (windows.size() == 1) {
            BlockOperations.setBlockInteractionState(chunks, sectionRef, pos.x, pos.y, pos.z, blockType, OPEN_WINDOW, false);
        }
        playStateSound(ref, blockType, OPEN_WINDOW, rotation, position, commandBuffer);
    }

    private static void closeWindow(World world, Ref<EntityStore> playerRef, Vector3i pos, Ref<ChunkStore> blockRef,
                                    ItemContainerBlock container, BlockType openedBlockType,
                                    Map<UUID, ContainerBlockWindow> windows) {
        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunks.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid() || !blockRef.isValid()) {
            return;
        }
        Store<ChunkStore> chunkStore = chunks.getStore();
        // A replaced or destroyed chest must not be animated by an old window's callback.
        Ref<ChunkStore> currentRef = BlockModule.getBlockEntity(chunkStore, sectionRef, pos.x, pos.y, pos.z);
        if (!blockRef.equals(currentRef) || chunkStore.getComponent(blockRef, ItemContainerBlock.getComponentType()) != container) {
            return;
        }
        BlockSection section = chunkStore.getComponent(sectionRef, BlockSection.getComponentType());
        if (section == null) {
            return;
        }
        BlockType currentType = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (currentType == null || !Objects.equals(baseBlockId(openedBlockType), baseBlockId(currentType))) {
            return;
        }
        int rotation = section.getRotationIndex(pos.x, pos.y, pos.z);
        if (windows.isEmpty()) {
            BlockOperations.setBlockInteractionState(chunks, sectionRef, pos.x, pos.y, pos.z, currentType, CLOSE_WINDOW, false);
        }
        if (playerRef.isValid() && playerRef.getStore().getExternalData().getWorld() == world) {
            playStateSound(playerRef, currentType, CLOSE_WINDOW, rotation, pos, playerRef.getStore());
        }
    }

    private static String baseBlockId(BlockType blockType) {
        return Objects.requireNonNullElse(blockType.getDefaultStateKey(), blockType.getId());
    }

    private static void playStateSound(Ref<EntityStore> ref, BlockType blockType, String state, int rotation,
                                       Vector3i pos, ComponentAccessor<EntityStore> accessor) {
        BlockType stateType = blockType.getBlockForState(state);
        if (stateType == null || stateType.getInteractionSoundEventIndex() == 0) {
            return;
        }
        Vector3d soundPos = new Vector3d();
        blockType.getBlockCenter(rotation, soundPos);
        soundPos.add(pos.x, pos.y, pos.z);
        SoundUtil.playSoundEvent3d(ref, stateType.getInteractionSoundEventIndex(), soundPos, accessor);
    }
}
