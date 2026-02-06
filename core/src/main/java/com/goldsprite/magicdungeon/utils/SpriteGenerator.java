package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;

public class SpriteGenerator {
    private static final int TILE_SIZE = Constants.TILE_SIZE; // Assumed 32

    private static Pixmap createPixmap() {
        Pixmap p = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        // Clear with transparent
        p.setColor(0, 0, 0, 0);
        p.fill();
        return p;
    }

    private static Texture toTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Draws a pixel art pattern onto the pixmap.
     * @param p The target Pixmap (32x32)
     * @param pattern Array of strings representing rows.
     * @param palette Map of characters to Colors. '.' is treated as transparent (skip).
     */
    private static void drawPattern(Pixmap p, String[] pattern, Map<Character, Color> palette) {
        int rows = pattern.length;
        int cols = pattern[0].length();
        
        // Calculate scale factor to fit TILE_SIZE (32)
        // If pattern is 16x16, scale is 2. If 8x8, scale is 4.
        int scaleX = TILE_SIZE / cols;
        int scaleY = TILE_SIZE / rows;
        
        for (int y = 0; y < rows; y++) {
            String row = pattern[y];
            for (int x = 0; x < cols; x++) {
                char c = row.charAt(x);
                if (c == '.' || c == ' ') continue; // Transparent
                
                Color color = palette.get(c);
                if (color != null) {
                    p.setColor(color);
                    p.fillRectangle(x * scaleX, y * scaleY, scaleX, scaleY);
                }
            }
        }
    }

    // --- Tile Generators ---

    public static Texture createWall() {
        Pixmap p = createPixmap();
        
        // Base Wall
        p.setColor(Color.valueOf("#444444"));
        p.fill();
        
        // Classic Magic Tower Wall (Regular Bricks)
        String[] pattern = {
            "BBBBBBBBBBBBBBBB",
            "B..............B",
            "B..............B",
            "B..............B",
            "BBBBBBBBBBBBBBBB",
            ".......B........",
            ".......B........",
            ".......B........",
            "BBBBBBBBBBBBBBBB",
            "B..............B",
            "B..............B",
            "B..............B",
            "BBBBBBBBBBBBBBBB",
            ".......B........",
            ".......B........",
            ".......B........"
        };
        
        Map<Character, Color> pal = new HashMap<>();
        pal.put('B', Color.valueOf("#2a2a2a")); // Dark mortar/outline
        
        drawPattern(p, pattern, pal);
        return toTexture(p);
    }

    public static Texture createFloor() {
        Pixmap p = createPixmap();
        p.setColor(Color.valueOf("#222222")); // Darker floor base
        p.fill();

        // Random noise for texture
        p.setColor(Color.valueOf("#2a2a2a"));
        for (int i = 0; i < 64; i++) {
            p.fillRectangle((int)(Math.random()*TILE_SIZE), (int)(Math.random()*TILE_SIZE), 2, 2);
        }
        
        // Cracks or Stones (16x16)
        String[] pattern = {
            "................",
            ".S..............",
            "......S.........",
            "...........S....",
            "................",
            "...S............",
            ".......S........",
            "................",
            ".............S..",
            ".S..............",
            "................",
            ".....S..........",
            "..........S.....",
            "................",
            "..S.............",
            "................"
        };
        Map<Character, Color> pal = new HashMap<>();
        pal.put('S', Color.valueOf("#333333"));
        drawPattern(p, pattern, pal);
        
        return toTexture(p);
    }

    public static Texture createStairs() {
        Pixmap p = createPixmap();
        p.setColor(Color.valueOf("#222222"));
        p.fill();
        
        String[] pattern = {
            "................",
            "WWWWWWWWWWWWWWWW",
            "GGGGGGGGGGGGGGGG",
            "................",
            "WWWWWWWWWWWWWWWW",
            "GGGGGGGGGGGGGGGG",
            "................",
            "WWWWWWWWWWWWWWWW",
            "GGGGGGGGGGGGGGGG",
            "................",
            "WWWWWWWWWWWWWWWW",
            "GGGGGGGGGGGGGGGG",
            "................",
            "WWWWWWWWWWWWWWWW",
            "GGGGGGGGGGGGGGGG",
            "................"
        };
        Map<Character, Color> pal = new HashMap<>();
        pal.put('W', Color.valueOf("#555555")); // Step edge
        pal.put('G', Color.valueOf("#333333")); // Step top
        drawPattern(p, pattern, pal);
        
        return toTexture(p);
    }

