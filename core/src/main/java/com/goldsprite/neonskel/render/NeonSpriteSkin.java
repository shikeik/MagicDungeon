package com.goldsprite.neonskel.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;

public class NeonSpriteSkin implements BoneSkin {
	public TextureRegion region;

	public NeonSpriteSkin(TextureRegion region) {
		this.region = region;
	}

	@Override
	public void draw(NeonRenderBatch batch, Affine2 t, float length, Color color) {
		if (region == null) return;

		// 计算绘制参数
		// 假设骨骼长度方向是 X 轴
		// 贴图通常以骨骼起点或中心为原点
		// 这里简单实现：以 (0,0) 为锚点 (左边中间?) 或者中心?
		// 通常 Spine 的 RegionAttachment 有 offset, rotation 等。
		// 这里简化：贴图中心对齐骨骼 (0,0) 点? 或者 (length/2, 0)?

		// 暂时实现：贴图原点在 (0,0), 并且旋转跟随骨骼
		// 实际上我们需要把 region 绘制到 t 指定的变换中

		float rotation = (float) Math.atan2(t.m10, t.m00) * 57.2957795f;
		float sx = (float) Math.sqrt(t.m00 * t.m00 + t.m10 * t.m10);
		float sy = (float) Math.sqrt(t.m01 * t.m01 + t.m11 * t.m11);

		float worldX = t.m02;
		float worldY = t.m12;

		// 假设 region 的宽对应 length? 不一定。
		// 暂时直接画，不做复杂缩放
		float w = region.getRegionWidth();
		float h = region.getRegionHeight();

		batch.draw(region, worldX, worldY, 0, 0, w, h, sx, sy, rotation);
	}
}
