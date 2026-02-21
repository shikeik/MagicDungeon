package com.goldsprite.magicdungeon2.testing;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon2.core.stats.StatData;

public class PlayerStatsTest implements IGameAutoTest {
	
	@Override
	public void run() {
		DLog.logT("PlayerStatsTest", "启动玩家属性测试");
		
		AutoTestManager.ENABLED = true;
		AutoTestManager atm = AutoTestManager.getInstance();
		
		StatData stats = new StatData();
	}
	
}
