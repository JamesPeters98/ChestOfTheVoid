package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.component.data.unknown.UnknownComponents;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VoidStorageBlockMigrationTest {
    @Test
    void migratesPlacedLegacyChestWithoutTouchingOtherComponents() {
        var unrelated = BsonDocument.parse("{\"Keep\":42}");
        var unknown = new UnknownComponents<ChunkStore>(new HashMap<>(Map.of(
                VoidStorageBlockMigration.LEGACY_ID, BsonDocument.parse("{\"Custom\":true,\"AllowViewing\":true}"),
                "AnotherPlugin", unrelated)));

        var migrated = VoidStorageBlockMigration.migrateLegacyState(unknown);

        assertNotNull(migrated);
        assertEquals(1, migrated.getCapacity());
        assertTrue(migrated.getItemContainer().isEmpty());
        assertTrue(migrated.getWindows().isEmpty());
        assertFalse(unknown.contains(VoidStorageBlockMigration.LEGACY_ID));
        assertEquals(unrelated, unknown.getUnknownComponents().get("AnotherPlugin"));
        assertNull(VoidStorageBlockMigration.migrateLegacyState(unknown));
    }

    @Test
    void leavesUnrelatedBlocksAlone() {
        var unknown = new UnknownComponents<ChunkStore>(new HashMap<>(Map.of("container", new BsonDocument())));

        assertNull(VoidStorageBlockMigration.migrateLegacyState(unknown));
        assertTrue(unknown.contains("container"));
    }
}
