package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.magicdungeon.screens.LoadingScreen;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class NewGameDialog extends BaseDialog {
    private VisTextField saveNameField;
    private VisTextField playerNameField;

    public NewGameDialog() {
        super("创建新游戏");
        setModal(true);
        addCloseButton();
        
        initUI();
        pack();
        centerWindow();
    }

    private void initUI() {
        VisTable content = new VisTable();
        content.defaults().pad(5);

        content.add(new VisLabel("存档名称:")).right();
        saveNameField = new VisTextField("MySave");
        content.add(saveNameField).width(200).row();

        content.add(new VisLabel("玩家名称:")).right();
        playerNameField = new VisTextField("Player");
        content.add(playerNameField).width(200).row();

        add(content).pad(20).row();

        VisTable buttons = new VisTable();
        buttons.defaults().pad(10).width(100);

        VisTextButton createBtn = new VisTextButton("创建");
        createBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createGame();
            }
        });

        VisTextButton cancelBtn = new VisTextButton("取消");
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });

        buttons.add(createBtn);
        buttons.add(cancelBtn);

        add(buttons).padBottom(10);
    }

    private void createGame() {
        String saveName = saveNameField.getText().trim();
        String playerName = playerNameField.getText().trim();

        if (saveName.isEmpty() || playerName.isEmpty()) {
            // TODO: Show error
            return;
        }

        try {
            // Check if save exists
            // Ideally SaveManager should have a check method
            // For now, let LoadingScreen handle it or catch exception
            
            close();
            ScreenManager.getInstance().setCurScreen(new LoadingScreen(saveName, playerName, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
