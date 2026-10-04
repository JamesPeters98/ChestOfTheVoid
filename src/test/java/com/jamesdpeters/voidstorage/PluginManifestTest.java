package com.jamesdpeters.voidstorage;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PluginManifestTest {
    @Test
    void packagedManifestTargetsTheVerifiedStableServer() throws IOException {
        try (var stream = getClass().getResourceAsStream("/manifest.json")) {
            assertNotNull(stream);
            var json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(json.contains("${"));
            var manifest = PluginManifest.CODEC.decode(BsonDocument.parse(json));
            assertTrue(manifest.getServerVersion().satisfies(Semver.fromString("0.6.8")));
            assertFalse(manifest.getServerVersion().satisfies(Semver.fromString("0.7.0")));
            assertTrue(manifest.includesAssetPack());
        }
    }
}
