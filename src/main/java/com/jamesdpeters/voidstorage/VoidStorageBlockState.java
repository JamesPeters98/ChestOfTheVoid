package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.DestroyableBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.meta.state.MarkerBlockState;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class VoidStorageBlockState extends BlockState implements DestroyableBlockState, MarkerBlockState {

    public static final BuilderCodec<VoidStorageBlockState> CODEC = BuilderCodec.builder(VoidStorageBlockState.class, VoidStorageBlockState::new, BlockState.BASE_CODEC)
            .append(new KeyedCodec<>("Custom", Codec.BOOLEAN),
                    (state, o) -> state.custom = o,
                    state -> state.custom)
            .add()
            .append(new KeyedCodec<>("AllowViewing", Codec.BOOLEAN),
                    (state, o) -> state.allowViewing = o,
                    state -> state.allowViewing)
            .add()
            .append(new KeyedCodec<>("Droplist", Codec.STRING),
                    (state, o) -> state.droplist = o,
                    state -> state.droplist)
            .add()
            .append(new KeyedCodec<>("Marker", WorldMapManager.MarkerReference.CODEC),
                    (state, o) -> state.marker = o,
                    state -> state.marker)
            .add()
            .build();

    protected boolean custom;
    protected boolean allowViewing = true;
    @Nullable
    protected String droplist;

    protected WorldMapManager.@Nullable MarkerReference marker;
    private final Map<UUID, ContainerBlockWindow> windows = new ConcurrentHashMap<>();

    public VoidStorageBlockState() { }

    public VoidStorageBlockState(ItemContainerState itemContainerState) {
        this.allowViewing = itemContainerState.isAllowViewing();
        this.droplist = itemContainerState.getDroplist();
        this.custom = true;
    }

    @Override
    public boolean initialize(BlockType blockType) {
        if (!super.initialize(blockType)) {
            return false;
        } else if (this.custom) {
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void onDestroy() {
        if (this.marker != null) {
            this.marker.remove();
        }
    }

    @Override
    public void setMarker(WorldMapManager.MarkerReference markerReference) {
        this.marker = markerReference;
        this.markNeedsSave();
    }

    public boolean isAllowViewing() {
        return allowViewing;
    }

    public Map<UUID, ContainerBlockWindow> getWindows() {
        return windows;
    }
}
