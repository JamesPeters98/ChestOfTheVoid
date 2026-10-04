package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.data.unknown.UnknownComponents;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.jspecify.annotations.Nullable;

/** Converts already placed chests from the launch-era block-state format. */
public class VoidStorageBlockMigration extends BlockModule.MigrationSystem {
    static final String LEGACY_ID = "void_storage_container";

    @Override
    public void onEntityAdd(Holder<ChunkStore> holder, AddReason reason, Store<ChunkStore> store) {
        var unknown = holder.getComponent(ChunkStore.REGISTRY.getUnknownComponentType());
        if (unknown == null) {
            return;
        }
        var migrated = migrateLegacyState(unknown);
        if (migrated != null && holder.getComponent(ItemContainerBlock.getComponentType()) == null) {
            holder.putComponent(ItemContainerBlock.getComponentType(), migrated);
        }
    }

    static @Nullable ItemContainerBlock migrateLegacyState(UnknownComponents<ChunkStore> unknown) {
        if (!unknown.contains(LEGACY_ID)) {
            return null;
        }
        // The old block state stored only chest settings. Items remain on the player.
        var container = ItemContainerBlock.CODEC.decode(new BsonDocument("Capacity", new BsonInt32(1)), new ExtraInfo());
        unknown.getUnknownComponents().remove(LEGACY_ID);
        return container;
    }

    @Override
    public void onEntityRemoved(Holder<ChunkStore> holder, RemoveReason reason, Store<ChunkStore> store) {
    }

    @Override
    public Query<ChunkStore> getQuery() {
        return ChunkStore.REGISTRY.getUnknownComponentType();
    }
}