    public static Texture createDoor() {
        Pixmap p = createPixmap();
        
        // 16x16 pattern
        String[] pattern = {
            "  FFFFFFFFFFFF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDKDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FDDDDDDDDDDF  ",
            "  FFFFFFFFFFFF  "
        };
        
        Map<Character, Color> pal = new HashMap<>();
        pal.put('F', Color.valueOf("#3e2723")); // Frame (Dark Brown)
        pal.put('D', Color.valueOf("#5d4037")); // Door (Brown)
        pal.put('K', Color.GOLD);               // Knob
        
        drawPattern(p, pattern, pal);
        return toTexture(p);
    }

    // --- Character Generators ---

    public static Texture createPlayer() {
        Pixmap p = createPixmap();

        // Reverting to classic geometric style but using drawPattern for consistency and slight touch-up
        // Head: 10,4, 12x10 -> x:5-10, y:2-6 (in 16x16 grid) roughly
        // Helmet: Gray top and sides
        // Body: Blue 8,14 16x12
        // Legs: Gray
        
        String[] pattern = {
            ".....HHHHHH.....", // Helmet Top
            "....HSSSSSSH....", // Head/Skin with Helmet sides
            "....HSSSSSSH....",
            "....H.E..E.H....", // Eyes (Black)
            "....H......H....",
            "....SSSSSSSS....", // Chin
            "....AAAAAAAA....", // Body (Armor)
            "....AAAAAAAA....",
            "....AAAAAAAA....",
            "....AAAAAAAA....",
            "....AAAAAAAA....",
            "....AAAAAAAA....",
            ".....LL..LL.....", // Legs
            ".....LL..LL.....",
            ".....LL..LL.....",
            "................"
        };
        
        Map<Character, Color> pal = new HashMap<>();
        pal.put('H', Color.valueOf("#aaaaaa")); // Helmet/Hair (Gray)
        pal.put('S', Color.valueOf("#ffcccc")); // Skin
        pal.put('A', Color.valueOf("#8888ff")); // Armor (Blueish)
        pal.put('L', Color.valueOf("#444444")); // Legs (Dark Gray)
        pal.put('E', Color.BLACK); // Eyes
        
        drawPattern(p, pattern, pal);
        return toTexture(p);
    }

    public static Texture createMonster(String type) {
        Pixmap p = createPixmap();
        String[] pattern = new String[16];
        Map<Character, Color> pal = new HashMap<>();
        
        if (type.equals("SLIME")) {
            pattern = new String[]{
                "................",
                "................",
                "................",
                "................",
                "......OOOO......",
                "....OOOOOOOO....",
                "...OOOOOOOOOO...",
                "..OOOOOOOOOOOO..",
                "..OOOOOOOOOOOO..",
                "..OOWOOOOOOWOO..",
                "..OOKOOOOOOKOO..",
                "..OOOOOOOOOOOO..",
                "...OOOOOOOOOO...",
                "....OOOOOOOO....",
                "................",
                "................"
            };
            pal.put('O', Color.valueOf("#44aaff")); // Slime Blue
            pal.put('W', Color.WHITE); // Eye White
            pal.put('K', Color.BLACK); // Pupil
            
        } else if (type.equals("SKELETON")) {
            pattern = new String[]{
                "......WWWW......",
                ".....WWWWWW.....",
                ".....WKWWKW.....",
                ".....WWWWWW.....",
                "......WWWW......",
                ".......NN.......",
                "....RRRRRRRR....",
                "....R.RRRR.R....",
                "....R.RRRR.R....",
                "......BBBB......",
                "......BBBB......",
                "......L..L......",
                "......L..L......",
                "......F..F......",
                "................",
                "................"
            };
            pal.put('W', Color.valueOf("#eeeeee")); // Skull
            pal.put('K', Color.BLACK); // Eye sockets
            pal.put('N', Color.valueOf("#cccccc")); // Neck
            pal.put('R', Color.valueOf("#dddddd")); // Ribs
            pal.put('B', Color.valueOf("#bbbbbb")); // Pelvis
            pal.put('L', Color.valueOf("#dddddd")); // Legs
            pal.put('F', Color.valueOf("#aaaaaa")); // Feet

        } else if (type.equals("ORC")) {
            pattern = new String[]{
                "................",
                ".....GGGGGG.....",
                "....GGGGGGGG....",
                "....GGRGGRGG....",
                "....GGGGGGGG....",
                "....WGGGGGGW....", // W = Tusk
                ".....GGGGGG.....",
                "....AAMMMMAA....",
                "....MMMMMMMM....",
                "....MMMMMMMM....",
                "....BBBBBBBB....",
                ".....LL..LL.....",
                ".....LL..LL.....",
                ".....FF..FF.....",
                "................",
                "................"
            };
            pal.put('G', Color.valueOf("#448822")); // Green Skin
            pal.put('R', Color.RED); // Eyes
            pal.put('W', Color.WHITE); // Tusks
            pal.put('A', Color.valueOf("#553311")); // Leather Armor
            pal.put('M', Color.valueOf("#448822")); // Muscles
            pal.put('B', Color.valueOf("#3e2723")); // Belt
            pal.put('L', Color.valueOf("#448822")); // Legs
            pal.put('F', Color.valueOf("#221100")); // Feet

        } else if (type.equals("BAT")) {
            pattern = new String[]{
                "................",
                "................",
                "................",
                "W..............W",
                ".W............W.",
                "..W...BBBB...W..",
                "...W.BBBBBB.W...",
                "....BBBYBBYBB...",
                "....BBBBBBBB....",
                ".....BBBBBB.....",
                "......BBBB......",
                ".......BB.......",
                "................",
                "................",
                "................",
                "................"
            };
            pal.put('B', Color.valueOf("#444444")); // Body
            pal.put('W', Color.valueOf("#666666")); // Wings
            pal.put('Y', Color.YELLOW); // Eyes

        } else if (type.equals("BOSS")) { // Dragon
            pattern = new String[]{
                ".......R.R......", // Horns
                "......RRRRR.....",
                ".....RRRRRRR....",
                "W...RRGYRGYRR...W",
                ".W..RRRRRRRRR..W.",
                "..W..RRRRRRR..W..",
                "...W.BBBBBBB.W...",
                "....BBBBBBBBB....",
                "....BBBBBBBBB....",
                "....BBBBBBBBB....",
                ".....BBBBBBB.....",
                ".....LL...LL.....",
                ".....LL...LL.....",
                "....CC.....CC....",
                "................",
                "................"
            };
            pal.put('R', Color.valueOf("#aa0000")); // Head
            pal.put('G', Color.GREEN); // Eyes
            pal.put('Y', Color.YELLOW); // Pupil/Nostril
            pal.put('B', Color.valueOf("#880000")); // Body
            pal.put('W', Color.valueOf("#660000")); // Wings
            pal.put('L', Color.valueOf("#880000")); // Legs
            pal.put('C', Color.valueOf("#440000")); // Claws
        }
        
        drawPattern(p, pattern, pal);
        return toTexture(p);
    }

