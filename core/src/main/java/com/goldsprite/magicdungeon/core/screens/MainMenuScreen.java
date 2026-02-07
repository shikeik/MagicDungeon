package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.core.renderer.TitleParallaxRenderer;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.utils.Constants;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.badlogic.gdx.math.MathUtils;

public class MainMenuScreen extends GScreen {
	private NeonBatch batch;
	private TitleParallaxRenderer renderer;
	private Stage stage;

	// UI Elements for animation
	private Table mainTable;
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

		// 2. Init UI Stage
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		buildUI();
	}

	private void buildUI() {
		mainTable = new VisTable();
		// mainTable.setFillParent(true); // Don't fill parent, we want to move it freely
		// We set size to stage size but position it manually
		mainTable.setSize(400, 600); // Fixed width column
		mainTable.setPosition(-500, 0); // Start off-screen (Left)
		mainTable.center();

		stage.addActor(mainTable);

		// Title
		VisLabel titleLabel = new VisLabel("MAGIC DUNGEON");
		titleLabel.setFontScale(1.5f);
		titleLabel.setColor(Color.CYAN);
		mainTable.add(titleLabel).padBottom(50).row();

		// Seed Input
		VisTable seedTable = new VisTable();
		seedTable.add(new VisLabel("Seed: ")).padRight(5);
		seedField = new VisTextField(String.valueOf(MathUtils.random(100000)));
		seedTable.add(seedField).width(150).padRight(5);

		VisTextButton randomSeedBtn = new VisTextButton("Dice");
		randomSeedBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				seedField.setText(String.valueOf(MathUtils.random(1000000)));
			}
		});
		seedTable.add(randomSeedBtn).width(50);
		mainTable.add(seedTable).padBottom(30).row();

		// Buttons
		addMenuButton("New Game", new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				startGame();
			}
		});

		if (SaveManager.hasSave()) {
			addMenuButton("Continue", new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					continueGame();
				}
			});
		}

		addMenuButton("Exit", new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});
	}

	private void addMenuButton(String text, ClickListener listener) {
		VisTextButton btn = new VisTextButton(text);
		btn.addListener(listener);
		mainTable.add(btn).width(220).height(60).padBottom(20).row();
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
		// We don't dispose here immediately if we want transition, but GScreen usually handles it.
		// GScreen manager usually disposes old screen.
	}

	private void continueGame() {
		GameScreen gameScreen = new GameScreen();
		getScreenManager().setCurScreen(gameScreen);
		gameScreen.loadGame();
	}

	@Override
	public void show() {
		super.show();
		// Trigger Entrance Animation
		// Move Table from -500 to 50 (Padding left)
		mainTable.clearActions();
		mainTable.setPosition(-500, (getUIViewport().getWorldHeight() - mainTable.getHeight()) / 2); // Vertically centered

		mainTable.addAction(Actions.sequence(
			Actions.delay(0.2f), // Slight delay before entering
			Actions.moveTo(50, mainTable.getY(), 0.8f, Interpolation.swingOut)
		));
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		// 1. Draw Background
		// Use UI Viewport size for renderer to match screen
		renderer.render(batch, delta, getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight());

		// 2. Draw UI
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// Ensure table stays centered vertically on resize
		if(mainTable != null) {
			mainTable.setY((getUIViewport().getWorldHeight() - mainTable.getHeight()) / 2);
		}
	}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (stage != null) stage.dispose();
		super.dispose();
	}
}
