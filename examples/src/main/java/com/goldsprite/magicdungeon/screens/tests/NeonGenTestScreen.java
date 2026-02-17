package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class NeonGenTestScreen extends ExampleGScreen {
    private NeonBatch neonBatch;
    private FrameBuffer frameBuffer;
    private TextureRegion resultRegion;
    private Image previewImage; // Stretch
    private Image actualImage;  // Actual Size

    private int targetSize = 64;
    private int previewLayoutSize = 512; // 预览面板显示大小

    private VisTable uiRoot;
    private VisTable contentTable;
    private NeonPreviewActor livePreviewActor;

    // 用于 FB 渲染的相机
    private OrthographicCamera fbCamera;
    private Stage stage;

    @Override
    public String getIntroduction() {
        return "NeonBatch 动态生成纹理测试\n" +
                "1. Live: 实时矢量绘制 (无损)\n" +
                "2. Texture(Stretch): FB捕获结果 (拉伸)\n" +
                "3. Texture(Actual): FB捕获结果 (1:1 原大)";
    }

    @Override
    public void create() {
        stage = new Stage(uiViewport);

        neonBatch = new NeonBatch();
        fbCamera = new OrthographicCamera();

        // UI Setup
        uiRoot = new VisTable();
        uiRoot.setFillParent(true);

        getImp().addProcessor(stage);

        // 1. Controls (Top Left)
        VisTable controls = new VisTable();
        controls.setBackground("list");
        controls.pad(10);

        controls.add(new VisLabel("Gen Size:")).padRight(5);
        controls.add(new SmartNumInput("Px", targetSize, 0.1f, (val) -> {
            int newSize = val.intValue();
            if (newSize < 16) newSize = 16;
            if (newSize != targetSize) {
                targetSize = newSize;
                regenerate();
            }
        })).width(120).padRight(20);

        controls.add(new VisLabel("Preview Size:")).padRight(5);
        controls.add(new SmartNumInput("Px", previewLayoutSize, 5, (val) -> {
            int newSize = val.intValue();
            if (newSize < 100) newSize = 100;
            if (newSize != previewLayoutSize) {
                previewLayoutSize = newSize;
                rebuildLayout();
            }
        })).width(120);

        uiRoot.add(controls).top().left().pad(20);
        uiRoot.row();

        // 2. Main Content Area
        contentTable = new VisTable();
        uiRoot.add(contentTable).expand().fill();

        stage.addActor(uiRoot);

        rebuildLayout();
    }

    private void rebuildLayout() {
        contentTable.clear();

        // Left: Live Render Actor
        livePreviewActor = new NeonPreviewActor();
        contentTable.add(createPanel("Live Vector (Stretch)", livePreviewActor)).pad(10);

        // Middle: Texture Result (Stretch)
        previewImage = new Image();
        previewImage.setScaling(Scaling.stretch);
        contentTable.add(createPanel("Texture (Stretch)", previewImage)).pad(10);

        // Right: Texture Result (Actual)
        actualImage = new Image();
        actualImage.setScaling(Scaling.none); // 1:1
        actualImage.setAlign(Align.center);
        contentTable.add(createPanel("Texture (Actual)", actualImage)).pad(10);

        regenerate();
    }

    private VisTable createPanel(String title, Actor content) {
        VisTable panel = new VisTable();
        panel.add(new VisLabel(title)).padBottom(5).row();

        Container<Actor> container = new Container<>(content);
        container.setBackground(new TextureRegionDrawable(neonBatch.getBlankRegion()).tint(new Color(0.2f, 0.2f, 0.2f, 1f)));
        container.fill(); // [核心修复] 强制内容填满容器
        // 强制容器大小为预览大小
        panel.add(container).size(previewLayoutSize, previewLayoutSize);
        return panel;
    }

    private void regenerate() {
        // Update Live Preview Size (Actor size matches Layout size for Stretch effect)
        if (livePreviewActor != null) {
            livePreviewActor.setSize(previewLayoutSize, previewLayoutSize);
        }

        // --- FrameBuffer Generation ---
        if (frameBuffer != null) {
            frameBuffer.dispose();
            frameBuffer = null;
        }

        // Safety check for targetSize
        if (targetSize <= 0) targetSize = 64;
        if (targetSize > 4096) targetSize = 4096;

        try {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, targetSize, targetSize, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        fbCamera.setToOrtho(false, targetSize, targetSize);
        fbCamera.update();

        try {
            frameBuffer.begin();
            ScreenUtils.clear(0, 0, 0, 0); // Transparent clear

            neonBatch.setProjectionMatrix(fbCamera.combined);
            neonBatch.begin();

            // Draw Content at target resolution
            drawPlayer(neonBatch, targetSize);

            neonBatch.end();
            frameBuffer.end();

            Texture tex = frameBuffer.getColorBufferTexture();
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

            // Flip Y because FrameBuffer is upside down
            resultRegion = new TextureRegion(tex);
            resultRegion.flip(false, true);

            TextureRegionDrawable drawable = new TextureRegionDrawable(resultRegion);

            if (previewImage != null) {
                previewImage.setDrawable(drawable);
                previewImage.setSize(previewLayoutSize, previewLayoutSize);
            }

            if (actualImage != null) {
                actualImage.setDrawable(drawable);
                // For actual size, we set the actor size to texture size
                actualImage.setSize(targetSize, targetSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (neonBatch.isDrawing()) neonBatch.end();
            // Try to end framebuffer if possible (unsafe to check 'isDrawing' equivalent easily without reflection or tracking)
            // But usually exceptions happen inside neonBatch calls.
            // We just ensure we don't crash the whole app.
            try { frameBuffer.end(); } catch(Exception ex) {}
        }
    }

    /**
     * 复刻 SpriteGenerator.createPlayer
     * @param size 绘制目标区域的边长
     */
    private void drawPlayer(NeonBatch batch, float size) {
        // Colors
        Color skin = Color.valueOf("#ffccaa");
        Color armor = Color.valueOf("#2196F3");
        Color darkArmor = Color.valueOf("#1565C0");
        Color gold = Color.GOLD;
        Color helmet = Color.valueOf("#CFD8DC");
        Color darkHelmet = Color.valueOf("#90A4AE");
        Color legs = Color.valueOf("#8d6e63");
        Color boots = Color.valueOf("#3E2723");

        // 1. Legs
        drawRectPix(batch, size, 90, 180, 25, 60, legs);
        drawRectPix(batch, size, 141, 180, 25, 60, legs);

        // Boots
        int bootY = 215;
        int bootW = 65;
        int bootH = 45;
        drawRectPix(batch, size, 53, bootY, bootW, bootH, boots);
        drawRectPix(batch, size, 138, bootY, bootW, bootH, boots);

        // Boot Detail
        Color bootLight = boots.cpy().mul(1.2f);
        drawRectPix(batch, size, 53, bootY + bootH - 10, bootW, 10, Color.BLACK); // Soles
        drawRectPix(batch, size, 138, bootY + bootH - 10, bootW, 10, Color.BLACK);
        drawRectPix(batch, size, 53 + 5, bootY + 5, 10, 20, bootLight); // Highlights
        drawRectPix(batch, size, 138 + 5, bootY + 5, 10, 20, bootLight);

        // 2. Body (Armor)
        drawRectPix(batch, size, 70, 100, 116, 90, armor); // Main Chest
        drawRectPix(batch, size, 50, 90, 30, 40, darkArmor); // Shoulder Pads
        drawRectPix(batch, size, 176, 90, 30, 40, darkArmor);

        drawRectPix(batch, size, 80, 110, 96, 70, darkArmor); // Chest Plate Detail
        drawRectPix(batch, size, 118, 110, 20, 70, gold); // Center strip

        // Arms
        drawRectPix(batch, size, 40, 100, 25, 70, darkArmor); // Left
        drawRectPix(batch, size, 191, 100, 25, 70, darkArmor); // Right
        // Hands
        drawRectPix(batch, size, 40, 170, 25, 25, skin);
        drawRectPix(batch, size, 191, 170, 25, 25, skin);

        // Belt
        drawRectPix(batch, size, 70, 180, 116, 15, Color.valueOf("#3e2723"));
        drawRectPix(batch, size, 118, 180, 20, 15, Color.GOLD);

        // 3. Head
        int headW = 76;
        int headH = 64;
        int headX = 128 - headW/2;
        int headY = 36;
        drawRectPix(batch, size, headX, headY, headW, headH, skin);

        // 4. Helmet
        drawRectPix(batch, size, headX - 5, headY - 10, headW + 10, 30, helmet); // Top Dome
        drawRectPix(batch, size, headX - 5, headY + 10, 15, headH, darkHelmet); // Sides
        drawRectPix(batch, size, headX + headW - 10, headY + 10, 15, headH, darkHelmet);
        drawRectPix(batch, size, 128 - 5, headY - 20, 10, 20, Color.RED); // Crest

        // 5. Face Details
        int eyeY = headY + 30;
        drawRectPix(batch, size, 128 - 20, eyeY, 12, 12, Color.BLACK);
        drawRectPix(batch, size, 128 + 8, eyeY, 12, 12, Color.BLACK);
        drawRectPix(batch, size, 128 - 18, eyeY + 2, 4, 4, Color.WHITE);
        drawRectPix(batch, size, 128 + 10, eyeY + 2, 4, 4, Color.WHITE);

        // === 6. NeonBatch 新特性验证 (Overlay) ===
        
        // 6.1 验证 drawTriangleStrip (渐变三角形带) - 绘制一个飘带
        float[] ribbonVerts = new float[] {
            10, 10,
            30, 40,
            50, 10,
            70, 40,
            90, 10
        };
        float[] ribbonColors = new float[] {
            Color.RED.toFloatBits(),
            Color.ORANGE.toFloatBits(),
            Color.YELLOW.toFloatBits(),
            Color.GREEN.toFloatBits(),
            Color.CYAN.toFloatBits()
        };
        // 缩放到当前 size (假设 ribbonVerts 是基于 100x100 的)
        float s = size / 100f;
        for(int i=0; i<ribbonVerts.length; i++) ribbonVerts[i] *= s;
        
        batch.drawTriangleStrip(ribbonVerts, ribbonColors, 5);

        // 6.2 验证 drawSkewGradientRect - 绘制 HP Bar 风格
        // 在头顶画一个血条
        float barW = size * 0.8f;
        float barH = size * 0.1f;
        float barX = (size - barW) / 2;
        float barY = size * 0.9f;
        batch.drawSkewGradientRect(barX, barY, barW, barH, size * 0.05f, Color.RED, Color.ORANGE);
        
        // 6.3 验证描边 (Stroke) - 画一个圆圈在脚下
        batch.drawCircle(size/2, size*0.1f, size*0.3f, 2f, Color.CYAN, 32, false);
    }

    /**
     * 辅助方法：使用 256x256 的 Pixmap 坐标系进行绘制，内部自动转为 UV 并应用到当前 size
     * Pixmap 坐标: (0,0) 在左上角, Y 向下
     * Neon 坐标: (0,0) 在左下角, Y 向上
     */
    private void drawRectPix(NeonBatch batch, float size, float px, float py, float w, float h, Color color) {
        float base = 256f;
        float u = px / base;
        // Y 轴翻转核心逻辑
        float v = 1.0f - (py + h) / base;
        float uw = w / base;
        float vh = h / base;

        batch.drawRect(u * size, v * size, uw * size, vh * size, 0, 0, color, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
        if (neonBatch != null) neonBatch.dispose();
    }

    // --- Inner Actor ---
    class NeonPreviewActor extends Actor {

        public NeonPreviewActor() {
            // Container.fill() will set the size, but we can set a default
            setSize(previewLayoutSize, previewLayoutSize);
        }

        @Override
        public void draw(Batch sb, float parentAlpha) {
            try {
                sb.end(); // Pause Stage batch

                // Setup NeonBatch
                neonBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
                neonBatch.begin();

                // [核心修复] 使用更稳健的坐标获取方式
                Vector2 pos = localToStageCoordinates(new Vector2(0, 0));

                // Translate to Actor's position
                neonBatch.getTransformMatrix().idt().translate(pos.x, pos.y, 0);

                // Draw using Actor's width as the scale
                // 这样无论 previewLayoutSize 是多少，都会完整绘制出 Player
                drawPlayer(neonBatch, getWidth());

                // Reset
                neonBatch.getTransformMatrix().idt();
                neonBatch.end();
            } catch (Exception e) {
                e.printStackTrace();
                // Ensure batch is ended if it was begun
                if (neonBatch.isDrawing()) neonBatch.end();
            } finally {
                // Always resume stage batch
                sb.begin();
            }
        }
    }
}
