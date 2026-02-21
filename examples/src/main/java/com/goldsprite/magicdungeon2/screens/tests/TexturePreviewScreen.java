package com.goldsprite.magicdungeon2.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.goldsprite.magicdungeon2.assets.TextureManager;

/**
 * 纹理预览测试屏幕
 *
 * 以网格形式展示所有通过 JSON 绘制指令生成的纹理，
 * 每个纹理下方显示名称，支持鼠标拖拽平移和滚轮缩放。
 */
public class TexturePreviewScreen extends GScreen {
    private NeonBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BitmapFont font;
    private BitmapFont titleFont;

    /** 每个预览格子的尺寸（像素） */
    private static final float CELL_SIZE = 80;
    /** 格子间距 */
    private static final float PADDING = 20;
    /** 每行显示数量 */
    private static final int COLS = 6;

    @Override
    public void create() {
        batch = new NeonBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(960, 540, camera);
        viewport.apply(true);

        font = FontUtils.generate(12, 2);
        font.setColor(Color.WHITE);
        titleFont = FontUtils.generate(18, 3);
        titleFont.setColor(Color.CYAN);

        // 相机控制器（拖拽平移 + 滚轮缩放）
        SimpleCameraController controller = new SimpleCameraController(camera);
        controller.setCoordinateMapper((x, y) -> viewport.unproject(new Vector2(x, y)));
        getImp().addProcessor(controller);

        // 初始化纹理管理器（加载所有 JSON 绘制计划）
        TextureManager.init();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.08f, 0.12f, 1);
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        String[] names = TextureManager.getLoadedNames();
        java.util.Arrays.sort(names);

        batch.begin();

        // 绘制纹理网格
        float startX = -(COLS * (CELL_SIZE + PADDING)) / 2f;
        float startY = ((names.length / COLS + 1) * (CELL_SIZE + PADDING + 20)) / 2f;

        for (int i = 0; i < names.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            float x = startX + col * (CELL_SIZE + PADDING);
            float y = startY - row * (CELL_SIZE + PADDING + 20);

            TextureRegion region = TextureManager.get(names[i]);
            if (region != null) {
                // 绘制背景边框
                batch.drawRect(x - 2, y - 2, CELL_SIZE + 4, CELL_SIZE + 4, 0, 0, Color.DARK_GRAY, true);
                // 绘制纹理
                batch.draw(region, x, y, CELL_SIZE, CELL_SIZE);
            } else {
                // 缺失纹理的占位符
                batch.drawRect(x, y, CELL_SIZE, CELL_SIZE, 0, 0, Color.RED, true);
            }

            // 绘制名称标签
            font.draw(batch, names[i], x, y - 4);
        }

        batch.end();

        // HUD 叠加层
        batch.setProjectionMatrix(
            batch.getProjectionMatrix().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        titleFont.draw(batch,
            String.format("纹理预览 - 已加载 %d 个 | 拖拽平移 / 滚轮缩放 / ESC 返回",
                TextureManager.getLoadedCount()),
            20, Gdx.graphics.getHeight() - 20);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void hide() {
        super.hide();
    }
}
