package com.goldsprite.magicdungeon.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;

import java.util.Map;

import com.goldsprite.magicdungeon.screens.tests.TexturePreviewScreen;

public class TestSelectionScreen extends BaseSelectionScreen {
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("功能验证", null);
		map.put("纹理预览", TexturePreviewScreen.class);

		map.put("临时观测测试(用完即删)", null);
	}
}
