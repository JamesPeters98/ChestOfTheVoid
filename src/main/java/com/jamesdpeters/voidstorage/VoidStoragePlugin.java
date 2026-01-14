package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class VoidStoragePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private static ComponentType<EntityStore, VoidStorageComponent> VOID_STORAGE = null;

    public VoidStoragePlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initialising %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCodecRegistry(Interaction.CODEC).register("VoidOpenContainer", VoidStorageOpenContainerInteraction.class, VoidStorageOpenContainerInteraction.CODEC);
        VOID_STORAGE = this.getEntityStoreRegistry().registerComponent(VoidStorageComponent.class, "VoidStorage", VoidStorageComponent.CODEC);
        LOGGER.atInfo().log("Registered VoidStorage component");
    }

    @Nullable
    public static ComponentType<EntityStore, VoidStorageComponent> getVoidStorage() {
        return VOID_STORAGE;
    }
}
