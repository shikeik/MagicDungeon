package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;

public class MagicDungeonLoadingRenderer implements ScreenManager.LoadingRenderer {
	private PolygonSpriteBatch polyBatch;
	private SpriteBatch batch;
	private SkeletonRenderer skeletonRenderer;
	private Skeleton skeleton;
	private AnimationState animationState;
	
	private String loadingText = "正在前往目标区域...";
	private boolean initialized = false;

	@Override
	public void setText(String text) {
		if (text != null) {
			this.loadingText = text;
		}
	}

	private void init() {
		if (initialized) return;
		
		polyBatch = new PolygonSpriteBatch();
		batch = new SpriteBatch();
		skeletonRenderer = new SkeletonRenderer();
		skeletonRenderer.setPremultipliedAlpha(true);
		
		try {
			// Load Spine (Using large_sworder as in LoadingScreen)
			TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("spines/large_sworder/large_sworder.atlas"));
			SkeletonJson json = new SkeletonJson(atlas);
			json.setScale(1.0f);
			SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("spines/large_sworder/large_sworder.json"));
	
			skeleton = new Skeleton(skeletonData);
			
			AnimationStateData stateData = new AnimationStateData(skeletonData);
			animationState = new AnimationState(stateData);
			animationState.setAnimation(0, "move", true); // "move" or "run"? LoadingScreen used "move"
		} catch (Exception e) {
			Gdx.app.error("LoadingRenderer", "Failed to load spine", e);
		}
		
		initialized = true;
	}

	@Override
	public void render(float delta, float alpha) {
		if (!initialized) init();
		if (skeleton == null) return;

		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		// Update Spine
		skeleton.setPosition(w / 2, h / 2 - 50);
		animationState.update(delta);
		animationState.apply(skeleton);
		skeleton.updateWorldTransform();
		
		// Draw
		polyBatch.begin();
		skeletonRenderer.draw(polyBatch, skeleton);
		polyBatch.end();
		
		// Draw Text
		batch.begin();
		
		if (VisUI.isLoaded()) {
			BitmapFont font = VisUI.getSkin().getFont("default-font");
			Color oldColor = font.getColor().cpy();
			font.setColor(Color.WHITE);
			
			// Calculate center
			GlyphLayout layout = new GlyphLayout(font, loadingText);
			float textX = (w - layout.width) / 2;
			float textY = (h / 2) - 80;
			
			font.draw(batch, loadingText, textX, textY);
			font.setColor(oldColor);
		}
		
		batch.end();
	}
	
	public void dispose() {
		if (polyBatch != null) polyBatch.dispose();
		if (batch != null) batch.dispose();
	}
}
