package com.gtnh.reforged.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ItemResourceAssetsTest {
    @Test
    void ironItemDefinitionsPointToTheirModels() throws IOException {
        assertTrue(resourceText("assets/gtnh_reforged/items/iron_dust.json")
                .contains("\"model\": \"gtnh_reforged:item/iron_dust\""));
        assertTrue(resourceText("assets/gtnh_reforged/items/iron_ingot.json")
                .contains("\"model\": \"gtnh_reforged:item/iron_ingot\""));
    }

    @Test
    void ironModelsPointToExistingTextures() throws IOException {
        assertTrue(resourceText("assets/gtnh_reforged/models/item/iron_dust.json")
                .contains("\"layer0\": \"gtnh_reforged:item/iron_dust\""));
        assertTrue(resourceText("assets/gtnh_reforged/models/item/iron_ingot.json")
                .contains("\"layer0\": \"gtnh_reforged:item/iron_ingot\""));

        assertTexture("assets/gtnh_reforged/textures/item/iron_dust.png");
        assertTexture("assets/gtnh_reforged/textures/item/iron_ingot.png");
    }

    @Test
    void ironItemsHaveEnglishAndChineseNames() throws IOException {
        String english = resourceText("assets/gtnh_reforged/lang/en_us.json");
        String chinese = resourceText("assets/gtnh_reforged/lang/zh_cn.json");

        assertTrue(english.contains("\"item.gtnh_reforged.iron_dust\": \"Iron Dust\""));
        assertTrue(english.contains("\"item.gtnh_reforged.iron_ingot\": \"Iron Ingot\""));
        assertTrue(chinese.contains("\"item.gtnh_reforged.iron_dust\": \"铁粉\""));
        assertTrue(chinese.contains("\"item.gtnh_reforged.iron_ingot\": \"铁锭\""));
    }

    private static String resourceText(String path) throws IOException {
        try (InputStream stream = resource(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertTexture(String path) throws IOException {
        try (InputStream stream = resource(path)) {
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " must be a readable PNG image");
            assertEquals(16, image.getWidth(), path + " width");
            assertEquals(16, image.getHeight(), path + " height");
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = ItemResourceAssetsTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        return stream;
    }
}
