package com.goldsprite.magicdungeon2.screens.basics;

import java.util.Map;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon2.screens.tests.AIDrawReplayScreen;
import com.goldsprite.magicdungeon2.screens.tests.TexturePreviewScreen;

public class TestSelectionScreen extends BaseSelectionScreen {
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("AI绘制回放编辑器", AIDrawReplayScreen.class);
		map.put("纹理预览(JSON绘制)", TexturePreviewScreen.class);
		map.put("功能验证", null);

		map.put("临时观测测试(用完即删)", null);
	}

	@Override
	public void show() {
		super.show();
	}
}
