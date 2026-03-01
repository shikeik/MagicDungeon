package com.goldsprite.magicdungeon2.screens.basics;

import java.util.Map;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon2.screens.tests.AIDrawReplayScreen;
import com.goldsprite.magicdungeon2.screens.tests.LanPlaygroundScreen;
import com.goldsprite.magicdungeon2.screens.tests.TexturePreviewScreen;
import com.goldsprite.magicdungeon2.screens.tests.netcode.NetcodeTankSandboxScreen;
import com.goldsprite.magicdungeon2.screens.tests.netcode.NetcodeUdpClientScreen;
import com.goldsprite.magicdungeon2.screens.tests.netcode.NetcodeUdpServerScreen;

public class TestSelectionScreen extends BaseSelectionScreen {
        @Override
        protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
                map.put("AI绘制回放编辑器", AIDrawReplayScreen.class);
                map.put("纹理预览(JSON绘制)", TexturePreviewScreen.class);
                map.put("局域网联机测试", LanPlaygroundScreen.class);
                
                map.put("Netcode坦克沙盒(纯内存)", NetcodeTankSandboxScreen.class);
                map.put("Netcode UDP服务端", NetcodeUdpServerScreen.class);
                map.put("Netcode UDP客户端", NetcodeUdpClientScreen.class);
		map.put("临时观测测试(用完即删)", null);
	}

	@Override
	public void show() {
		super.show();
	}
}
