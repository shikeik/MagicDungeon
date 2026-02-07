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
import com.badlogic.gdx.utils.Align;
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
import com.goldsprite.gdengine.neonbatch.BloomRenderer;

public class MainMenuScreen extends GScreen {
	private NeonBatch batch;
	private BloomRenderer bloomRender;
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
		bloomRender = new BloomRenderer();

		// 2. Init UI Stage
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		buildUI();
	}

	private void buildUI() {
		// 1. Title Label (Centered)
		VisLabel titleLabel = new VisLabel("MAGIC DUNGEON");
		titleLabel.setFontScale(1.5f);
		titleLabel.setColor(Color.CYAN);
		// Position title at top center
		titleLabel.setPosition(
			(getUIViewport().getWorldWidth() - titleLabel.getPrefWidth()) / 2,
			getUIViewport().getWorldHeight() - 100
		);
		// Add simple pulse action to title
		titleLabel.addAction(Actions.forever(
			Actions.sequence(
				Actions.scaleTo(1.1f, 1.1f, 1f),
				Actions.scaleTo(1f, 1f, 1f)
			)
		));
		// Set origin for scaling
		titleLabel.setOrigin(Align.center);
		stage.addActor(titleLabel);

		// 2. Main Menu Container (Left Side)
		mainTable = new VisTable();
		mainTable.setSize(300, 400); // Fixed width column
		// Initial Position: Off-screen Left, Vertically Centered
		float targetX = 50;
		float startX = -350;
		float centerY = (getUIViewport().getWorldHeight() - mainTable.getHeight()) / 2;

		mainTable.setPosition(startX, centerY);
		// mainTable.debug(); // For debug

		stage.addActor(mainTable);

		// Seed Input Area
 		VisTable seedTable = new VisTable();
 		// Content is added later during layout
 		// Just initialize here

		float startY = centerY + 150;
 		float gap = 70;

 		// Seed Group
 		// Ensure width matches buttons (220)
 		seedTable.setSize(220, 50); // Set explicit size
 		seedTable.setPosition(targetX, startY);

 		// Re-layout seed table to ensure alignment
 		seedTable.clearChildren();
 		seedTable.left(); // Align left content
 		seedTable.add(new VisLabel("Seed: ")).width(50).padRight(5); // Fixed width label
 		seedField = new VisTextField(String.valueOf(MathUtils.random(100000)));
	 	seedField.setAlignment(Align.center);
 		seedTable.add(seedField).expandX().fillX().padRight(5); // Field takes remaining space
 		VisTextButton randomSeedBtn = new VisTextButton("R"); // Smaller text "R" for Roll/Random to save space or icon
 		randomSeedBtn.addListener(new ClickListener() {
 			@Override
 			public void clicked(InputEvent event, float x, float y) {
 				seedField.setText(String.valueOf(MathUtils.random(1000000)));
 			}
 		});
 		seedTable.add(randomSeedBtn).width(30); // Fixed width button

 		stage.addActor(seedTable);

 		startY -= gap;

		// Buttons
		createMenuButton("New Game", targetX, startY, 0.1f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				startGame();
			}
		});

		startY -= gap;

		if (SaveManager.hasSave()) {
			createMenuButton("Continue", targetX, startY, 0.2f, new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					continueGame();
				}
			});
			startY -= gap;
		}

		createMenuButton("Exit", targetX, startY, 0.3f, new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		// Seed table animation
		seedTable.addAction(createEntranceAction(0f));
	}

	private void createMenuButton(String text, float targetX, float targetY, float delay, ClickListener listener) {
		VisTextButton btn = new VisTextButton(text);
		btn.addListener(listener);
		btn.setSize(220, 60);
		btn.setPosition(targetX, targetY);

		// Add to stage
		stage.addActor(btn);

		// Animation
		btn.addAction(createEntranceAction(delay));
	}

	private com.badlogic.gdx.scenes.scene2d.Action createEntranceAction(float delay) {
		// Initial state: Offscreen left and transparent
		return Actions.sequence(
			Actions.alpha(0),
			Actions.moveBy(-300, 0), // Move to start pos (relative to final)
			Actions.delay(delay),
			Actions.parallel(
				Actions.fadeIn(0.5f),
				Actions.moveBy(300, 0, 0.8f, Interpolation.swingOut)
			)
		);
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
		// Animations are triggered by actions added in buildUI/createMenuButton
		// But if we return to this screen, we might need to reset them?
		// Since we rebuild UI in create(), and create() is called once...
		// Actually GScreen.init() calls create().
		// If we reuse the screen instance, we need to reset actions.
		// Current GdxLauncher creates new instance every time?
		// No, ScreenManager caches screens.
		// So we should probably rebuild UI or reset actions in show().

		stage.clear(); // Clear old actors
		buildUI();     // Rebuild to restart animations
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		float scl = 1f;
		// 1. Draw Background
		// Use UI Viewport size for renderer to match screen
		
		bloomRender.captureStart(batch);
		
		batch.setProjectionMatrix(getUICamera().combined);
		renderer.render(batch, delta, getUIViewport().getWorldWidth()*scl, getUIViewport().getWorldHeight()*scl);
		
		bloomRender.captureEnd();
		bloomRender.process();
		bloomRender.render(batch);
		
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
		bloomRender.resize((int)getViewSize().x, (int)getViewSize().y);
	}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (stage != null) stage.dispose();
		super.dispose();
	}
}
