package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenContainerInteraction;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"removal", "deprecation"})
public class VoidStorageOpenContainerInteraction extends OpenContainerInteraction {
    public static final BuilderCodec<VoidStorageOpenContainerInteraction> CODEC = BuilderCodec.builder(
                    VoidStorageOpenContainerInteraction.class, VoidStorageOpenContainerInteraction::new, SimpleBlockInteraction.CODEC
            )
            .documentation("Opens the void container of the block currently being interacted with.")
            .build();

    @Override
    protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer, InteractionType type, InteractionContext context, @Nullable ItemStack itemInHand, Vector3i pos, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());

        var voidStorageComponentType = VoidStoragePlugin.getVoidStorage();
        if (voidStorageComponentType == null)
            return;

        var voidStorage = commandBuffer.ensureAndGetComponent(ref, voidStorageComponentType);

        if (playerComponent != null) {
            BlockState container = world.getState(pos.x, pos.y, pos.z, true);

            if (container instanceof ItemContainerState itemContainerState) {
                container.getChunk().setState(pos.x, pos.y, pos.z, new VoidStorageBlockState(itemContainerState));
                container = world.getState(pos.x, pos.y, pos.z, true);
            }

            if (container instanceof VoidStorageBlockState itemContainerState) {
                BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);
                if (itemContainerState.isAllowViewing()) {
                    UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());

                    assert uuidComponent != null;

                    UUID uuid = uuidComponent.getUuid();
                    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
                    ContainerBlockWindow window = new ContainerBlockWindow(
                            pos.x, pos.y, pos.z, chunk.getRotationIndex(pos.x, pos.y, pos.z), blockType, voidStorage.getItemContainer()
                    );
                    Map<UUID, ContainerBlockWindow> windows = itemContainerState.getWindows();
                    if (windows.putIfAbsent(uuid, window) == null) {
                        if (playerComponent.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window)) {
                            window.registerCloseEvent(event -> {
                                windows.remove(uuid, window);
                                BlockType currentBlockType = world.getBlockType(pos);
                                if (windows.isEmpty()) {
                                    world.setBlockInteractionState(pos, currentBlockType, "CloseWindow");
                                }

                                BlockType interactionStatex = currentBlockType.getBlockForState("CloseWindow");
                                if (interactionStatex != null) {
                                    int soundEventIndexx = interactionStatex.getInteractionSoundEventIndex();
                                    if (soundEventIndexx != 0) {
                                        int rotationIndexx = chunk.getRotationIndex(pos.x, pos.y, pos.z);
                                        Vector3d soundPosx = new Vector3d();
                                        blockType.getBlockCenter(rotationIndexx, soundPosx);
                                        soundPosx.add(pos);
                                        SoundUtil.playSoundEvent3d(ref, soundEventIndexx, soundPosx, commandBuffer);
                                    }
                                }
                            });
                            if (windows.size() == 1) {
                                world.setBlockInteractionState(pos, blockType, "OpenWindow");
                            }

                            BlockType interactionState = blockType.getBlockForState("OpenWindow");
                            if (interactionState == null) {
                                return;
                            }

                            int soundEventIndex = interactionState.getInteractionSoundEventIndex();
                            if (soundEventIndex == 0) {
                                return;
                            }

                            int rotationIndex = chunk.getRotationIndex(pos.x, pos.y, pos.z);
                            Vector3d soundPos = new Vector3d();
                            blockType.getBlockCenter(rotationIndex, soundPos);
                            soundPos.add(pos);
                            SoundUtil.playSoundEvent3d(ref, soundEventIndex, soundPos, commandBuffer);
                        } else {
                            windows.remove(uuid, window);
                        }
                    }
                }
            } else {
                playerComponent.sendMessage(
                        Message.translation("server.interactions.invalidBlockState")
                                .param("interaction", this.getClass().getSimpleName())
                                .param("blockState", container != null ? container.getClass().getSimpleName() : "null")
                );
            }
        }

    }
}
