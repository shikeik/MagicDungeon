package com.goldsprite.magicdungeon.screens.tests.neondrawer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;

/**
 * NeonBatch 升级版测试：渐变、光影与抗锯齿
 */
public class NeonDrawerTestScreen extends GScreen {

	private NeonBatch batch;
	private Texture noiseTexture; // 用于给矢量图增加质感的噪点纹理

	@Override
	public void create() {
		batch = new NeonBatch();

		// 生成一个简单的噪点纹理，用于叠加质感，消除“塑料感”
		Pixmap p = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
		for(int x=0; x<128; x++){
			for(int y=0; y<128; y++){
				if(MathUtils.randomBoolean(0.1f)) {
					p.setColor(1, 1, 1, 0.15f); // 淡淡的噪点
					p.drawPixel(x, y);
				}
			}
		}
		noiseTexture = new Texture(p);
		p.dispose();
	}

	@Override
	public void render(float delta) {
		// 深色背景，突出发光效果
		Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();

		// -------------------------------------------------
		// 左侧：旧效果 (扁平、无光影)
		// -------------------------------------------------
		drawFlatDragon(150, 150);
		drawFlatPotion(150, 400);

		// -------------------------------------------------
		// 右侧：新效果 (渐变、羽化、体积感)
		// -------------------------------------------------
		drawGradientDragon(600, 150);
		drawGradientPotion(600, 400);

		batch.end();
	}

	// --- 旧版绘制逻辑 (模拟你原本的实现) ---
	private void drawFlatDragon(float x, float y) {
		// 身体
		batch.drawRect(x, y, 100, 120, 0, 0, Color.RED, true);
		// 腹部
		batch.drawRect(x + 20, y + 20, 60, 80, 0, 0, Color.ORANGE, true);
		// 翅膀 (纯色三角形)
		batch.drawTriangle(x, y + 100, x - 50, y + 150, x, y + 60, 0, Color.MAROON, true);
		batch.drawTriangle(x + 100, y + 100, x + 150, y + 150, x + 100, y + 60, 0, Color.MAROON, true);
		// 眼睛
		batch.drawRect(x + 20, y + 90, 20, 10, 0, 0, Color.LIME, true);
		batch.drawRect(x + 60, y + 90, 20, 10, 0, 0, Color.LIME, true);
	}

	private void drawFlatPotion(float x, float y) {
		// 瓶身 (纯色圆)
		batch.drawCircle(x, y, 50, 0, Color.PURPLE, 32, true);
		// 瓶颈
		batch.drawRect(x - 15, y + 40, 30, 40, 0, 0, Color.PURPLE, true);
		// 塞子
		batch.drawRect(x - 20, y + 80, 40, 10, 0, 0, Color.GRAY, true);
	}

	// --- 新版绘制逻辑 (使用升级后的 API) ---
	private void drawGradientDragon(float x, float y) {
		// 1. 翅膀：使用【羽化描边】和【渐变填充】
		// 这里的技巧是：内侧深红，外侧全透明，制造出薄膜感
		Color wingInner = new Color(0.8f, 0.1f, 0.1f, 1f);
		Color wingOuter = new Color(0.5f, 0.0f, 0.0f, 0.0f); // 透明边缘

		// 左翼 (使用新的 drawGradientTriangle)
		batch.drawGradientTriangle(x, y + 100, wingInner, x - 60, y + 160, wingOuter, x, y + 60, wingInner);
		// 右翼
		batch.drawGradientTriangle(x + 100, y + 100, wingInner, x + 160, y + 160, wingOuter, x + 100, y + 60, wingInner);

		// 2. 身体：使用【垂直线性渐变】(模拟顶部光照)
		Color bodyTop = new Color(1.0f, 0.3f, 0.3f, 1f); // 亮红
		Color bodyBot = new Color(0.6f, 0.0f, 0.0f, 1f); // 暗红
		batch.drawVerticalGradientRect(x, y, 100, 120, bodyBot, bodyTop);

		// 3. 腹部：使用【水平光泽】
		Color bellyC = Color.GOLD;
		Color bellyEdge = Color.ORANGE;
		// 模拟中间亮两边暗的柱状体积感
		batch.drawHorizontalGradientRect(x + 20, y + 20, 60, 80, bellyEdge, bellyC, bellyEdge);

		// 4. 眼睛：使用【发光描边】(Glow)
		// 先画深绿底色
		batch.drawRect(x + 20, y + 90, 20, 10, 0, 0, new Color(0, 0.5f, 0, 1), true);
		batch.drawRect(x + 60, y + 90, 20, 10, 0, 0, new Color(0, 0.5f, 0, 1), true);
		// 再画高亮中心
		batch.drawRect(x + 25, y + 92, 10, 6, 0, 0, Color.LIME, true);

		// 5. 叠加噪点纹理 (增加皮肤质感)
		// 这一步对于消除“Flash 矢量感”至关重要
		batch.setColor(Color.GRAY); // 混合颜色
		batch.draw(noiseTexture, x, y, 100, 120);
		batch.setColor(Color.WHITE); // 恢复
	}

	private void drawGradientPotion(float x, float y) {
		// 1. 瓶身液体：使用【径向渐变】(Radial Gradient)
		// 中心亮紫，边缘深紫 -> 制造球体体积
		Color liquidCenter = new Color(0.8f, 0.2f, 1.0f, 1f);
		Color liquidEdge = new Color(0.3f, 0.0f, 0.5f, 1f);
		batch.drawRadialGradientCircle(x, y, 50, liquidCenter, liquidEdge, 64);

		// 2. 发光边缘 (Rim Light)：使用【羽化描边】
		// 内侧颜色透明，外侧颜色带一点亮紫
		Color glowInner = new Color(1, 0, 1, 0);
		Color glowOuter = new Color(0.8f, 0.6f, 1f, 0.6f);
		// drawCircle 支持传入 innerColor 和 outerColor 做羽化
		batch.drawCircleGradientStroke(x, y, 50, 4f, glowInner, glowOuter, 64);

		// 3. 瓶颈：线性渐变 + 玻璃质感
		batch.drawHorizontalGradientRect(x - 15, y + 40, 30, 40,
			new Color(0.4f, 0, 0.6f, 0.8f), new Color(0.6f, 0.2f, 0.8f, 0.5f), new Color(0.4f, 0, 0.6f, 0.8f));

		// 4. 高光 (Fake Highlight)：画一个半透明白色椭圆
		batch.drawOval(x - 20, y + 15, 20, 15, -45, 0, new Color(1,1,1,0.4f), 16, true);

		// 5. 气泡
		batch.drawCircle(x + 10, y - 20, 5, 0, new Color(1,1,1,0.3f), 32, true);
		batch.drawCircle(x - 5, y + 10, 3, 0, new Color(1,1,1,0.3f), 32, true);
	}

	@Override
	public void dispose() {
		batch.dispose();
		noiseTexture.dispose();
	}


}
