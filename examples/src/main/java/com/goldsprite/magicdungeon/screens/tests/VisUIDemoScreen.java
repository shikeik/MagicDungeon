package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;

public class VisUIDemoScreen extends GScreen {
    private Stage stage;
    private VisTable root;

    // 当前使用的皮肤模式：true=Shimmer(VisUI默认), false=Neutralizer(GameSkin)
    private boolean useDefaultSkin = true;

    @Override
    public void create() {
        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        rebuildUI();
    }

    private void rebuildUI() {
        if (root != null) {
            root.remove();
        }

        Skin currentSkin = VisUI.getSkin();

        root = new VisTable();
        root.setSkin(currentSkin); // 关键：设置 Table 的 Skin，这样 setBackground("name") 才能从正确的 Skin 查找
        root.setFillParent(true);
        try {
            root.setBackground("window-bg");
        } catch (Exception e) {
            // 如果 currentSkin 依然没有 window-bg，尝试回退到 VisUI.getSkin()
            if (currentSkin != VisUI.getSkin()) {
                 try {
                     root.setBackground(VisUI.getSkin().getDrawable("window-bg"));
                 } catch (Exception ex) {
                     // 彻底放弃背景，避免崩溃
                 }
            }
        }

        stage.addActor(root);

        // title
        Label title = new Label(useDefaultSkin ? "通用 UI (Shimmer) 演示" : "游戏 UI (Neutralizer) 演示", currentSkin);
        if (useDefaultSkin) {
             // VisUI default uses "default" label style, usually fine.
             // If we want title style:
             title = new Label("通用 UI (Shimmer) 演示", currentSkin, "title");
        } else {
             title = new Label("游戏 UI (Neutralizer) 演示", currentSkin, "title");
        }

        title.setFontScale(1.5f);
        root.add(title).pad(20).colspan(2).row();

        // Toggle Skin Button
        TextButton switchSkinBtn = new TextButton("切换皮肤 / Switch Skin", currentSkin, "toggle");

        switchSkinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                useDefaultSkin = !useDefaultSkin;
                rebuildUI();
            }
        });
        root.add(switchSkinBtn).colspan(2).padBottom(20).row();

        // Split into two columns
        Table leftCol = new Table();
        leftCol.defaults().pad(5).left();
        Table rightCol = new Table();
        rightCol.defaults().pad(5).left();

        root.add(leftCol).expand().fill().top().pad(20);
        root.add(rightCol).expand().fill().top().pad(20);

        // --- Left Column: Basic Widgets ---

        // Buttons
        leftCol.add(new Label("--- Buttons ---", currentSkin)).row();
        Table btnTable = new Table();

        btnTable.add(new TextButton("普通按钮", currentSkin)).padRight(10);

        TextButton toggleBtn = new TextButton("切换按钮", currentSkin, "toggle");
        btnTable.add(toggleBtn).padRight(10);

        TextButton disabledBtn = new TextButton("禁用按钮", currentSkin);
        disabledBtn.setDisabled(true);
        btnTable.add(disabledBtn);

        leftCol.add(btnTable).row();

        // Checkboxes & Radios
        leftCol.add(new Label("--- CheckBox & Radio ---", currentSkin)).padTop(10).row();
        Table checkTable = new Table();
        checkTable.add(new CheckBox("复选框 A", currentSkin)).padRight(10);
        CheckBox cbB = new CheckBox("复选框 B", currentSkin);
        cbB.setChecked(true);
        checkTable.add(cbB).padRight(10);

        CheckBox disabledCheck = new CheckBox("禁用", currentSkin);
        disabledCheck.setChecked(true);
        disabledCheck.setDisabled(true);
        checkTable.add(disabledCheck);
        leftCol.add(checkTable).row();

        Table radioTable = new Table();
        ButtonGroup<CheckBox> group = new ButtonGroup<>();
        CheckBox r1 = new CheckBox("单选 1", currentSkin, "radio");
        CheckBox r2 = new CheckBox("单选 2", currentSkin, "radio");
        group.add(r1);
        group.add(r2);
        radioTable.add(r1).padRight(10);
        radioTable.add(r2);
        leftCol.add(radioTable).row();

        // Inputs
        leftCol.add(new Label("--- Inputs ---", currentSkin)).padTop(10).row();
        TextField field1 = new TextField("文本输入框", currentSkin);
        leftCol.add(field1).width(200).row();

        TextField field2 = new TextField("密码输入框", currentSkin);
        field2.setPasswordMode(true);
        field2.setPasswordCharacter('*');
        leftCol.add(field2).width(200).row();

        // --- Right Column: Advanced Widgets ---

        // SelectBox
        rightCol.add(new Label("--- SelectBox ---", currentSkin)).row();
        SelectBox<String> selectBox = new SelectBox<>(currentSkin);
        selectBox.setItems("选项 A", "选项 B", "选项 C", "非常长的选项测试");
        rightCol.add(selectBox).width(150).row();

        // Slider & Progress
        rightCol.add(new Label("--- Slider & Progress ---", currentSkin)).padTop(10).row();
        Slider slider = new Slider(0, 100, 1, false, currentSkin);
        slider.setValue(50);
        rightCol.add(slider).width(200).row();

        ProgressBar progress = new ProgressBar(0, 100, 1, false, currentSkin);
        progress.setValue(30);
        progress.setAnimateDuration(0.5f);
        rightCol.add(progress).width(200).row();

        // Link slider to progress
        Slider finalSlider = slider;
        ProgressBar finalProgress = progress;
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                finalProgress.setValue(finalSlider.getValue());
            }
        });

        // Window
        rightCol.add(new Label("--- Window ---", currentSkin)).padTop(10).row();
        TextButton openWinBtn = new TextButton("打开测试窗口", currentSkin);
        rightCol.add(openWinBtn).row();

        openWinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                createTestWindow(currentSkin);
            }
        });

        // Tree
        rightCol.add(new Label("--- Tree ---", currentSkin)).padTop(10).row();
        Tree<SimpleNode, String> tree = new Tree<>(currentSkin);

        SimpleNode node1 = new SimpleNode("根节点 1", currentSkin);
        SimpleNode node2 = new SimpleNode("根节点 2", currentSkin);
        node1.add(new SimpleNode("子节点 A", currentSkin));
        node1.add(new SimpleNode("子节点 B", currentSkin));
        tree.add(node1);
        tree.add(node2);
        node1.setExpanded(true);

        // ScrollPane
        ScrollPane scrollPane = new HoverFocusScrollPane(tree, currentSkin, "list"); // Shimmer uses "list" for scrollpane usually? Or default.
        // VisUI uses "list" style for scrollpane background.
        // Let's try "list" or "default".
        // Neutralizer has "list"? VisUIHelper maps "default" to "list" if missing.
        // Let's use "nobackground" or "default".
        // Use "default" for safety.

        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        rightCol.add(scrollPane).expand().fill().row();
    }

    private static class SimpleNode extends Tree.Node<SimpleNode, String, Label> {
        public SimpleNode(String text, Skin skin) {
            super(new Label(text, skin));
            setValue(text);
        }
    }

    private void createTestWindow(Skin skin) {
        Window window = new Window("测试窗口", skin);
        window.setModal(false);
        window.setMovable(true);
        // window.addCloseButton(); // Standard Window doesn't have this, manual impl needed if wanted

        // Manual Close Button
        TextButton closeBtn = new TextButton("X", skin);
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        window.getTitleTable().add(closeBtn).height(window.getPadTop());

        window.add(new Label("这是一个标准的 LibGDX Window", skin)).pad(20).row();
        window.add(new TextButton("确定", skin)).padBottom(10);

        window.pack();
        stage.addActor(window);
        window.setPosition(stage.getWidth() / 2, stage.getHeight() / 2, Align.center);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
}
