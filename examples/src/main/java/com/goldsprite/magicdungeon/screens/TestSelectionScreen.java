package com.goldsprite.magicdungeon.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon.screens.tests.DualGridDemoScreen;
import com.goldsprite.magicdungeon.screens.tests.MaterialPreviewScreen;
import com.goldsprite.magicdungeon.screens.tests.NeonGenTestScreen;
import com.goldsprite.magicdungeon.screens.tests.TexturePreviewScreen;
import java.util.Map;
import com.goldsprite.magicdungeon.screens.tests.StarAssault;

public class TestSelectionScreen extends BaseSelectionScreen {int k36;
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("功能验证", null);
		map.put("bob游戏演示", StarAssault.class);
		map.put("双网格演示", DualGridDemoScreen.class);
		map.put("新材质预览", MaterialPreviewScreen.class);
		map.put("纹理预览", TexturePreviewScreen.class);
		map.put("Neon生成测试", NeonGenTestScreen.class);

		map.put("临时观测测试(用完即删)", null);
	}
}
