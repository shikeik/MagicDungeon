package ${PACKAGE};

import com.goldsprite.magicdungeon.core.scripting.IGameScriptEntry;
import com.goldsprite.magicdungeon.ecs.GameWorld;
import com.goldsprite.magicdungeon.log.Debug;

public class ${MAIN_CLASS} implements IGameScriptEntry {
	@Override
	public void onStart(GameWorld world) {
		Debug.logT("UserProject", "Game Started: Hello " + "${PROJECT_NAME}!");
	}
}
