package com.goldsprite.neonskel.logic;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.neonskel.data.*;
import com.goldsprite.neonskel.render.NeonSpriteSkin;
import com.goldsprite.neonskel.utils.NeonAssetProvider;

public class AnimationState {

	private final ObjectMap<String, NeonAnimation> animationLibrary = new ObjectMap<>();
	
	// 多轨道支持
	private final Array<TrackEntry> tracks = new Array<>();

	private float speed = 1.0f;
	
	public void addAnimation(NeonAnimation anim) {
		animationLibrary.put(anim.name, anim);
	}

	/** 播放动画 (循环, 轨道0) */
	public TrackEntry play(String name) {
		return setAnimation(0, name, true);
	}
	
	/** 播放动画 (指定循环, 轨道0) */
	public TrackEntry play(String name, boolean loop) {
		return setAnimation(0, name, loop);
	}
	
	/** 播放一次 (不循环, 轨道0) */
	public TrackEntry playOnce(String name) {
		return setAnimation(0, name, false);
	}

	/** 淡入淡出 (循环, 轨道0) */
	public TrackEntry crossFade(String name, float duration) {
		return setAnimation(0, name, true, duration);
	}
	
	public TrackEntry setAnimation(int trackIndex, String name, boolean loop) {
		return setAnimation(trackIndex, name, loop, 0f);
	}

	public TrackEntry setAnimation(int trackIndex, String name, boolean loop, float mixDuration) {
		NeonAnimation anim = animationLibrary.get(name);
		if (anim == null) return null;
		
		// 扩展 tracks 数组
		while (tracks.size <= trackIndex) {
			tracks.add(null);
		}
		
		TrackEntry current = tracks.get(trackIndex);
		
		// 如果当前已经在播放同一个动画且正在播放中，且请求也是循环，则不重置
		if (current != null && current.animation == anim && current.loop == loop && !current.isComplete()) {
			return current;
		}
		
		TrackEntry entry = new TrackEntry(trackIndex, anim);
		entry.loop = loop;
		
		if (current != null && mixDuration > 0) {
			entry.mixingFrom = current;
			entry.mixDuration = mixDuration;
			entry.mixTime = 0f;
			entry.mixAlpha = 0f;
		}
		
		tracks.set(trackIndex, entry);
		return entry;
	}
	
	public void clearTrack(int trackIndex) {
		if (trackIndex >= 0 && trackIndex < tracks.size) {
			tracks.set(trackIndex, null);
		}
	}
	
	public TrackEntry getCurrent(int trackIndex) {
		if (trackIndex < 0 || trackIndex >= tracks.size) return null;
		return tracks.get(trackIndex);
	}

	public void update(float delta) {
		float dt = delta * speed;
		
		for (int i = 0; i < tracks.size; i++) {
			TrackEntry track = tracks.get(i);
			if (track == null) continue;
			
			track.trackTime += dt * track.timeScale;
			
			// 处理混合
			if (track.mixingFrom != null) {
				track.mixTime += dt;
				
				// 更新 mixingFrom 的时间
				track.mixingFrom.trackTime += dt * track.mixingFrom.timeScale;
				
				if (track.mixTime >= track.mixDuration) {
					track.mixAlpha = 1.0f;
					track.mixingFrom = null;
				} else {
					track.mixAlpha = track.mixTime / track.mixDuration;
				}
			}
			
			// 如果非循环动画结束，是否自动清除？通常由上层逻辑决定，或者保留最后一帧状态
		}
	}

	public void apply(NeonSkeleton skeleton, NeonAssetProvider assets) {
		for (int i = 0; i < tracks.size; i++) {
			TrackEntry track = tracks.get(i);
			if (track == null) continue;
			
			applyTrack(track, skeleton, assets);
		}
	}
	
	private void applyTrack(TrackEntry track, NeonSkeleton skeleton, NeonAssetProvider assets) {
		// 如果混合权重为 0 (完全不显示)，跳过
		if (track.alpha <= 0) return;
		
		// 遍历当前动画的所有时间轴
		for (NeonTimeline timeline : track.animation.timelines) {
			 applyTimeline(timeline, track, skeleton, assets);
		}
	}
	