    public static Texture createItem(String name) {
        Pixmap p = createPixmap();
        String[] pattern = new String[16];
        Map<Character, Color> pal = new HashMap<>();
        
        // Default palette
        pal.put('G', Color.GOLD);
        pal.put('W', Color.WHITE);
        pal.put('B', Color.BROWN);
        pal.put('S', Color.LIGHT_GRAY);
        pal.put('I', Color.GRAY);
        pal.put('R', Color.RED);
        pal.put('L', Color.BLUE);
        
        // Determine item type based on name
        
        if (name.contains("Sword") || name.contains("Blade")) {
            pattern = new String[]{
                ".............SS.",
                "............SS..",
                "...........SS...",
                "..........SS....",
                ".........SS.....",
                "........SS......",
                ".......SS.......",
                "......SS........",
                ".....SS.........",
                "....HH..........",
                "...H..H.........",
                "..H....H........",
                ".......H........",
                "................",
                "................",
                "................"
            };
            pal.put('S', Color.valueOf("#dddddd")); // Blade
            pal.put('H', Color.valueOf("#8d6e63")); // Hilt
            if (name.contains("Gold") || name.contains("Legendary")) pal.put('S', Color.GOLD);
            if (name.contains("Rusty")) pal.put('S', Color.valueOf("#8d6e63"));

        } else if (name.contains("Axe")) {
             pattern = new String[]{
                "................",
                "......SSSSS.....",
                "....SS.....S....",
                "...S.......S....",
                "..S.......H.....",
                "..S......H......",
                "...S....H.......",
                "....SSSSH.......",
                "........H.......",
                "........H.......",
                "........H.......",
                "........H.......",
                "........H.......",
                "........H.......",
                "................",
                "................"
            };
            pal.put('S', Color.GRAY);
            pal.put('H', Color.valueOf("#5d4037"));

        } else if (name.contains("Wand") || name.contains("Staff")) {
            pattern = new String[]{
                "..............O.",
                ".............O.O",
                "............O.O.",
                ".............O..",
                "...........H....",
                "..........H.....",
                ".........H......",
                "........H.......",
                ".......H........",
                "......H.........",
                ".....H..........",
                "....H...........",
                "...H............",
                "..H.............",
                "................",
                "................"
            };
            pal.put('H', Color.valueOf("#5d4037"));
            pal.put('O', name.contains("Magic") ? Color.MAGENTA : Color.CYAN);

        } else if (name.contains("Potion") || name.contains("Elixir")) {
            pattern = new String[]{
                "................",
                "................",
                ".......KK.......",
                ".......KK.......",
                "......L..L......",
                ".....LLLLLL.....",
                ".....LLLLLL.....",
                "....LLLLLLLL....",
                "....LLLLLLLL....",
                "....LLLLLLLL....",
                "....LLLLLLLL....",
                ".....LLLLLL.....",
                ".....LLLLLL.....",
                "................",
                "................",
                "................"
            };
            pal.put('K', Color.valueOf("#cccccc")); // Cork
            Color liquid = Color.RED;
            if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
            if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;
            pal.put('L', liquid);

        } else if (name.contains("Shield")) {
            pattern = new String[]{
                "................",
                "................",
                ".....BBBBBB.....",
                "....B......B....",
                "....B..GG..B....",
                "....B..GG..B....",
                "....B......B....",
                "....B......B....",
                ".....B....B.....",
                ".....B....B.....",
                "......B..B......",
                ".......BB.......",
                "................",
                "................",
                "................",
                "................"
            };
            Color shieldColor = Color.valueOf("#5d4037"); // Wood
            if (name.contains("Iron")) shieldColor = Color.GRAY;
            pal.put('B', shieldColor);
            pal.put('G', Color.DARK_GRAY);

        } else if (name.contains("Armor") || name.contains("Mail")) {
             pattern = new String[]{
                "................",
                "....S......S....",
                "...SS......SS...",
                "..SSS......SSS..",
                "..SSSSSSSSSSSS..",
                "..SSSSSSSSSSSS..",
                "..SSSSSSSSSSSS..",
                "..SSSSSSSSSSSS..",
                "..SSSSSSSSSSSS..",
                "...SSSSSSSSSS...",
                "...SSSSSSSSSS...",
                "....SSSSSSSS....",
                "................",
                "................",
                "................",
                "................"
            };
            Color armorColor = Color.valueOf("#8d6e63"); // Leather
            if (name.contains("Iron") || name.contains("Plate")) armorColor = Color.LIGHT_GRAY;
            pal.put('S', armorColor);
            
        } else if (name.contains("Ring")) {
             pattern = new String[]{
                "................",
                "................",
                "................",
                "......GGGG......",
                ".....G....G.....",
                "....G......G....",
                "....G......G....",
                "....G..J...G....",
                "....G......G....",
                "....G......G.....",
                ".....G....G.....",
                "......GGGG......",
                "................",
                "................",
                "................",
                "................"
            };
            pal.put('G', Color.GOLD);
            pal.put('J', name.contains("Defense") ? Color.CYAN : Color.RED); // Gem
            
        } else if (name.contains("Scroll")) {
             pattern = new String[]{
                "................",
                "................",
                ".......PP.......",
                "......P..P......",
                ".....P....P.....",
                "....P......P....",
                "...P........P...",
                "...P...AA...P...",
                "...P...AA...P...",
                "...P........P...",
                "....P......P....",
                ".....P....P.....",
                "......P..P......",
                ".......PP.......",
                "................",
                "................"
            };
            pal.put('P', Color.valueOf("#fff9c4")); // Paper
            pal.put('A', Color.BLACK); // Glyph

        } else if (name.contains("Coin") || name.contains("Gold")) {
             pattern = new String[]{
                "................",
                "................",
                "................",
                "................",
                "......GGGG......",
                ".....G....G.....",
                ".....G.DD.G.....",
                ".....G....G.....",
                ".....G.DD.G.....",
                ".....G....G.....",
                "......GGGG......",
                "................",
                "................",
                "................",
                "................",
                "................"
            };
            pal.put('G', Color.GOLD);
            pal.put('D', Color.valueOf("#ffb300")); // Darker Gold

        } else {
            // Default "New Item" Box
            pattern = new String[]{
                "................",
                "................",
                "..QQQQQQQQQQQQ..",
                "..Q..........Q..",
                "..Q...????...Q..",
                "..Q...????...Q..",
                "..Q.....??...Q..",
                "..Q.....??...Q..",
                "..Q.....??...Q..",
                "..Q..........Q..",
                "..Q.....??...Q..",
                "..Q.....??...Q..",
                "..Q..........Q..",
                "..QQQQQQQQQQQQ..",
                "................",
                "................"
            };
            pal.put('Q', Color.MAGENTA);
            pal.put('?', Color.WHITE);
        }

        drawPattern(p, pattern, pal);
        return toTexture(p);
    }
}
