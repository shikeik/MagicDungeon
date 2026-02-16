package com.goldsprite.neonskel.logic;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.neonskel.data.*;
import com.goldsprite.neonskel.render.NeonSpriteSkin;
import com.goldsprite.neonskel.utils.NeonAssetProvider;

public class AnimationState {

	private final ObjectMap<String, NeonAnimation> animationLibrary = new ObjectMap<>();

	private NeonAnimation currentAnim;
	private float currentTime = 0f;

	private NeonAnimation prevAnim;
	private float prevTime = 0f;

	private float mixTimer = 0f;
	private float mixDuration = 0f;
	private boolean inTransition = false;

	private float speed = 1.0f;
	private boolean isPlaying = false;

	public void addAnimation(NeonAnimation anim) {
		animationLibrary.put(anim.name, anim);
	}

	public void play(String name) {
		startAnimation(name, 0f);
	}

	public void crossFade(String name, float duration) {
		startAnimation(name, duration);
	}

	private void startAnimation(String name, float transitionTime) {
		NeonAnimation nextAnim = animationLibrary.get(name);
		if (nextAnim == null) return;
		if (currentAnim == nextAnim && isPlaying && currentAnim.looping) return;

		if (transitionTime <= 0 || currentAnim == null) {
			this.currentAnim = nextAnim;
			this.currentTime = 0f;
			this.inTransition = false;
			this.prevAnim = null;
		} else {
			this.prevAnim = this.currentAnim;
			this.prevTime = this.currentTime;
			this.currentAnim = nextAnim;
			this.currentTime = 0f;
			this.mixDuration = transitionTime;
			this.mixTimer = 0f;
			this.inTransition = true;
		}
		this.isPlaying = true;
	}

	public void update(float delta) {
		if (!isPlaying || currentAnim == null) return;

		float dt = delta * speed;
		currentTime = updateTime(currentAnim, currentTime, dt);

		if (inTransition) {
			mixTimer += dt;
			if (prevAnim != null) prevTime = updateTime(prevAnim, prevTime, dt);

			if (mixTimer >= mixDuration) {
				inTransition = false;
				prevAnim = null;
			}
		}
	}

	private float updateTime(NeonAnimation anim, float time, float dt) {
		time += dt;
		if (time >= anim.duration) {
			if (anim.looping) time %= anim.duration;
			else time = anim.duration;
		}
		return time;
	}

	public void apply(NeonSkeleton skeleton, NeonAssetProvider assets) {
		if (currentAnim == null) return;

		float alpha = 1.0f;
		if (inTransition && prevAnim != null) {
			alpha = mixTimer / mixDuration;
		}

		// 遍历当前动画的所有时间轴
		for (NeonTimeline timeline : currentAnim.timelines) {

			// --- 分支 1: 处理浮点数属性 (骨骼) ---
			if (timeline.property.isFloat()) {
				NeonBone bone = skeleton.getBone(timeline.boneName);
				if (bone == null) continue;

				float valCurr = timeline.evaluate(currentTime);
				float finalVal = valCurr;

				// 混合逻辑 (仅针对 float)
				if (inTransition && prevAnim != null) {
					NeonTimeline prevLine = findTimeline(prevAnim, timeline.boneName, timeline.property);
					if (prevLine != null) {
						float valPrev = prevLine.evaluate(prevTime);
						finalVal = MathUtils.lerp(valPrev, valCurr, alpha);
					}
				}

				switch (timeline.property) {
					case X: bone.x = finalVal; break;
					case Y: bone.y = finalVal; break;
					case ROTATION: bone.rotation = finalVal; break;
					case SCALE_X: bone.scaleX = finalVal; break;
					case SCALE_Y: bone.scaleY = finalVal; break;
				}
			}
			// --- 分支 2: 处理对象属性 (Sprite) ---
			else if (timeline.property == NeonProperty.SPRITE) {
				// 获取 Slot 并设置 Skin
				NeonSlot slot = skeleton.getSlot(timeline.boneName);
				if (slot != null && slot.skin instanceof NeonSpriteSkin) {
					Object val = timeline.evaluateObject(currentTime);
					if (val instanceof String) {
						// 通过 AssetProvider 查找纹理
						if (assets != null) {
							TextureRegion reg = assets.findRegion((String) val);
							((NeonSpriteSkin) slot.skin).region = reg;
						}
					} else if (val instanceof TextureRegion) {
						((NeonSpriteSkin) slot.skin).region = (TextureRegion) val;
					}
				}
			}
		}
	}

	private NeonTimeline findTimeline(NeonAnimation anim, String bone, NeonProperty prop) {
		for(NeonTimeline t : anim.timelines) {
			if(t.boneName.equals(bone) && t.property == prop) return t;
		}
		return null;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}
}
