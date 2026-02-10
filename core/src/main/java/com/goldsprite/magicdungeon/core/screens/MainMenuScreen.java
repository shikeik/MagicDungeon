package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.core.renderer.TitleParallaxRenderer;
import com.goldsprite.magicdungeon.core.ui.SettingsWindow;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.utils.Constants;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.BloomRenderer;

public class MainMenuScreen extends GScreen {
	private NeonBatch batch;
	private BloomRenderer bloomRender;
	private TitleParallaxRenderer renderer;
	private Stage stage;

	// UI Elements for animation
	private VisLabel titleLabel;
	private Group menuGroup;
	private VisTextField seedField;

	@Override
	protected void initViewport() {
		this.viewSizeShort = Constants.VIEWPORT_HEIGHT;
		this.viewSizeLong = Constants.VIEWPORT_WIDTH;
		this.uiViewportScale = PlatformImpl.isDesktopUser() ? 0.6f : 1.0f;
		super.initViewport();
	}

	@Override
	public void create() {
		// 1. Init Renderer
		batch = new NeonBatch();
		renderer = new TitleParallaxRenderer();
		bloomRender = new BloomRenderer();

		// 2. Init UI Stage
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		buildUI();
	}

	private void buildUI() {
		// 1. Title Label (Centered)
		titleLabel = new VisLabel("MAGIC DUNGEON");
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

		// 2. Menu Group
		if (menuGroup != null) menuGroup.remove();
		menuGroup = new Group();
		stage.addActor(menuGroup);

		// Seed Input Area
 		VisTable seedTable = new VisTable();
		
		float targetX = 50; // X offset from left
		float currentY = 50; // Start from top-ish relative to center
 		float gap = 70;

 		// Seed Group
 		seedTable.setSize(220, 50); 
 		seedTable.setPosition(targetX, currentY);

 		seedTable.clearChildren();
 		seedTable.left(); 
 		seedTable.add(new VisLabel("Seed: ")).width(50).padRight(5); 
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

 		currentY -= gap;

		// Buttons
		createMenuButton("New Game", targetX, currentY, 0.1f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				startGame();
			}
		});

		currentY -= gap;

		if (SaveManager.hasSave()) {
			createMenuButton("Continue", targetX, currentY, 0.2f, new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					continueGame();
				}
			});
			currentY -= gap;
		}
		
		// [New] Settings Button
		createMenuButton("Settings", targetX, currentY, 0.25f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				openSettings();
			}
		});
		currentY -= gap;

		createMenuButton("Exit", targetX, currentY, 0.3f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		// Seed table animation
		seedTable.addAction(createEntranceAction(0f));
		
		// Initial Layout
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
		if (stage.getRoot().findActor("SettingsWindow") != null) return;
		
		SettingsWindow win = new SettingsWindow();
		win.setName("SettingsWindow");
		stage.addActor(win);
		win.fadeIn();
	}

	private void createMenuButton(String text, float targetX, float targetY, float delay, ClickListener listener) {
		VisTextButton btn = new VisTextButton(text);
		btn.addListener(listener);
		btn.setSize(220, 60);
		btn.setPosition(targetX, targetY);
		menuGroup.addActor(btn);
		btn.addAction(createEntranceAction(delay));
	}

	private com.badlogic.gdx.scenes.scene2d.Action createEntranceAction(float delay) {
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

	private void startGame() {
		long seed = 0;
		try {
			seed = Long.parseLong(seedField.getText());
		} catch (NumberFormatException e) {
			seed = seedField.getText().hashCode();
		}

		GameScreen gameScreen = new GameScreen(seed);
		getScreenManager().setCurScreen(gameScreen);
	}

	private void continueGame() {
		GameScreen gameScreen = new GameScreen();
		getScreenManager().setCurScreen(gameScreen);
		gameScreen.loadGame();
	}

	@Override
	public void show() {
		super.show();
		stage.clear(); 
		buildUI();     
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		float scl = 1f;
		bloomRender.captureStart(batch);
		batch.setProjectionMatrix(getUICamera().combined);
		renderer.render(batch, delta, getUIViewport().getWorldWidth()*scl, getUIViewport().getWorldHeight()*scl);
		bloomRender.captureEnd();
		bloomRender.process();
		bloomRender.render(batch);
		
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		updateLayout();
		bloomRender.resize((int)getViewSize().x, (int)getViewSize().y);
	}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (stage != null) stage.dispose();
		super.dispose();
	}
}
