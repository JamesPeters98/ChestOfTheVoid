package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VoidStorageComponent implements Component<EntityStore> {

    static final short CAPACITY = 63;

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
    public VoidStorageComponent clone() {
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
        return new SimpleItemContainer(CAPACITY);
    }

}
