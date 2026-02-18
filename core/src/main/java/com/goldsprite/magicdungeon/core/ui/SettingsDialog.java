package com.goldsprite.magicdungeon.core.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon.core.SettingsManager;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class SettingsDialog extends BaseDialog {
	private final SettingsManager settings;
	private final InputManager inputManager;

	// UI Components
	private VisTable contentTable;
	private VisTable inputTable;

	public SettingsDialog() {
		super("设置");
		this.settings = SettingsManager.getInstance();
		this.inputManager = InputManager.getInstance();

		autoPack = false;
		// BaseDialog already handles pack, modal, center, etc.
		setResizable(false);
		setMovable(true);

		centerWindow();

		buildUI();
	}

	@Override
	public VisDialog show(Stage stage) {
		float width = Math.max(900, stage.getWidth() * 2 / 3f);
		float height = stage.getHeight() * 4 / 5f;
		setSize(width, height);
		return super.show(stage);
	}

	private void buildUI() {
		VisTable mainTable = new VisTable();
		mainTable.pad(20);
		mainTable.top();

		// --- Tab Selection ---
		VisTable tabs = new VisTable();
		VisTextButton btnGeneral = new VisTextButton("常规设置");
		VisTextButton btnControls = new VisTextButton("按键设置");

		tabs.add(btnGeneral).width(150).padRight(10);
		tabs.add(btnControls).width(150);

		mainTable.add(tabs).padBottom(20).row();

		// --- Content Area ---
		contentTable = new VisTable();
		HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(contentTable);
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

		// Add main table to the content area of BaseDialog (VisDialog)
		getContentTable().add(mainTable).expand().fill();

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

	// ... (rest of methods: showGeneralSettings, showControlSettings, addInputRow, startRebinding)
	// Same as before, just kept concise for SearchReplace

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
		inputTable.add(new VisLabel("动作")).width(150).left();
		inputTable.add(new VisLabel("键盘")).width(150).left();
		inputTable.add(new VisLabel("手柄")).width(150).left();
		inputTable.row();
		inputTable.addSeparator().colspan(3).padBottom(10);

		for (InputAction action : InputAction.values()) {
			addInputRow(action);
		}

		contentTable.add(inputTable).expandX().fillX();
	}

	private void addInputRow(InputAction action) {
		VisLabel nameLabel = new VisLabel(action.name());

		// 键盘绑定
		int boundKey = inputManager.getBoundKey(action);
		String keyName = boundKey == -1 ? "无" : Input.Keys.toString(boundKey);
		VisTextButton bindKeyBtn = new VisTextButton(keyName);
		bindKeyBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				startRebinding(action, bindKeyBtn, true);
			}
		});

        // 手柄绑定
        int boundButton = inputManager.getBoundButton(action);
        String btnName = boundButton == -1 ? "无" : inputManager.getButtonName(boundButton);
        if(boundButton >= InputManager.VIRTUAL_AXIS_START) {
            btnName = "轴"; // 暂时简化或使用具体名称
        }
        VisTextButton bindControllerBtn = new VisTextButton(btnName);
        bindControllerBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startRebinding(action, bindControllerBtn, false);
            }
        });

		inputTable.add(nameLabel).left().padBottom(5);
		inputTable.add(bindKeyBtn).width(120).left().padBottom(5).padRight(10);
        inputTable.add(bindControllerBtn).width(120).left().padBottom(5);
		inputTable.row();
	}

	private void startRebinding(InputAction action, VisTextButton btn, boolean isKeyboard) {
		InputManager.getInstance().setInputBlocked(true);
		btn.setText("按任意键...");
		RebindDialog dialog = new RebindDialog(action, btn, isKeyboard);
		// 使用 BaseDialog.show() 将对话框加入 GScreen 的 dialogStack
		dialog.show(getStage());
	}

	private void saveAll() {
		settings.save();
		inputManager.saveMappings();
	}

	// --- 输入捕获内部类 ---
	private class RebindDialog extends BaseDialog {
		private final InputAction action;
		private final VisTextButton targetBtn;
        private final boolean isKeyboard;

        private final ControllerListener controllerListener = new ControllerAdapter() {
			@Override
			public boolean buttonDown(Controller controller, int buttonCode) {
				if(!isKeyboard) {
					DLog.logT("Settings", "正在重新绑定手柄按键: " + buttonCode);
					inputManager.rebindController(action, buttonCode);
					updateButtonLabel();
					close();
					return true;
				}
				return false;
			}

			@Override
			public boolean axisMoved(Controller controller, int axisCode, float value) {
				if(!isKeyboard && Math.abs(value) > 0.5f) {
					 // 映射摇杆到虚拟按键
					 int virtualCode = -1;
					 if(axisCode == InputManager.AXIS_X) {
						 virtualCode = value < 0 ? InputManager.AXIS_LEFT_LEFT : InputManager.AXIS_LEFT_RIGHT;
					 } else if(axisCode == InputManager.AXIS_Y) {
						 virtualCode = value < 0 ? InputManager.AXIS_LEFT_UP : InputManager.AXIS_LEFT_DOWN;
					 }

					 if(virtualCode != -1) {
						DLog.logT("Settings", "正在重新绑定手柄摇杆: " + virtualCode);
						inputManager.rebindController(action, virtualCode);
						updateButtonLabel();
						close();
						return true;
					 }
				}
				return false;
			}
		};

		public RebindDialog(InputAction action, VisTextButton targetBtn, boolean isKeyboard) {
			super("请按键");
			this.action = action;
			this.targetBtn = targetBtn;
			this.isKeyboard = isKeyboard;

			setModal(true);

			if(isKeyboard) {
				text("请按下用于 '" + action.name() + "' 的按键...\n(按 ESC 取消)");
			} else {
				text("请按下手柄按键或拨动摇杆...\n(按 ESC 取消)");
			}
			pack();
			centerWindow();

			// 捕获键盘输入
			addListener(new InputListener() {
				@Override
				public boolean keyDown(InputEvent event, int keycode) {
					if (keycode == Input.Keys.ESCAPE) {
						// 取消
						updateButtonLabel();
						close();
						return true;
					}

					if (isKeyboard) {
						// 有效按键
						String msg = "正在重绑定 " + action + " 为 " + Input.Keys.toString(keycode);
						DLog.logT("Settings", msg);
						inputManager.rebindKeyboard(action, keycode);
						updateButtonLabel();
						close();
						return true;
					}
					return false;
				}
			});

			if(!isKeyboard) {
				Controllers.addListener(controllerListener);
			}
		}

		private void updateButtonLabel() {
            if(isKeyboard) {
			    int key = inputManager.getBoundKey(action);
			    targetBtn.setText(Input.Keys.toString(key));
            } else {
                int btn = inputManager.getBoundButton(action);
                String name = inputManager.getButtonName(btn);
//                if(btn >= InputManager.VIRTUAL_AXIS_START) name = "Axis";
                targetBtn.setText(name);
            }
		}

		@Override
		public void setStage(Stage stage) {
			super.setStage(stage);
			if (stage != null) {
				stage.setKeyboardFocus(this);
			}
		}

        @Override
        public boolean remove() {
            inputManager.setInputBlocked(false);
            if(!isKeyboard) {
                Controllers.removeListener(controllerListener);
            }
            return super.remove();
        }
	}
}
