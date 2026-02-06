package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.utils.Constants;
import com.badlogic.gdx.math.MathUtils;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class MainMenuScreen extends GScreen {
	private Stage stage;
	private VisTextField seedField;

	@Override
	protected void initViewport() {
		this.viewSizeShort = Constants.VIEWPORT_HEIGHT;
		this.viewSizeLong = Constants.VIEWPORT_WIDTH;
		this.uiViewportScale = 0.6f;

		super.initViewport();
	}

	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		VisTable table = new VisTable();
		table.setFillParent(true);
		stage.addActor(table);

		VisLabel titleLabel = new VisLabel("NEW DUNGEON");
		table.add(titleLabel).padBottom(50).colspan(2).row();

		// Seed Input Area
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
		
		table.add(seedTable).padBottom(20).colspan(2).row();

		VisTextButton startButton = new VisTextButton("New Game");
		startButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				long seed = 0;
				try {
					seed = Long.parseLong(seedField.getText());
				} catch (NumberFormatException e) {
					seed = seedField.getText().hashCode();
				}
				
				GameScreen gameScreen = new GameScreen(seed);
				getScreenManager().setCurScreen(gameScreen);
				dispose();
			}
		});
		table.add(startButton).width(200).height(50).padBottom(20).colspan(2).row();

		if (SaveManager.hasSave()) {
			VisTextButton loadButton = new VisTextButton("Continue");
			loadButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					GameScreen gameScreen = new GameScreen();
					getScreenManager().setCurScreen(gameScreen);
					gameScreen.loadGame();
					dispose();
				}
			});
			table.add(loadButton).width(200).height(50).padBottom(20).colspan(2).row();
		}

		VisTextButton exitButton = new VisTextButton("Exit");
		exitButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});
		table.add(exitButton).width(200).height(50).colspan(2).row();
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		stage.act(delta);
		stage.draw();
	}
}
