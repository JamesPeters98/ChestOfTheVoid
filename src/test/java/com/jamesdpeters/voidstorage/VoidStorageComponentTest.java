package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.event.EventBus;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class VoidStorageComponentTest {
    @BeforeAll
    static void registerServerContainerCodecs() throws IOException {
        Options.parse(new String[]{"--disable-sentry", "--disable-file-watcher"});
        // Codec default validation needs an item asset map even without a running server.
        var builder = new HytaleAssetStore.Builder<>(String.class, Item.class, new DefaultAssetMap<String, Item>())
                .setPath("Item/Items").setCodec(Item.CODEC).setKeyFunction(Item::getId);
        var events = new EventBus(false);
        AssetRegistry.register(new HytaleAssetStore<>(builder) {
            @Override
            protected EventBus getEventBus() {
                return events;
            }
        });
        ItemContainer.CODEC.register("Simple", SimpleItemContainer.class, SimpleItemContainer.CODEC);
    }

    // Launch-era save shape, including the old dropped-animation field and final slot.
    private static final String LEGACY_SAVE = """
            {"VoidStorage":{"Id":"Simple","Capacity":63,"Items":{
              "0":{"Id":"Cloth_Block_Wool_Black","Quantity":100,"Durability":0.0,
                   "MaxDurability":0.0,"OverrideDroppedItemAnimation":false},
              "62":{"Id":"Ingredient_Void_Essence","Quantity":37,"Durability":0.0,
                    "MaxDurability":0.0,"OverrideDroppedItemAnimation":false}
            }}}
            """;

    @Test
    void legacyPlayerInventorySurvivesSaveRoundTrip() {
        var storage = VoidStorageComponent.CODEC.decode(BsonDocument.parse(LEGACY_SAVE));
        var restored = VoidStorageComponent.CODEC.decode(VoidStorageComponent.CODEC.encode(storage));

        assertEquals(63, restored.getItemContainer().getCapacity());
        assertEquals("Cloth_Block_Wool_Black", restored.getItemContainer().getItemStack((short) 0).getItemId());
        assertEquals(100, restored.getItemContainer().getItemStack((short) 0).getQuantity());
        assertEquals("Ingredient_Void_Essence", restored.getItemContainer().getItemStack((short) 62).getItemId());
        assertEquals(37, restored.getItemContainer().getItemStack((short) 62).getQuantity());
        assertNull(restored.getItemContainer().getItemStack((short) 1));
    }

    @Test
    void clonesKeepItemsWithoutSharingTheMutableInventory() {
        var original = VoidStorageComponent.CODEC.decode(BsonDocument.parse(LEGACY_SAVE));
        var clone = original.clone();
        clone.getItemContainer().removeItemStackFromSlot((short) 62);

        assertNotSame(original.getItemContainer(), clone.getItemContainer());
        assertNull(clone.getItemContainer().getItemStack((short) 62));
        assertEquals(37, original.getItemContainer().getItemStack((short) 62).getQuantity());
    }

    @Test
    void newPlayersReceiveSeparateEmpty63SlotInventories() {
        var first = new VoidStorageComponent();
        var second = VoidStorageComponent.CODEC.decode(new BsonDocument());

        assertEquals(63, first.getItemContainer().getCapacity());
        assertEquals(63, second.getItemContainer().getCapacity());
        assertTrue(first.getItemContainer().isEmpty());
        assertTrue(second.getItemContainer().isEmpty());
        assertNotSame(first.getItemContainer(), second.getItemContainer());
    }
}
