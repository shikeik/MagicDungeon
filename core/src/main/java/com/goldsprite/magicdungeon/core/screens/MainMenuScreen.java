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
		seedTable.add(new VisLabel("Seed: ")).padRight(5);
		seedField = new VisTextField(String.valueOf(MathUtils.random(100000)));
		seedTable.add(seedField).width(120).padRight(5);

		VisTextButton randomSeedBtn = new VisTextButton("Dice");
		randomSeedBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				seedField.setText(String.valueOf(MathUtils.random(1000000)));
			}
		});
		seedTable.add(randomSeedBtn).width(50);

		// Add rows to table
		// Note: We want staggered animation, so we might need to add them as individual actors to stage?
		// Or we can just animate the cells?
		// Easier approach: Add to table, but set table to be transparent initially?
		// No, user wants "one by one".
		// Best approach: Add them to table, but then get the cells and apply transform?
		// Actually, let's keep the table for layout, but animate the Table itself for entrance?
		// User said: "not all at once but one by one".
		// So we should NOT use a single table for everything if we want them to fly in separately easily.
		// OR, we use a Table but set `Transform` to true and animate actors.

		// Let's use a Table for layout, but make the table invisible/transparent and animate children?
		// No, children positions are relative to table.

		// Alternative: Separate Actors.
		// Let's stick to Table for Seed (it's a group), but Buttons can be separate.

		mainTable.add(seedTable).padBottom(30).row();

		// We will add buttons to the table, but we will animate the TABLE itself as a group?
		// No, user wants "one by one".
		// So we need to apply actions to the actors inside the table?
		// Layout might fight with actions if we move them.
		// Unless we use `Transform` and `Layout` properly.

		// Let's Try:
		// Add everything to mainTable.
		// In show(), iterate over children and add MoveAction with delay.
		// But children are constrained by Table Layout.
		// If we move them, Table layout might reset them next frame.
		// Solution: Use `Transform` on actors and animate `translation`.
		// OR: Don't use Table for main layout, use absolute positioning or a VerticalGroup that doesn't force position every frame?
		// Let's use `VerticalGroup` or just absolute positioning relative to a container.

		// Let's revert to adding to mainTable, but use a trick:
		// Animate the alpha or scale? User said "move in".
		// Okay, let's just make mainTable act as a container anchor,
		// and we animate the buttons "flying in" to their slots?
		// That's hard with Table.

		// Simplest "Staggered Entrance":
		// 1. Table is placed at final position.
		// 2. All children are set to invisible or offset.
		// 3. We apply actions to children to move them from offset to 0.
		// (Table layout sets X/Y. We can use `setTransform(true)` on buttons and animate `visual` position? No, Table controls position.)

		// Better approach:
		// Don't use Table. Use a customized Group or just Stage coordinates.
		// Given it's a simple menu:
		// Title (already added)
		// Seed Group (Actor 1)
		// Button 1 (Actor 2)
		// Button 2 (Actor 3)
		// Button 3 (Actor 4)

		// Let's redo buildUI to not use a master Table for layout, but place actors manually or use a helper.

		float startY = centerY + 150;
		float gap = 70;

		// Seed Group
		seedTable.setPosition(targetX+150, startY+50);
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

		float scl = 1.5f;
		// 1. Draw Background
		// Use UI Viewport size for renderer to match screen
		renderer.render(batch, delta, getUIViewport().getWorldWidth()*scl, getUIViewport().getWorldHeight()*scl);

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
