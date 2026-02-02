package ${PACKAGE_NAME};

import com.goldsprite.magicdungeon.core.scripting.IGameScriptEntry;
import com.goldsprite.magicdungeon.ecs.GameWorld;
import com.goldsprite.magicdungeon.log.Debug;

public class ${CLASS_NAME} implements IGameScriptEntry {
	@Override
	public void onStart(GameWorld world) {
		Debug.logT("Script", "${CLASS_NAME} started!");
	}
}
