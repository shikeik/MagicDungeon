package com.goldsprite.neonskel.logic;

import com.goldsprite.neonskel.data.NeonAnimation;

/**
 * 轨道条目 (TrackEntry)
 * 用于记录单个轨道上的动画播放状态，支持混合 (Mixing) 和多轨道叠加。
 */
public class TrackEntry {
    // 轨道索引
    public int trackIndex;
    
    // 当前动画
    public NeonAnimation animation;
    
    // 播放状态
    public boolean loop;
    public float trackTime;
    public float timeScale = 1.0f;
    public float alpha = 1.0f; // 轨道的混合权重 (用于层级叠加)
    
    // 混合相关 (Crossfade)
    public TrackEntry mixingFrom; // 从哪个动画过渡来
    public float mixTime;         // 当前混合进度
    public float mixDuration;     // 总混合时间
    public float mixAlpha;        // 当前混合比例 (0~1)
    
    public TrackEntry(int trackIndex, NeonAnimation animation) {
        this.trackIndex = trackIndex;
        this.animation = animation;
        this.trackTime = 0f;
    }
    
    public void reset() {
        trackTime = 0f;
        loop = false;
        timeScale = 1.0f;
        alpha = 1.0f;
        mixingFrom = null;
        mixTime = 0f;
        mixDuration = 0f;
        mixAlpha = 1.0f;
    }
    
    /**
     * 获取用于计算的时间 (考虑循环)
     */
    public float getAnimationTime() {
        if (loop) {
            float duration = animation.duration;
            if (duration == 0) return 0;
            return trackTime % duration;
        } else {
            return Math.min(trackTime, animation.duration);
        }
    }
    
    public boolean isComplete() {
        return !loop && trackTime >= animation.duration;
    }
}
