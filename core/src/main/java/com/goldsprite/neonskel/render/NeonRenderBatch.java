package com.goldsprite.neonskel.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public interface NeonRenderBatch {
    void drawRect(float x, float y, float width, float height, float rotation, float strokeWidth, Color color, boolean filled);
    void drawCircle(float x, float y, float radius, float strokeWidth, Color color, int segments, boolean filled);
    void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation);
}