	private void applyTimeline(NeonTimeline timeline, TrackEntry track, NeonSkeleton skeleton, NeonAssetProvider assets) {
		// 1. 计算当前动画的值
		float time = track.getAnimationTime();
		
		// 处理浮点属性 (骨骼变换)
		if (timeline.property.isFloat()) {
			NeonBone bone = skeleton.getBone(timeline.boneName);
			if (bone == null) return;
			
			float valCurrent = timeline.evaluate(time);
			float valTarget = valCurrent;
			
			// 2. 处理 Crossfade 混合
			if (track.mixingFrom != null) {
				NeonTimeline prevLine = findTimeline(track.mixingFrom.animation, timeline.boneName, timeline.property);
				if (prevLine != null) {
					float timePrev = track.mixingFrom.getAnimationTime();
					float valPrev = prevLine.evaluate(timePrev);
					valTarget = MathUtils.lerp(valPrev, valCurrent, track.mixAlpha);
				}
			}
			
			// 3. 应用到骨骼
			// 如果是轨道 0 (Base Track)，通常是覆盖模式
			// 如果是轨道 > 0 (Layer Track)，通常是混合模式 (track.alpha)
			
			if (track.trackIndex == 0) {
				// Base track: 直接设置 (如果有 crossfade 已经计算在 valTarget 中)
				// 但如果 track.alpha < 1 (虽然轨道0通常是1)，也应该混合
				if (track.alpha >= 1f) {
					setBoneProperty(bone, timeline.property, valTarget);
				} else {
					float currentVal = getBoneProperty(bone, timeline.property);
					setBoneProperty(bone, timeline.property, MathUtils.lerp(currentVal, valTarget, track.alpha));
				}
			} else {
				// Layer track: 混合当前值
				float currentVal = getBoneProperty(bone, timeline.property);
				setBoneProperty(bone, timeline.property, MathUtils.lerp(currentVal, valTarget, track.alpha));
			}
		} 
		// 处理对象属性 (Sprite)
		else if (timeline.property == NeonProperty.SPRITE) {
			// Sprite 切换通常不插值，直接在特定时间点切换
			// 对于混合，通常取 mixAlpha >= 0.5 时的值
			
			float applyTime = time;
			NeonTimeline targetTimeline = timeline;
			
			if (track.mixingFrom != null && track.mixAlpha < 0.5f) {
				// 使用旧动画
				targetTimeline = findTimeline(track.mixingFrom.animation, timeline.boneName, timeline.property);
				if (targetTimeline != null) {
					applyTime = track.mixingFrom.getAnimationTime();
				} else {
					// 旧动画没有，使用新的
					targetTimeline = timeline;
				}
			}
			
			if (targetTimeline != null) {
				applySpriteTimeline(targetTimeline, applyTime, skeleton, assets);
			}
		}
	}
	
	private void applySpriteTimeline(NeonTimeline timeline, float time, NeonSkeleton skeleton, NeonAssetProvider assets) {
		NeonSlot slot = skeleton.getSlot(timeline.boneName);
		if (slot != null && slot.skin instanceof NeonSpriteSkin) {
			Object val = timeline.evaluateObject(time);
			if (val instanceof String) {
				if (assets != null) {
					TextureRegion reg = assets.findRegion((String) val);
					((NeonSpriteSkin) slot.skin).region = reg;
				}
			} else if (val instanceof TextureRegion) {
				((NeonSpriteSkin) slot.skin).region = (TextureRegion) val;
			}
		}
	}

	private void setBoneProperty(NeonBone bone, NeonProperty prop, float val) {
		switch (prop) {
			case X: bone.x = val; break;
			case Y: bone.y = val; break;
			case ROTATION: bone.rotation = val; break;
			case SCALE_X: bone.scaleX = val; break;
			case SCALE_Y: bone.scaleY = val; break;
		}
	}
	
	private float getBoneProperty(NeonBone bone, NeonProperty prop) {
		switch (prop) {
			case X: return bone.x;
			case Y: return bone.y;
			case ROTATION: return bone.rotation;
			case SCALE_X: return bone.scaleX;
			case SCALE_Y: return bone.scaleY;
		}
		return 0f;
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
