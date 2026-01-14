package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class VoidStorageComponent implements Component<EntityStore> {

    public static final BuilderCodec<VoidStorageComponent> CODEC = BuilderCodec.builder(VoidStorageComponent.class, VoidStorageComponent::new)
            .append(new KeyedCodec<>("VoidStorage", ItemContainer.CODEC), (voidStorage, itemContainer, extraInfo) -> {
                voidStorage.setItemContainer(itemContainer);
            }, (voidStorage, extraInfo) -> voidStorage.voidContainer)
            .add()
            .afterDecode(voidStorage -> {
                if (voidStorage.voidContainer == null) {
                    voidStorage.setItemContainer(voidStorage.createDefaultContainer());
                }
            })
            .build();

    private ItemContainer voidContainer = createDefaultContainer();

    @Override
    public @Nullable Component<EntityStore> clone() {
        var voidStorage = new VoidStorageComponent();
        voidStorage.setItemContainer(this.voidContainer.clone());
        return voidStorage;
    }

    void setItemContainer(ItemContainer itemContainer) {
        this.voidContainer = itemContainer;
    }

    ItemContainer getItemContainer() {
        return this.voidContainer;
    }

    ItemContainer createDefaultContainer() {
        return new SimpleItemContainer((short) 63);
    }

}
