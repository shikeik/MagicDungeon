package com.goldsprite.neonskel.adapter.ecs;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.neonskel.data.NeonSlot;
import com.goldsprite.neonskel.logic.AnimationState;
import com.goldsprite.neonskel.logic.NeonSkeleton;
import com.goldsprite.neonskel.render.NeonRenderBatch;
import com.goldsprite.neonskel.utils.NeonAssetProvider;

/**
 * ECS 适配层：连接核心库与游戏引擎
 * 这只是一个示例组件，实际使用时应根据项目的 ECS 框架进行调整。
 */
public class SkeletonComponent extends Component implements NeonAssetProvider {

	public NeonSkeleton skeleton;
	public AnimationState state;

	// Transform mock (in real ECS this would be injected or retrieved from TransformComponent)
	public float x, y, rotation = 0, scaleX = 1f, scaleY = 1f;

	public SkeletonComponent() {
		skeleton = new NeonSkeleton();
		state = new AnimationState();
	}

	@Override
	public void update(float delta) {
		// 1. Sync Transform
		skeleton.setPosition(x, y);
		skeleton.rotation = rotation;
		skeleton.scaleX = scaleX;
		skeleton.scaleY = scaleY;

		// 2. Update State
		state.update(delta);

		// 3. Apply Animation
		state.apply(skeleton, this);

		// 4. Update World Transform
		skeleton.updateWorldTransform();
	}

	public void render(NeonRenderBatch batch) {
		for (NeonSlot slot : skeleton.getDrawOrder()) {
			slot.draw(batch);
		}
	}

	@Override
	public TextureRegion findRegion(String name) {
		// TODO: 连接到实际的资源管理器 (如 TextureManager 或 AssetManager)
		return null;
	}
}
