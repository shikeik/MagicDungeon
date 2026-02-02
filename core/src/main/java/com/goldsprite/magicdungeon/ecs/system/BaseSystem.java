package com.goldsprite.magicdungeon.ecs.system;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.magicdungeon.ecs.ComponentManager;
import com.goldsprite.magicdungeon.ecs.EcsObject;
import com.goldsprite.magicdungeon.ecs.GameSystemInfo;
import com.goldsprite.magicdungeon.ecs.GameWorld;
import com.goldsprite.magicdungeon.ecs.SystemType;
import com.goldsprite.magicdungeon.ecs.component.Component;
import com.goldsprite.magicdungeon.ecs.entity.GObject;
import com.goldsprite.magicdungeon.log.Debug;
import com.goldsprite.magicdungeon.neonbatch.NeonBatch;
import java.util.List;

/**
 * 系统基类
 */
public abstract class BaseSystem extends EcsObject {

	protected GameWorld world;
	private boolean isEnabled = true;
	private int systemTypeFlags; // 缓存类型掩码

	// 系统关注的组件类型
	private Class<? extends Component>[] interestComponents;

	public BaseSystem() {
		super(); // 分配 ID
		this.world = GameWorld.inst();

		// 解析注解
		GameSystemInfo info = this.getClass().getAnnotation(GameSystemInfo.class);
		if (info != null) {
			this.interestComponents = info.interestComponents();
			this.systemTypeFlags = info.type();
			// [修改] 使用 SystemType.toString 输出可读类型
			Debug.logT("System", "Init %s: type=[%s]",
						getClass().getSimpleName(),
						SystemType.toString(systemTypeFlags));
		} else {
			this.systemTypeFlags = SystemType.UPDATE;
			Debug.logT("System", "Init %s: No Annotation, default to [UPDATE]", getClass().getSimpleName());
		}

		// 自动注册到世界 (构造即生效)
		GameWorld.inst().registerSystem(this);
	}

	/**
	 * 获取当前系统关心的实体列表 (O(1) 高效查询)
	 * 利用 ComponentManager 的缓存
	 */
	protected List<GObject> getInterestEntities() {
		if (interestComponents != null && interestComponents.length > 0) {
			return ComponentManager.getEntitiesWithComponents(interestComponents);
		}
		// 如果没定义感兴趣的组件，默认返回所有(顶层)实体
		// 注意：这可能不是你想要的，通常建议 System 明确声明 interest
		return world.getRootEntities();
	}

	public void awake() {}

	// --- 逻辑管线 ---
	@Override public void fixedUpdate(float fixedDelta){}
	@Override public void update(float delta){}
	// --- 渲染管线 ---
	public void render(NeonBatch batch, Camera camera) {}

	// --- 状态查询 API ---
	public boolean isUpdateSystem() { return SystemType.isUpdate(systemTypeFlags); }
	public boolean isFixedSystem() { return SystemType.isFixed(systemTypeFlags); }
	public boolean isRenderSystem() { return SystemType.isRender(systemTypeFlags); }
	public boolean isLogicSystem() { return SystemType.isLogic(systemTypeFlags); }
	public int getSystemType() { return systemTypeFlags; }

	public boolean isEnabled() { return isEnabled; }
	public void setEnabled(boolean enabled) { isEnabled = enabled; }

	public String getSystemName() {
		return this.getClass().getSimpleName();
	}

	public GameSystemInfo getSystemInfo() {
		return getClass().getAnnotation(GameSystemInfo.class);
	}
}
