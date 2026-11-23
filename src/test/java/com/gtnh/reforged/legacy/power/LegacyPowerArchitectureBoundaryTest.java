package com.gtnh.reforged.legacy.power;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the pure Legacy Power API/core packages against accidental
 * Minecraft, Forge, or NeoForge implementation dependencies.
 *
 * <p>Concrete game-facing adapters belong in the bridge/integration
 * subpackages and are deliberately excluded from this source scan.</p>
 */
class LegacyPowerArchitectureBoundaryTest {

    private static final List<String> FORBIDDEN_TYPE_PREFIXES = List.of(
        "net.minecraft.",
        "net.minecraftforge.",
        "net.neoforged."
    );

    @Test
    void coreAndApiSourcesDoNotReferenceGamePlatformTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/gtnh/reforged/legacy/power");

        assertFalse(Files.notExists(sourceRoot),
            "Expected Legacy Power source directory to exist: " + sourceRoot);

        try (Stream<Path> sources = Files.walk(sourceRoot)) {
            List<String> violations = sources
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !isBridgeOrIntegrationSource(sourceRoot, path))
                .flatMap(path -> findForbiddenReferences(path).stream())
                .toList();

            assertFalse(!violations.isEmpty(),
                () -> "Legacy Power API/core must not reference Minecraft, Forge, "
                    + "or NeoForge types. Move game-facing code into a bridge/integration "
                    + "subpackage. Violations:\n" + String.join("\n", violations));
        }
    }

    private static boolean isBridgeOrIntegrationSource(Path sourceRoot, Path source) {
        Path relative = sourceRoot.relativize(source);
        return relative.getNameCount() > 1
            && (relative.getName(0).toString().equals("bridge")
                || relative.getName(0).toString().equals("integration"));
    }

    private static List<String> findForbiddenReferences(Path source) {
        try {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            return FORBIDDEN_TYPE_PREFIXES.stream()
                .filter(text::contains)
                .map(prefix -> source + " references forbidden prefix " + prefix)
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect source file " + source, exception);
        }
    }
}
