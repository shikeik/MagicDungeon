package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.neonbatch.BloomRenderer;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon.assets.AudioAssets;
import com.goldsprite.magicdungeon.core.renderer.TitleParallaxRenderer;
import com.goldsprite.magicdungeon.core.ui.SettingsDialog;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import com.goldsprite.magicdungeon.utils.Constants;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.ui.LoadGameDialog;
import com.goldsprite.magicdungeon.ui.NewGameDialog;

public class MainMenuScreen extends GScreen {
	private NeonBatch batch;
	private BloomRenderer bloomRender;
	private TitleParallaxRenderer renderer;
	private Stage stage;

	// UI Elements for animation
	private VisLabel titleLabel;
	private Group menuGroup;
	private VisTextField seedField;
	private SettingsDialog settingsDialog;

	private final Array<VisTextButton> menuButtons = new Array<>();
	private int focusedIndex = -1;

	@Override
	protected void initViewport() {
		this.viewSizeShort = Constants.VIEWPORT_HEIGHT;
		this.viewSizeLong = Constants.VIEWPORT_WIDTH;
		this.uiViewportScale = 1.0f;
		super.initViewport();
	}

	@Override

	public void create() {
		//这里无需配置因为入口已配置了
		// // 配置 ScreenManager 的输入钩子
		// ScreenManager.inputUpdater = () -> InputManager.getInstance().update();
		// ScreenManager.backKeyTrigger = () -> InputManager.getInstance().isJustPressed(InputAction.BACK);

		// 1. 初始化渲染器
		batch = new NeonBatch();
		renderer = new TitleParallaxRenderer();
		bloomRender = new BloomRenderer();

		// 2. 初始化 UI 舞台
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		settingsDialog = new SettingsDialog();

		buildUI();

		AudioSystem.getInstance().playMusic(AudioAssets.MUSIC_SOLVE_THIS);
	}

