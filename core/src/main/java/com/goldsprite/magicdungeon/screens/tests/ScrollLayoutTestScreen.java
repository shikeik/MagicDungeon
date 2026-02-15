package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

public class ScrollLayoutTestScreen extends GScreen {
    private Stage stage;

    @Override
    public void create() {
        super.create();

        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        VisTable root = new VisTable();
        root.setFillParent(true);
        root.defaults().pad(20);

        // 左侧面板
        VisTable leftContent = new VisTable();
        for (int i = 0; i < 50; i++) {
            leftContent.add(new VisLabel("Left Item " + i)).row();
        }
        HoverFocusScrollPane leftPane = new HoverFocusScrollPane(leftContent);
        leftPane.setFadeScrollBars(false);
        leftPane.setScrollingDisabled(true, false);

        // 右侧面板
        VisTable rightContent = new VisTable();
        for (int i = 0; i < 50; i++) {
            rightContent.add(new VisLabel("Right Item " + i)).row();
        }
        HoverFocusScrollPane rightPane = new HoverFocusScrollPane(rightContent);
        rightPane.setFadeScrollBars(false);
        rightPane.setScrollingDisabled(true, false);

        VisTable leftContainer = new VisTable();
        leftContainer.add(new VisLabel("Hover Left to Scroll")).row();
        leftContainer.add(leftPane).size(300, 400);

        VisTable rightContainer = new VisTable();
        rightContainer.add(new VisLabel("Hover Right to Scroll")).row();
        rightContainer.add(rightPane).size(300, 400);

        root.add(leftContainer);
        root.add(rightContainer);

        stage.addActor(root);
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
        // GScreen.resize 已经调用了 initViewport 里的 update
        // 但 stage 也需要 update
        stage.getViewport().update(width, height, true);
    }
}
