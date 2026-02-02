package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class SpriteGenerator {
    private static final int TILE_SIZE = Constants.TILE_SIZE;

    private static Pixmap createPixmap() {
        return new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
    }

    private static Texture toTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // --- Tile Generators ---

    public static Texture createWall() {
        Pixmap p = createPixmap();
        // Base color
        p.setColor(Color.valueOf("#444444"));
        p.fill();

        // Bricks
        p.setColor(Color.valueOf("#333333"));
        int brickH = 8;
        for (int y = 0; y < TILE_SIZE; y += brickH) {
            int offset = (y / brickH) % 2 == 0 ? 0 : 8;
            for (int x = -8; x < TILE_SIZE; x += 16) {
                p.fillRectangle(x + offset, y, 15, brickH - 1);
            }
        }
        return toTexture(p);
    }

    public static Texture createFloor() {
        Pixmap p = createPixmap();
        p.setColor(Color.valueOf("#222222"));
        p.fill();

        // Noise/Gravel
        p.setColor(Color.valueOf("#2a2a2a"));
        for (int i = 0; i < 20; i++) {
            int x = (int) (Math.random() * TILE_SIZE);
            int y = (int) (Math.random() * TILE_SIZE);
            p.fillRectangle(x, y, 2, 2);
        }
        return toTexture(p);
    }

    public static Texture createStairs() {
        Pixmap p = createPixmap();
        p.setColor(Color.valueOf("#222222"));
        p.fill();

        p.setColor(Color.valueOf("#666666"));
        for (int i = 0; i < TILE_SIZE; i += 4) {
            p.fillRectangle(4, i, TILE_SIZE - 8, 2);
        }
        return toTexture(p);
    }

    public static Texture createDoor() {
        Pixmap p = createPixmap();
        p.setColor(Color.BROWN);
        p.fill();
        p.setColor(Color.valueOf("#5D4037")); // Darker brown frame
        p.drawRectangle(0, 0, TILE_SIZE, TILE_SIZE);
        p.fillRectangle(TILE_SIZE/2 - 2, TILE_SIZE/2, 4, 4); // Knob
        return toTexture(p);
    }

    // --- Character Generators ---

    public static Texture createPlayer() {
        Pixmap p = createPixmap();

        // Head
        p.setColor(Color.valueOf("#ffcccc")); // Skin
        p.fillRectangle(10, 4, 12, 10);

        // Helmet/Hair
        p.setColor(Color.valueOf("#aaaaaa"));
        p.fillRectangle(10, 2, 12, 4);
        p.fillRectangle(8, 4, 2, 8);
        p.fillRectangle(22, 4, 2, 8);

        // Body (Armor)
        p.setColor(Color.valueOf("#8888ff"));
        p.fillRectangle(8, 14, 16, 12);

        // Legs
        p.setColor(Color.valueOf("#444444"));
        p.fillRectangle(10, 26, 4, 6);
        p.fillRectangle(18, 26, 4, 6);

        // Eyes
        p.setColor(Color.BLACK);
        p.fillRectangle(12, 8, 2, 2);
        p.fillRectangle(18, 8, 2, 2);

        return toTexture(p);
    }

    public static Texture createMonster(String type) {
        Pixmap p = createPixmap();

        // Note: Pixmap Y is 0 at top. Canvas Y is 0 at top. Logic should match but keep in mind.
        // Re-implementing H5 logic directly.

        if (type.equals("SLIME")) {
            p.setColor(Color.BLUE);
            p.fillCircle(16, 16, 10); // Top half approx
            p.fillRectangle(6, 16, 20, 12); // Bottom fill to square it off a bit?
            // H5: arc(16, 20, 10, PI, 0) -> top arc. lineTo 26,28 -> bottom right. lineTo 6,28 -> bottom left.
            // Simplified for Pixmap:
            p.fillCircle(16, 20, 10);

            // Eyes
            p.setColor(Color.WHITE);
            p.fillRectangle(12, 18, 3, 3);
            p.fillRectangle(18, 18, 3, 3);
            p.setColor(Color.BLACK);
            p.fillRectangle(13, 19, 1, 1);
            p.fillRectangle(19, 19, 1, 1);

        } else if (type.equals("SKELETON")) {
            p.setColor(Color.valueOf("#eeeeee"));
            // Skull
            p.fillRectangle(10, 4, 12, 10);
            // Ribs
            p.fillRectangle(12, 16, 8, 10);
            // Legs
            p.fillRectangle(10, 26, 3, 6);
            p.fillRectangle(19, 26, 3, 6);
            // Eyes
            p.setColor(Color.BLACK);
            p.fillRectangle(12, 7, 3, 3);
            p.fillRectangle(17, 7, 3, 3);

        } else if (type.equals("ORC")) {
            p.setColor(Color.valueOf("#00aa00"));
            p.fillRectangle(6, 6, 20, 22);
            // Tusks
            p.setColor(Color.WHITE);
            p.fillRectangle(10, 20, 2, 4);
            p.fillRectangle(20, 20, 2, 4);
             // Eyes
             p.setColor(Color.RED);
             p.fillRectangle(10, 10, 4, 2);
             p.fillRectangle(18, 10, 4, 2);

        } else if (type.equals("BAT")) {
             p.setColor(Color.valueOf("#555555"));
             // Wings (Simplified as rectangle/lines for Pixmap)
             p.fillTriangle(16, 16, 4, 10, 8, 22);
             p.fillTriangle(16, 16, 28, 10, 24, 22);
             p.fillCircle(16, 16, 4); // Body

             // Eyes
             p.setColor(Color.YELLOW);
             p.fillRectangle(14, 18, 1, 1);
             p.fillRectangle(17, 18, 1, 1);

        } else if (type.equals("BOSS")) {
             // Dragon
             // Wings
             p.setColor(Color.valueOf("#880000")); // Dark Red Wings
             p.fillTriangle(16, 16, 2, 6, 16, 24);
             p.fillTriangle(16, 16, 30, 6, 16, 24);

             // Body
             p.setColor(Color.valueOf("#dd0000")); // Bright Red
             p.fillRectangle(12, 10, 8, 16);

             // Head
             p.setColor(Color.RED);
             p.fillRectangle(10, 6, 12, 10);

             // Horns
             p.setColor(Color.YELLOW);
             p.fillRectangle(10, 2, 2, 6);
             p.fillRectangle(20, 2, 2, 6);

             // Eyes
             p.setColor(Color.GREEN); // Green eyes
             p.fillRectangle(11, 10, 2, 2);
             p.fillRectangle(19, 10, 2, 2);
        }

        return toTexture(p);
    }

    public static Texture createItem(String name) {
        Pixmap p = createPixmap();

        if (name.contains("Potion")) {
            p.setColor(name.contains("Health") ? Color.RED : Color.BLUE);
            p.fillCircle(16, 20, 6);
            p.fillRectangle(14, 10, 4, 10); // Neck
            p.setColor(Color.WHITE);
            p.fillRectangle(14, 14, 2, 2); // Shine
        } else if (name.contains("Sword") || name.contains("Blade")) {
            p.setColor(Color.valueOf("#aaaaaa"));
            // Blade
            for(int i=0; i<16; i++) {
                p.fillRectangle(24-i, 8+i, 3, 3);
            }
            // Hilt
            p.setColor(Color.BROWN);
            p.fillRectangle(6, 26, 6, 6);
        } else {
            // Default box
            p.setColor(Color.GOLD);
            p.fillRectangle(8, 8, 16, 16);
        }

        return toTexture(p);
    }
}
