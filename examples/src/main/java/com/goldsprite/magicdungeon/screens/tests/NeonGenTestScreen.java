package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.InputProcessor;
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

public class NeonGenTestScreen extends ExampleGScreen {
    private NeonBatch neonBatch;
    private FrameBuffer frameBuffer;
    private TextureRegion resultRegion;
    private Image previewImage;
    private int targetSize = 256;

    private VisTable uiRoot;
    private VisTable contentTable;
    private NeonPreviewActor livePreviewActor;

    // 用于 FB 渲染的相机
    private OrthographicCamera fbCamera;
	private Stage stage;

	@Override
    public String getIntroduction() {
        return "NeonBatch 动态生成纹理测试\n" +
                "左侧(Live): 实时 NeonBatch 绘制\n" +
                "右侧(Texture): FrameBuffer 捕获结果";
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
        controls.setBackground("window-bg");
        controls.pad(10);

        controls.add(new VisLabel("Size:")).padRight(10);
        controls.add(new SmartNumInput("Px", targetSize, 32, (val) -> {
            int newSize = val.intValue();
            // 限制一下最小尺寸，防止崩溃
            if (newSize < 16) newSize = 16;
            if (newSize != targetSize) {
                targetSize = newSize;
                regenerate();
            }
        })).width(150);

        uiRoot.add(controls).top().left().pad(20);
        uiRoot.row();

        // 2. Main Content Area
        contentTable = new VisTable();

        // Left: Live Render Actor
        Table leftPanel = new Table();
        leftPanel.add(new VisLabel("Live Render (NeonBatch)")).padBottom(10).row();

        livePreviewActor = new NeonPreviewActor();
        // 用 Container 包裹以便居中显示
        Container<NeonPreviewActor> leftContainer = new Container<>(livePreviewActor);
        leftContainer.setBackground(new TextureRegionDrawable(neonBatch.getBlankRegion()).tint(new Color(0.2f, 0.2f, 0.2f, 1f))); // Dark BG
        leftPanel.add(leftContainer).size(400, 400);

        // Right: Texture Result
        Table rightPanel = new Table();
        rightPanel.add(new VisLabel("Result Texture (FrameBuffer)")).padBottom(10).row();

        previewImage = new Image();
        Container<Image> rightContainer = new Container<>(previewImage);
        rightContainer.setBackground(new TextureRegionDrawable(neonBatch.getBlankRegion()).tint(new Color(0.2f, 0.2f, 0.2f, 1f))); // Dark BG
        rightPanel.add(rightContainer).size(400, 400);

        contentTable.add(leftPanel).expand().fill().pad(20);
        contentTable.add(rightPanel).expand().fill().pad(20);

        uiRoot.add(contentTable).expand().fill();

        stage.addActor(uiRoot);

        regenerate();
    }

    private void regenerate() {
        // Update Live Preview Size
        livePreviewActor.setSize(targetSize, targetSize);

        // --- FrameBuffer Generation ---
        if (frameBuffer != null) frameBuffer.dispose();

        try {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, targetSize, targetSize, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        fbCamera.setToOrtho(false, targetSize, targetSize);
        fbCamera.update();

        frameBuffer.begin();
        ScreenUtils.clear(0, 0, 0, 0); // Transparent clear

        neonBatch.setProjectionMatrix(fbCamera.combined);
        neonBatch.begin();

        // Draw Content (at 0,0)
        drawTestContent(targetSize);

        neonBatch.end();
        frameBuffer.end();

        Texture tex = frameBuffer.getColorBufferTexture();
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Flip Y because FrameBuffer is upside down
        resultRegion = new TextureRegion(tex);
        resultRegion.flip(false, true);

        previewImage.setDrawable(new TextureRegionDrawable(resultRegion));

        // Resize image to fit container but keep aspect ratio (pixelated look)
        // actually just setting the size of the image widget to targetSize is enough,
        // the Container will center it.
        contentTable.findActor("Preview");
        // But previewImage reference is enough
        previewImage.setSize(targetSize, targetSize);
        // Scaling usually handled by Image scaling, but we want 1:1 or integer scale?
        // Let's just set size to targetSize.
    }

    /**
     * Draw actual content. Assumes (0,0) is bottom-left.
     */
    private void drawTestContent(float size) {
        float cx = size / 2;
        float cy = size / 2;

        // Background Circle
        neonBatch.drawCircle(cx, cy, size * 0.45f, 4, Color.GOLD, 64, false);
        neonBatch.drawCircle(cx, cy, size * 0.40f, 0, new Color(1, 0.8f, 0, 0.3f), 64, true);

        // Star
        neonBatch.drawStar(cx, cy, size * 0.35f, size * 0.15f, 5, 0, 2, Color.RED, true);
        neonBatch.drawStar(cx, cy, size * 0.35f, size * 0.15f, 5, 0, 2, Color.ORANGE, false);

        // Rects (simulate lines or bars)
        neonBatch.drawRect(cx - size*0.2f, cy - size*0.1f, size*0.4f, size*0.05f, 0, 2, Color.CYAN, true);

        // Gradient Rect (HP Bar style)
        neonBatch.drawSkewGradientRect(cx - size*0.3f, cy - size*0.3f, size*0.6f, size*0.1f, size*0.05f, Color.GREEN, Color.LIME);
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
            setSize(targetSize, targetSize);
        }

        @Override
        public void draw(Batch sb, float parentAlpha) {
            sb.end(); // Pause Stage batch

            // Setup NeonBatch
            neonBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
            neonBatch.begin();

            Vector2 pos = localToStageCoordinates(new Vector2(0, 0));

            // Translate to Actor's position
            neonBatch.getTransformMatrix().idt().translate(pos.x, pos.y, 0);

            // Draw
            drawTestContent(targetSize);

            // Reset
            neonBatch.getTransformMatrix().idt();
            neonBatch.end();

            sb.begin(); // Resume Stage batch
        }
    }
}
