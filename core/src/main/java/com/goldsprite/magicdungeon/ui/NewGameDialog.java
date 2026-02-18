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
import com.goldsprite.gdengine.ui.widget.single.DialogUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;

public class NewGameDialog extends BaseDialog {
    private VisTextField saveNameField;
    private VisTextField playerNameField;

    public NewGameDialog() {
        super("创建新游戏");
        
        initUI();
        pack();
        centerWindow();
    }

    private void initUI() {
        VisTable content = new VisTable();
        content.defaults().pad(5);

        content.add(new VisLabel("存档名称:")).right();
        
        // [Task 3] 智能生成不重名的默认存档名
        String defaultSaveName = "MySave";
        int i = 1;
        while (SaveManager.hasSave(defaultSaveName)) {
            defaultSaveName = "MySave" + i++;
        }
        
        saveNameField = new VisTextField(defaultSaveName);
        content.add(saveNameField).width(200).row();

        content.add(new VisLabel("玩家名称:")).right();
        playerNameField = new VisTextField("Player");
        content.add(playerNameField).width(200).row();

        getContentTable().add(content).pad(20).row(); // Use getContentTable()

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

        getButtonsTable().add(buttons).padBottom(10); // Use getButtonsTable()
    }

    private void createGame() {
        String saveName = saveNameField.getText().trim();
        String playerName = playerNameField.getText().trim();

        if (saveName.isEmpty() || playerName.isEmpty()) {
            Dialogs.showOKDialog(getStage(), "错误", "存档名或玩家名不能为空!");
            return;
        }

        if (SaveManager.hasSave(saveName)) {
            DialogUI.confirm(getStage(), "确认", "存档 '" + saveName + "' 已存在, 是否覆盖?", () -> {
                try {
                    SaveManager.deleteSave(saveName);
                    startGame(saveName, playerName);
                    throw new RuntimeException("存档创建成功");
                } catch (Exception e) {
                    Dialogs.showDetailsDialog(getStage(), "删除存档失败", "无法删除旧存档", e.getMessage());
                }
            });
            return;
        }

        startGame(saveName, playerName);
    }

    private void startGame(String saveName, String playerName) {
        close();
        ScreenManager.getInstance().goScreen(new LoadingScreen(saveName, playerName, true));
    }
}
