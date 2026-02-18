package com.goldsprite.magicdungeon.screens.tests.dualgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class DualGridRenderer {
    private final TextureRegion[][] atlas = new TextureRegion[TerrainType.values().length][16];
    private static final int[] MASK_TO_ATLAS_X = {
        -1, // 0: 0000 (全空)
        1, // 1: 0001 (只有BR) -> 外转角: 右下 (1,3)
        0, // 2: 0010 (只有BL) -> 外转角: 左下 (0,0)
        3, // 3: 0011 (BL+BR)  -> 底部边缘 (3,0)
        0, // 4: 0100 (只有TR) -> 外转角: 右上 (0,2)
        1, // 5: 0101 (TR+BR)  -> 右侧边缘 (1,0)
        2, // 6: 0110 (TR+BL)  -> 对角线 (2,3)
        1, // 7: 0111 (TR+BL+BR) -> 内转角: 左上 (3,1)
        3, // 8: 1000 (只有TL) -> 外转角: 左上 (3,3)
        0, // 9: 1001 (TL+BR)  -> 对角线 (0,1)
        3, // 10: 1010 (TL+BL) -> 左侧边缘 (3,2)
        2, // 11: 1011 (TL+BL+BR) -> 内转角: 右上 (2,2)
        1, // 12: 1100 (TL+TR) -> 顶部边缘 (1,2)
        2, // 13: 1101 (TL+TR+BR) -> 内转角: 左下 (2,0)
        3, // 14: 1110 (TL+TR+BL) -> 内转角: 右下 (1,1)
        2  // 15: 1111 (全满)  -> 中心块 (2,1)
    };
    private static final int[] MASK_TO_ATLAS_Y = {
        -1, // 0
        3, // 1
        0, // 2
        0, // 3
        2, // 4
        0, // 5
        3, // 6
        1, // 7
        3, // 8
        1, // 9
        2, // 10
        0, // 11
        2, // 12
        2, // 13
        1, // 14
        1  // 15
    };

    public void load() {
        for (TerrainType type : TerrainType.values()) {
            if (type == TerrainType.AIR) continue;
            Texture tex = new Texture(Gdx.files.internal(type.texPath));
            TextureRegion[][] temp = TextureRegion.split(tex, type.sourceSize, type.sourceSize);
            for (int i = 0; i < 16; i++) {
                atlas[type.id][i] = temp[i / 4][i % 4];
            }
        }
    }

    public TextureRegion getIcon(TerrainType type) {
        if (type == TerrainType.AIR) return null;
        // Return the tile at (2,2) (index 10) as requested
        // Row 2 (0-indexed) is the 3rd row. Col 2 is 3rd col.
        // Index = 2 * 4 + 2 = 10.
        return atlas[type.id][10];
    }

    public void renderLayer(SpriteBatch batch, GridData grid, int layerIndex) {
        // 在这一层里，我们需要对所有可能的 TerrainType 进行一遍 DualGrid 渲染
        for (TerrainType type : TerrainType.values()) {
            if (type == TerrainType.AIR) continue;

            for (int x = 0; x <= DualGridConfig.GRID_W; x++) {
                for (int y = 0; y <= DualGridConfig.GRID_H; y++) {
                    int mask = calculateMask(grid, layerIndex, x, y, type);
                    if (mask <= 0) continue;

                    int tx = MASK_TO_ATLAS_X[mask];
                    int ty = MASK_TO_ATLAS_Y[mask];
                    if (tx == -1) continue;

                    float drawX = x * DualGridConfig.TILE_SIZE - DualGridConfig.DISPLAY_OFFSET;
                    float drawY = y * DualGridConfig.TILE_SIZE - DualGridConfig.DISPLAY_OFFSET;
                    batch.draw(atlas[type.id][ty * 4 + tx], drawX, drawY, DualGridConfig.TILE_SIZE, DualGridConfig.TILE_SIZE);
                }
            }
        }
    }

    private int calculateMask(GridData grid, int layer, int x, int y, TerrainType target) {
        int tr = (grid.getTileId(layer, x, y) == target.id) ? 1 : 0;
        int tl = (grid.getTileId(layer, x - 1, y) == target.id) ? 1 : 0;
        int br = (grid.getTileId(layer, x, y - 1) == target.id) ? 1 : 0;
        int bl = (grid.getTileId(layer, x - 1, y - 1) == target.id) ? 1 : 0;
        return (tl << 3) | (tr << 2) | (bl << 1) | br;
    }

    public void dispose() {
        for (int i = 0; i < atlas.length; i++) {
            if (atlas[i][0] != null && atlas[i][0].getTexture() != null) {
                atlas[i][0].getTexture().dispose();
            }
        }
    }
}
