package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.single.DialogUI;
import com.goldsprite.magicdungeon.model.SaveData;
import com.goldsprite.magicdungeon.screens.LoadingScreen;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTextButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LoadGameDialog extends BaseDialog {
    private VisList<SaveData> saveList;
    private VisTextButton loadBtn;
    private VisTextButton deleteBtn;
    private VisLabel infoLabel;

    public LoadGameDialog() {
        super("选择存档");
        initUI();
    }

    @Override
    public com.kotcrab.vis.ui.widget.VisDialog show(com.badlogic.gdx.scenes.scene2d.Stage stage) {
        // [UI调整] 始终使用舞台尺寸的 3/5
        float w = stage.getWidth() * 0.6f;
        float h = stage.getHeight() * 0.6f;
        setSize(w, h);
        centerWindow();
        return super.show(stage);
    }

    private void initUI() {
        getContentTable().defaults().pad(5);

        saveList = new VisList<>();
        List<SaveData> saves = SaveManager.listSaves();
        saveList.setItems(saves.toArray(new SaveData[0]));
        
        saveList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                updateSelection();
            }
        });

        VisScrollPane scrollPane = new VisScrollPane(saveList);
        scrollPane.setFadeScrollBars(false);
        getContentTable().add(scrollPane).expand().fill().row();

        infoLabel = new VisLabel("请选择存档");
        getContentTable().add(infoLabel).expandX().fillX().pad(10).row();
        
        getButtonsTable().defaults().pad(10).width(80);
        
        loadBtn = new VisTextButton("加载");
        loadBtn.setDisabled(true);
        loadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!loadBtn.isDisabled()) {
                    loadGame();
                }
            }
        });

        deleteBtn = new VisTextButton("删除");
        deleteBtn.setDisabled(true);
        deleteBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!deleteBtn.isDisabled()) {
                    deleteGame();
                }
            }
        });

        VisTextButton cancelBtn = new VisTextButton("取消");
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });

        getButtonsTable().add(loadBtn);
        getButtonsTable().add(deleteBtn);
        getButtonsTable().add(cancelBtn);
        getButtonsTable().pack();
    }
    
    private void updateSelection() {
        SaveData selected = saveList.getSelected();
        if (selected != null) {
            loadBtn.setDisabled(false);
            deleteBtn.setDisabled(false);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String info = "玩家: " + selected.currentPlayerName + "\n" +
                          "最后游玩: " + sdf.format(new Date(selected.lastPlayedTime)) + "\n" +
                          "区域: " + selected.currentAreaId + " (层 " + selected.currentFloor + ")";
            infoLabel.setText(info);
        } else {
            loadBtn.setDisabled(true);
            deleteBtn.setDisabled(true);
            infoLabel.setText("请选择存档");
        }
    }

    private void loadGame() {
        SaveData selected = saveList.getSelected();
        if (selected != null) {
            close();
            ScreenManager.getInstance().goScreen(new LoadingScreen(selected.saveName, selected.currentPlayerName, false));
        }
    }

    private void deleteGame() {
        SaveData selected = saveList.getSelected();
        if (selected == null) return;
        
        DialogUI.confirm("删除确认", "确定要删除存档 '" + selected.saveName + "' 吗?", () -> {
            try {
                SaveManager.deleteSave(selected.saveName);
                List<SaveData> saves = SaveManager.listSaves();
                saveList.setItems(saves.toArray(new SaveData[0]));
                updateSelection();
            } catch (Exception e) {
                DialogUI.show("错误", "删除失败: " + e.getMessage());
            }
        });
    }
}
