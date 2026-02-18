package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.ai.AIDrawExecutor;
import com.goldsprite.magicdungeon.utils.AssetUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * AI 绘制测试场景：扫描 assets/ai_draw_cmds/ 下的所有 JSON 计划，
 * 显示为可选择的按钮网格，点击执行并显示预览
 */
public class AIDrawTestScreen extends GScreen {

    private Stage stage;
    private SpriteBatch batch;

    // 绘制计划列表
    private Array<AIDrawPlanFile> planFiles;
    private TextureRegion previewRegion;

    // UI 组件
    private VisImage preview;
    private VisLabel infoLabel;
    private VisTable planButtonsContainer;

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        batch = new SpriteBatch();
        stage = new Stage(getUIViewport(), batch);
        if (imp != null) imp.addProcessor(stage);

        // 扫描绘制计划文件
        scanDrawPlanFiles();

        // 构建 UI
        setupUI();
    }

    private void scanDrawPlanFiles() {
        planFiles = new Array<>();
        
        // 使用 AssetUtils 从 assets.txt 索引扫描文件列表
        String[] fileNames = AssetUtils.listNames("ai_draw_cmds");
        for (String fileName : fileNames) {
            if (fileName.endsWith(".json")) {
                FileHandle fh = Gdx.files.internal("ai_draw_cmds/" + fileName);
                if (fh.exists()) {
                    planFiles.add(new AIDrawPlanFile(fh));
                    DLog.logT("AIDrawTestScreen", "Found plan: %s", fileName);
                }
            }
        }
        
        planFiles.sort((a, b) -> a.displayName.compareTo(b.displayName));
        DLog.logT("AIDrawTestScreen", "Scanned %d draw plan files from assets.txt", planFiles.size);
    }

    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");

        // 顶部标题
        root.add(new VisLabel("AI 绘制计划预览（共 " + planFiles.size + " 项）")).pad(10).expandX().row();
        root.addSeparator().padBottom(5).expandX().fillX().row();

        // 主体：左侧预览（大） + 右侧按钮列表（小）
        VisTable mainTable = new VisTable(false);

        // 左侧：预览区（占 60%）
        VisTable previewPanel = new VisTable(false);
        previewPanel.add(new VisLabel("■ 预览")).padBottom(5).expandX().row();
        preview = new VisImage();
        preview.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        previewPanel.add(preview).fill().expand().size(450, 450).row();
        infoLabel = new VisLabel("未选择计划");
        previewPanel.add(infoLabel).expandX().fillX().padTop(5).row();

        // 右侧：按钮列表（竖向，占 40%）
        VisTable buttonPanel = new VisTable(false);
        buttonPanel.add(new VisLabel("■ 计划列表")).padBottom(5).expandX().row();

        planButtonsContainer = new VisTable(false);
        planButtonsContainer.defaults().fill().expandX().pad(3);

        if (planFiles.size == 0) {
            planButtonsContainer.add(new VisLabel("[红]无计划文件！")).row();
            DLog.logT("AIDrawTestScreen", "⚠️  No draw plan files found in ai_draw_cmds/");
        } else {
            // 竖向列表显示所有按钮
            for (final AIDrawPlanFile pf : planFiles) {
                VisTextButton btn = new VisTextButton(pf.displayName);
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        selectAndExecutePlan(pf);
                    }
                });
                planButtonsContainer.add(btn).fillX().row();
            }
        }

        VisScrollPane buttonScroll = new VisScrollPane(planButtonsContainer);
        buttonScroll.setScrollingDisabled(true, false);
        buttonPanel.add(buttonScroll).fill().expand().padTop(5).row();

        // 组合到主表：预览 60% + 按钮 40%
        mainTable.add(previewPanel).fill().expand().uniformX();
        mainTable.add(buttonPanel).fill().expand().uniformX();

        root.add(mainTable).fill().expand().padLeft(5).padRight(5).padBottom(5).row();

        stage.addActor(root);
        DLog.logT("AIDrawTestScreen", "UI setup complete with %d buttons", planFiles.size);
    }

    private void selectAndExecutePlan(AIDrawPlanFile pf) {
        DLog.logT("AIDrawTestScreen", "Executing plan: %s", pf.displayName);

        try {
            String json = pf.file.readString();
            previewRegion = AIDrawExecutor.generateFromJson(json);
            if (previewRegion != null) {
                preview.setDrawable(new TextureRegionDrawable(previewRegion));
                infoLabel.setText("✓ " + pf.displayName + " (" + previewRegion.getRegionWidth() + "x" + previewRegion.getRegionHeight() + ")");
            } else {
                infoLabel.setText("✗ 生成失败: " + pf.displayName);
                DLog.logT("AIDrawTestScreen", "Generation failed for: %s", pf.displayName);
            }
        } catch (Exception e) {
            infoLabel.setText("✗ 执行失败: " + e.getMessage());
            DLog.logErrT("AIDrawTestScreen", "Exception executing plan %s: %s", pf.displayName, e.getMessage());
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (previewRegion != null && previewRegion.getTexture() != null) {
            previewRegion.getTexture().dispose();
        }
        super.dispose();
    }

    /**
     * 绘制计划文件信息
     */
    private static class AIDrawPlanFile {
        FileHandle file;
        String displayName; // 去除 .json 后缀的文件名

        AIDrawPlanFile(FileHandle fh) {
            this.file = fh;
            String name = fh.nameWithoutExtension();
            // snake_case 转换为可读形式（例如 metallic_orb -> Metallic Orb）
            this.displayName = name.replaceAll("_", " ");
            this.displayName = this.displayName.replaceAll("([a-z])([A-Z])", "$1 $2");
            this.displayName = this.displayName.substring(0, 1).toUpperCase() + this.displayName.substring(1);
        }
    }
}
