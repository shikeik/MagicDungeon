package com.goldsprite.magicdungeon.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.magicdungeon.core.SettingsManager;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;

public class SettingsWindow extends VisWindow {

    private final SettingsManager settings;
    private final InputManager inputManager;
    
    // UI Components
    private VisTable contentTable;
    private VisTable inputTable;
    
    public SettingsWindow() {
        super("设置");
        this.settings = SettingsManager.getInstance();
        this.inputManager = InputManager.getInstance();
        
        setModal(true);
        setCenterOnAdd(true);
        setResizable(false);
        setMovable(true);
        addCloseButton();
        
        setSize(800, 600);
        centerWindow();
        
        buildUI();
    }
    
    private void buildUI() {
        VisTable mainTable = new VisTable();
        mainTable.pad(20);
        mainTable.top();
        
        // --- Tab Selection (Simple Buttons for now) ---
        VisTable tabs = new VisTable();
        VisTextButton btnGeneral = new VisTextButton("常规设置");
        VisTextButton btnControls = new VisTextButton("按键设置");
        
        tabs.add(btnGeneral).width(150).padRight(10);
        tabs.add(btnControls).width(150);
        
        mainTable.add(tabs).padBottom(20).row();
        
        // --- Content Area ---
        contentTable = new VisTable();
        VisScrollPane scrollPane = new VisScrollPane(contentTable);
        scrollPane.setFadeScrollBars(false);
        
        mainTable.add(scrollPane).expand().fill().row();
        
        // --- Buttons ---
        VisTextButton btnSave = new VisTextButton("保存");
        btnSave.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveAll();
                close();
            }
        });
        
        mainTable.add(btnSave).width(120).padTop(10);
        
        add(mainTable).expand().fill();
        
        // Listeners for tabs
        btnGeneral.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showGeneralSettings();
            }
        });
        
        btnControls.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showControlSettings();
            }
        });
        
        // Default View
        showGeneralSettings();
    }
    
    private void showGeneralSettings() {
        contentTable.clearChildren();
        contentTable.top().left();
        
        // Music Volume
        contentTable.add(new VisLabel("音乐音量")).left().padBottom(5).row();
        VisSlider musicSlider = new VisSlider(0, 1, 0.05f, false);
        musicSlider.setValue(settings.getMusicVolume());
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setMusicVolume(musicSlider.getValue());
            }
        });
        contentTable.add(musicSlider).width(300).padBottom(20).row();
        
        // SFX Volume
        contentTable.add(new VisLabel("音效音量")).left().padBottom(5).row();
        VisSlider sfxSlider = new VisSlider(0, 1, 0.05f, false);
        sfxSlider.setValue(settings.getSfxVolume());
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setSfxVolume(sfxSlider.getValue());
            }
        });
        contentTable.add(sfxSlider).width(300).padBottom(20).row();
        
        // Fullscreen
        VisCheckBox fullscreenCheck = new VisCheckBox("全屏模式");
        fullscreenCheck.setChecked(settings.isFullscreen());
        fullscreenCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setFullscreen(fullscreenCheck.isChecked());
            }
        });
        contentTable.add(fullscreenCheck).left().padBottom(20).row();
    }
    
    private void showControlSettings() {
        contentTable.clearChildren();
        contentTable.top();
        
        inputTable = new VisTable();
        inputTable.top();
        
        // Header
        inputTable.add(new VisLabel("动作")).width(200).left();
        inputTable.add(new VisLabel("按键 (点击修改)")).width(200).left();
        inputTable.row();
        inputTable.addSeparator().colspan(2).padBottom(10);
        
        for (InputAction action : InputAction.values()) {
            addInputRow(action);
        }
        
        contentTable.add(inputTable).expandX().fillX();
    }
    
    private void addInputRow(InputAction action) {
        VisLabel nameLabel = new VisLabel(action.name());
        
        int boundKey = inputManager.getBoundKey(action);
        String keyName = boundKey == -1 ? "None" : Input.Keys.toString(boundKey);
        
        VisTextButton bindBtn = new VisTextButton(keyName);
        bindBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startRebinding(action, bindBtn);
            }
        });
        
        inputTable.add(nameLabel).left().padBottom(5);
        inputTable.add(bindBtn).width(150).left().padBottom(5);
        inputTable.row();
    }
    
    private void startRebinding(InputAction action, VisTextButton btn) {
        btn.setText("按任意键...");
        
        // Create a modal dialog to capture input
        RebindDialog dialog = new RebindDialog(action, btn);
        getStage().addActor(dialog);
    }
    
    private void saveAll() {
        settings.save();
        inputManager.saveMappings();
    }
    
    // --- Inner Class for Input Capture ---
    private class RebindDialog extends VisDialog {
        private final InputAction action;
        private final VisTextButton targetBtn;
        
        public RebindDialog(InputAction action, VisTextButton targetBtn) {
            super("请按键");
            this.action = action;
            this.targetBtn = targetBtn;
            
            setModal(true);
            
            text("请按下用于 '" + action.name() + "' 的按键...\n(按 ESC 取消)");
            pack();
            centerWindow();
            
            // Capture keyboard input
            addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ESCAPE) {
                        // Cancel
                        updateButtonLabel();
                        close();
                        return true;
                    }
                    
                    // Valid key
                    com.goldsprite.gdengine.log.Debug.logT("Settings", "Rebinding " + action + " to " + Input.Keys.toString(keycode));
                    inputManager.rebindKeyboard(action, keycode);
                    updateButtonLabel();
                    close();
                    return true;
                }
            });
        }
        
        private void updateButtonLabel() {
            int key = inputManager.getBoundKey(action);
            targetBtn.setText(Input.Keys.toString(key));
        }
        
        @Override
        public void setStage(Stage stage) {
            super.setStage(stage);
            if (stage != null) {
                stage.setKeyboardFocus(this);
            }
        }
    }
}
