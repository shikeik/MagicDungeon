package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.CustomAtlasLoader;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.utils.SimpleCameraController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MaterialPreviewScreen extends GScreen {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BitmapFont font;
    private BitmapFont headerFont;

    private List<PreviewItem> previews;
    private static final int ICON_SIZE = 64;
    private static final int PADDING = 20;
    
    // 定义要展示的 Atlas 列表 (Path -> Title)
    private static final Map<String, String> TARGET_ATLASES = new LinkedHashMap<>();
    static {
        TARGET_ATLASES.put("sprites/all_blocks_sheet.png", "方块 (Blocks)");
        TARGET_ATLASES.put("sprites/all_items_sheet.png", "物品 (Items)");
        TARGET_ATLASES.put("sprites/all_entities_sheet.png", "实体 (Entities)");
    }

    private static class PreviewItem {
        TextureRegion region;
        String name;
        float x, y;
        boolean isHeader; // 是否为标题项

        public PreviewItem(TextureRegion region, String name, float x, float y) {
            this(region, name, x, y, false);
        }

        public PreviewItem(TextureRegion region, String name, float x, float y, boolean isHeader) {
            this.region = region;
            this.name = name;
            this.x = x;
            this.y = y;
            this.isHeader = isHeader;
        }
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        // 使用 ExtendViewport 保持宽高比并适配不同分辨率
        viewport = new ExtendViewport(1280, 720, camera);
        
        font = FontUtils.generate(16, 3);
        font.setColor(Color.WHITE);
        
        headerFont = FontUtils.generate(32, 3);
        headerFont.setColor(Color.YELLOW);

        previews = new ArrayList<>();
        
        CustomAtlasLoader loader = CustomAtlasLoader.inst();
        
        int colsPerRow = 6;
        float startX = 50;
        float startY = 300; // 起始高度
        float currentY = startY;

        for (Map.Entry<String, String> entry : TARGET_ATLASES.entrySet()) {
            String path = entry.getKey();
            String title = entry.getValue();

            // 添加标题
            previews.add(new PreviewItem(null, title, startX, currentY, true));
            currentY -= 60; // 标题高度

            // 获取所有 Region 名称
            Set<String> names = loader.getRegionNames(path);
            
            int col = 0;
            int row = 0;
            
            for (String name : names) {
                TextureRegion reg = loader.getRegion(path, name);
                String displayName = loader.getDisplayName(path, name);
                
                if (reg != null) {
                    if (col >= colsPerRow) {
                        col = 0;
                        row++;
                    }
                    
                    float x = startX + col * (ICON_SIZE + PADDING + 40);
                    float y = currentY - row * (ICON_SIZE + PADDING + 40);
                    
                    previews.add(new PreviewItem(reg, displayName, x, y));
                    col++;
                }
            }
            
            // 计算下一组的起始 Y 坐标
            if (!names.isEmpty()) {
                currentY = currentY - (row + 1) * (ICON_SIZE + PADDING + 40) - 40; // 额外间距
            } else {
                 currentY -= 40;
            }
        }

        // Camera Controller
        SimpleCameraController controller = new SimpleCameraController(camera);
        controller.setCoordinateMapper((x, y) -> viewport.unproject(new Vector2(x, y)));
        getImp().addProcessor(controller);
        
        // 初始相机位置居中
        camera.position.set(640, 360, 0);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1);

        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        for (PreviewItem item : previews) {
            if (item.isHeader) {
                headerFont.draw(batch, item.name, item.x, item.y);
            } else {
                batch.draw(item.region, item.x, item.y, ICON_SIZE, ICON_SIZE);
                font.draw(batch, item.name, item.x, item.y - 10);
            }
        }
        batch.end();

        // HUD
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        font.draw(batch, "Material Preview Mode", 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "Drag to Pan, Scroll to Zoom", 20, Gdx.graphics.getHeight() - 40);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (headerFont != null) headerFont.dispose();
    }
}
