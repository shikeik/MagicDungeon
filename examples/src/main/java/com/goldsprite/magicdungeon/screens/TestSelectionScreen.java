package com.goldsprite.magicdungeon.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon.screens.tests.*;
import com.goldsprite.magicdungeon.screens.tests.neonskel.NeonLayeredMixTestScreen;
import com.goldsprite.magicdungeon.screens.tests.neonskel.NeonIKTestScreen;

import java.util.Map;

public class TestSelectionScreen extends BaseSelectionScreen {
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("功能验证", null);

		map.put("多边形绘制器蒙皮渲染测试", PolyBatchTestScreen.class);
		map.put("控件测试: 自动焦点式滑动布局", ScrollLayoutTestScreen.class);
		map.put("", null);

		map.put("通用 UI & 皮肤切换演示", VisUIDemoScreen.class);
		map.put("双网格演示", DualGridDemoScreen.class);
		map.put("新材质预览", MaterialPreviewScreen.class);
		map.put("纹理预览", TexturePreviewScreen.class);
		map.put("Neon生成测试", NeonGenTestScreen.class);
		map.put("狼Spine动画测试", SpineTestScreen.class);
		map.put("Neon分轨混合测试", NeonLayeredMixTestScreen.class);
		map.put("Neon IK 测试", NeonIKTestScreen.class);

		map.put("临时观测测试(用完即删)", null);
	}

	@Override
	public void show() {
		super.show();
	}
}