	private void buildUI() {
		// 1. 标题标签 (居中)
		titleLabel = new VisLabel("魔塔地牢");
		titleLabel.setFontScale(1.5f);
		titleLabel.setColor(Color.CYAN);

		titleLabel.addAction(Actions.forever(
			Actions.sequence(
				Actions.scaleTo(1.1f, 1.1f, 1f),
				Actions.scaleTo(1f, 1f, 1f)
			)
		));
		titleLabel.setOrigin(Align.center);
		stage.addActor(titleLabel);

		// 2. 菜单组
		if (menuGroup != null) menuGroup.remove();
		menuGroup = new Group();
		stage.addActor(menuGroup);

		menuButtons.clear();
		focusedIndex = -1;

		/*
		// 种子输入区域
		VisTable seedTable = new VisTable();

		float targetX = 50; // 距左侧偏移
		float currentY = 50; // 从相对于中心的顶部开始
		float gap = 70;

		// 种子组
		seedTable.setSize(320, 50);
		seedTable.setPosition(targetX, currentY);

		seedTable.clearChildren();
		seedTable.left();
		seedTable.add(new VisLabel("种子: ")).padRight(0);
		seedField = new VisTextField(String.valueOf(MathUtils.random(100000)));
		seedField.setAlignment(Align.center);
		seedTable.add(seedField).expandX().fillX().padRight(5);
		VisTextButton randomSeedBtn = new VisTextButton("R");
		randomSeedBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				seedField.setText(String.valueOf(MathUtils.random(1000000)));
			}
		});
		seedTable.add(randomSeedBtn).width(30);

		menuGroup.addActor(seedTable);
		*/

		float targetX = 50;
		float currentY = 50;
		float gap = 70;

		currentY -= gap;

		// 按钮
		createMenuButton("新游戏", targetX, currentY, 0.1f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 使用 BaseDialog.show() 把窗口压入 GScreen.dialogStack，
				// 否则 ESC/BACK 无法被 GScreen.handleBackKey 拦截。
				new NewGameDialog().show(stage);
			}
		});
		currentY -= gap;

		if (!SaveManager.listSaves().isEmpty()) {
			createMenuButton("继续游戏", targetX, currentY, 0.2f, new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					new LoadGameDialog();
				}
			});
			currentY -= gap;
		}

		createMenuButton("设置", targetX, currentY, 0.25f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				openSettings();
			}
		});
		currentY -= gap;

		createMenuButton("退出游戏", targetX, currentY, 0.3f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		// 种子表动画
		// seedTable.addAction(createEntranceAction(0f));

		// 初始布局
		updateLayout();
	}

	private void updateLayout() {
		if (stage == null) return;
		float w = getUIViewport().getWorldWidth();
		float h = getUIViewport().getWorldHeight();

		if (titleLabel != null) {
			titleLabel.setPosition(
				(w - titleLabel.getPrefWidth()) / 2,
				h - 100
			);
		}

		if (menuGroup != null) {
			menuGroup.setPosition(0, h / 2);
		}
	}

	private void openSettings() {
		if (stage.getActors().contains(settingsDialog, true)) {
			settingsDialog.remove();
		}

		settingsDialog.show(stage);
	}

	private void createMenuButton(String text, float targetX, float targetY, float delay, ClickListener listener) {
		VisTextButton btn = new VisTextButton(text);
		btn.addListener(listener);
		btn.setSize(320, 60);
		btn.setPosition(targetX, targetY);
		menuGroup.addActor(btn);
		btn.addAction(createEntranceAction(delay));

		menuButtons.add(btn);
	}

	private Action createEntranceAction(float delay) {
		return Actions.sequence(
			Actions.alpha(0),
			Actions.moveBy(-300, 0),
			Actions.delay(delay),
			Actions.parallel(
				Actions.fadeIn(0.5f),
				Actions.moveBy(300, 0, 0.8f, Interpolation.swingOut)
			)
		);
	}

	/*
	private void startGame() {
		long seed = 0;
		try {
			seed = Long.parseLong(seedField.getText());
		} catch (NumberFormatException e) {
			seed = seedField.getText().hashCode();
		}

		final long finalSeed = seed;
		getScreenManager().playTransition(() -> {
			// [修改] 直接进入游戏 (默认为营地)，而不是 WorldMapScreen
			GameScreen gameScreen = new GameScreen(finalSeed);
			getScreenManager().setCurScreen(gameScreen);
		});
	}

	private void continueGame() {
		getScreenManager().playTransition(() -> {
			GameScreen gameScreen = new GameScreen();
			getScreenManager().setCurScreen(gameScreen);
			gameScreen.loadGame();
		});
	}
	*/

	@Override
	public void show() {
		super.show();
		stage.clear();
		buildUI();
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	@Override
	public void render0(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		float scl = 1f;
		bloomRender.captureStart(batch);
		batch.setProjectionMatrix(getUICamera().combined);
		renderer.render(batch, delta, getUIViewport().getWorldWidth() * scl, getUIViewport().getWorldHeight() * scl);
		bloomRender.captureEnd();
		bloomRender.process();
		bloomRender.render(batch);

		handleFocusInput();
		stage.act(delta);
		stage.draw();
	}

	private void handleFocusInput() {
		if (menuButtons.size == 0) return;

		InputManager input = InputManager.getInstance();
		boolean changed = false;

		// UI_DOWN -> Next Button (index increases)
		if (input.isJustPressed(InputAction.UI_DOWN)) {
			focusedIndex++;
			if (focusedIndex >= menuButtons.size) focusedIndex = 0;
			changed = true;
		} else if (input.isJustPressed(InputAction.UI_UP)) {
			focusedIndex--;
			if (focusedIndex < 0) focusedIndex = menuButtons.size - 1;
			changed = true;
		}

		// Initial focus
		if (focusedIndex == -1 && (changed || input.getInputMode() == InputManager.InputMode.KEYBOARD)) {
			focusedIndex = 0;
			changed = true;
		}

		if (changed) {
			for (int i = 0; i < menuButtons.size; i++) {
				VisTextButton btn = menuButtons.get(i);
				if (i == focusedIndex) {
					btn.setColor(Color.WHITE); // 保持原色，利用 focusBorder 高亮
					stage.setKeyboardFocus(btn); // 触发 VisUI 的 focusBorder 渲染
				} else {
					btn.setColor(Color.LIGHT_GRAY); // 未选中稍微压暗
				}
			}
		}

		if (input.isJustPressed(InputAction.UI_CONFIRM)) {
			if (focusedIndex >= 0 && focusedIndex < menuButtons.size) {
				VisTextButton btn = menuButtons.get(focusedIndex);
				InputEvent event = new InputEvent();
				event.setType(InputEvent.Type.touchDown);
				btn.fire(event);
				event.setType(InputEvent.Type.touchUp);
				btn.fire(event);
			}
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		updateLayout();
		bloomRender.resize((int) getViewSize().x, (int) getViewSize().y);
	}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (stage != null) stage.dispose();
		super.dispose();
	}
}
