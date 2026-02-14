package com.goldsprite.gdengine.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;

import java.util.*;
import java.util.function.Consumer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.function.Supplier;

/**
 * 使用：
 * * 创建:
 * * * 实例化: ScreenManager.getInstance().addScreen(new YourScreen()).setLaunchScreen(YourScreen.class);
 * * * 渲染: ScreenManager.getInstance().render();
 * * * 重大小: ScreenManager.getInstance().resize(width, height);
 * * * 回收: ScreenManager.getInstance().dispose();
 * * 切换屏幕:
 * * * 首先需要已添加目标屏幕，然后在屏幕内部使用getScreenManager().setCurScreen(TargetScreen.class)
 * * 返回上次屏幕:
 * * * 在屏幕历史堆栈有屏幕时getScreenManager().popLastScreen()来返回上个屏幕
 * * 转场效果:
 * * * 使用 playTransition(Runnable onMiddle) 来执行带黑屏过渡的操作
 */
public class ScreenManager implements Disposable {

	// 1. 定义屏幕方向枚举
	public enum Orientation {
		Portrait, // 竖屏
		Landscape // 横屏
		}
	
	// 转场状态
	private enum TransitionState {
		NONE,
		FADE_OUT, // 透明 -> 黑
		FADE_IN   // 黑 -> 透明
	}

	// 2. 定义回调接口 (底层不依赖 Android/Lwjgl)
	public static Consumer<Orientation> orientationChanger;
	public static List<Runnable> exitGame = new ArrayList<>();//声明退出游戏事件回调，需要在各平台自身实现
    
    // [新增] 输入系统钩子，解耦具体输入实现
    public static Runnable inputUpdater;
    public static Supplier<Boolean> backKeyTrigger = () -> 
        Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);

	private static ScreenManager instance;
	private final Map<Class<?>, GScreen> screens = new HashMap<>();
	private InputMultiplexer imp;
	//屏幕历史堆栈
	private final Stack<GScreen> screenHistory = new Stack<>();
	private GScreen curScreen;
	private GScreen launchScreen;
	private boolean popping;//用于标记是否为弹出历史屏幕状态
	private Viewport viewport;//统一视口

	// 转场相关变量
	private TransitionState transitionState = TransitionState.NONE;
	private float transitionDuration = 0.5f;
	private float transitionTime = 0f;
	private Runnable onTransitionMiddle;
	private Runnable onTransitionEnd;
	private ShapeRenderer shapeRenderer;

	public ScreenManager() {
		this(new InputMultiplexer());
	}

	public ScreenManager(InputMultiplexer imp) {
		this(new ScreenViewport(), imp);
	}
	public ScreenManager(Viewport viewport) {
		this(viewport, new InputMultiplexer());
	}

	public ScreenManager(Viewport viewport, InputMultiplexer imp) {
		instance = this;
		this.viewport = viewport;
		//关键 这里需要手动update才能触发worldWidth/Height初始化赋值，之后GScreen才能拿到视口数据
		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		initInputHandler(imp);
		
		// 初始化转场渲染器
		shapeRenderer = new ShapeRenderer();
	}


	/**
	 * 单例屏幕管理器
	 */
	public static synchronized ScreenManager getInstance() {
		return instance;
	}

	// 3. 增加切换方法
	public void setOrientation(Orientation orientation) {
		if (orientationChanger != null) {
			orientationChanger.accept(orientation);
			//curScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		// 注意：实际视口 resize 会在 resize() 回调中被触发，这里不需要手动改 Viewport
	}

	private void initInputHandler(InputMultiplexer imp) {
		this.imp = imp;

		//设置到gdx输入管线
		if (Gdx.input.getInputProcessor() == null) Gdx.input.setInputProcessor(imp);
		
		// 移除旧的 InputAdapter，改为在 render 中轮询以支持所有输入设备 (包括手柄)
		InputAdapter defaultHandler = new InputAdapter() {
			boolean isFullscreen;

			public boolean keyDown(int keyCode) {
				if(keyCode == Input.Keys.F11) {
					isFullscreen = !isFullscreen;
					PlatformImpl.fullScreenEvent.accept(isFullscreen);
				}
				return false;
			}
		};
		imp.addProcessor(defaultHandler);
	}

	public InputMultiplexer getImp() {
		return imp;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public ScreenManager setViewport(Viewport viewport) {
		this.viewport = viewport;
		return this;
	}

	/**
	 * 开始一个转场效果 (淡入淡出)
	 * @param onMiddle 当屏幕完全变黑时执行的操作 (通常用于切换屏幕或重置关卡)
	 */
	public void playTransition(Runnable onMiddle) {
		playTransition(onMiddle, null);
	}

	/**
	 * 开始一个转场效果 (淡入淡出)
	 * @param onMiddle 当屏幕完全变黑时执行的操作
	 * @param onEnd 当转场完全结束时执行的操作
	 */
	public void playTransition(Runnable onMiddle, Runnable onEnd) {
		if (transitionState != TransitionState.NONE) return; // 已经在转场中

		this.transitionState = TransitionState.FADE_OUT;
		this.transitionTime = 0f;
		this.onTransitionMiddle = onMiddle;
		this.onTransitionEnd = onEnd;
	}
	
	public boolean isTransitioning() {
		return transitionState != TransitionState.NONE;
	}
	
	/**
	 * 渲染(已初始化的)当前屏幕
	 */
	public void render() {
        // 全局输入更新
        if (inputUpdater != null) inputUpdater.run();
        
        // 全局返回键逻辑 (转场期间禁用)
        if (transitionState == TransitionState.NONE && backKeyTrigger != null && backKeyTrigger.get()) {
            GScreen current = getCurScreen();
            boolean consumed = false;
            if (current != null) {
                consumed = current.handleBackKey();
            }
            
            if (!consumed) {
                if (!popLastScreen()) {
                    if (exitGame != null && !exitGame.isEmpty()) exitGame.forEach(r -> r.run());
                }
            }
        }

		if (!curScreen.isInitialized()) return;

		float delta = Gdx.graphics.getDeltaTime();
		curScreen.getUIViewport().apply();
		curScreen.render(delta);
		
		// 渲染转场效果
		renderTransition(delta);
	}

	private void renderTransition(float delta) {
		if (transitionState == TransitionState.NONE) return;

		transitionTime += delta;
		float alpha = 0f;

		if (transitionState == TransitionState.FADE_OUT) {
			alpha = Math.min(1f, transitionTime / transitionDuration);
			if (transitionTime >= transitionDuration) {
				// Fade Out 完成，执行中间操作
				if (onTransitionMiddle != null) {
					onTransitionMiddle.run();
					onTransitionMiddle = null; // 确保只执行一次
				}
				// 切换到 Fade In
				transitionState = TransitionState.FADE_IN;
				transitionTime = 0f;
				alpha = 1f; // 保持全黑一帧
			}
		} else if (transitionState == TransitionState.FADE_IN) {
			alpha = 1f - Math.min(1f, transitionTime / transitionDuration);
			if (transitionTime >= transitionDuration) {
				// Fade In 完成
				transitionState = TransitionState.NONE;
				if (onTransitionEnd != null) {
					onTransitionEnd.run();
					onTransitionEnd = null;
				}
				alpha = 0f;
			}
		}

		if (alpha > 0) {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			
			Camera cam = curScreen.getUIViewport().getCamera();
			shapeRenderer.setProjectionMatrix(cam.combined);
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(0, 0, 0, alpha);
			
			// 绘制一个覆盖相机的巨大矩形，确保覆盖全屏（包括可能的黑边区域，如果视口设置允许）
			// 但通常我们只需要覆盖视口区域。为了保险，画大一点。
			float w = curScreen.getUIViewport().getWorldWidth();
			float h = curScreen.getUIViewport().getWorldHeight();
			shapeRenderer.rect(cam.position.x - w, cam.position.y - h, w * 2, h * 2);
			
			shapeRenderer.end();
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}
	}

	/**
	 * 释放资源
	 */
	@Override
	public void dispose() {
		if (shapeRenderer != null) shapeRenderer.dispose();
		for (GScreen screen : screens.values()) {
			if (screen.isInitialized())
				screen.dispose();
		}
	}

	public void resize(int width, int height) {
		if(curScreen != null) curScreen.resize(width, height);
	}

	/**
	 * 添加并配置游戏屏幕
	 */
	public ScreenManager addScreen(GScreen screen) {
		Class<?> key = screen.getClass();
		if (!screens.containsKey(key)) {
			screens.put(key, screen);
			screen.setScreenManager(this);
			screen.setImp(new InputMultiplexer());
		}
		return this;
	}

	private void removeScreen(Class<? extends GScreen> key) {
		screens.remove(key);
	}

	/**
	 * 通过键获取游戏屏幕
	 */
	public GScreen getScreen(Class<?> key) {
		if (!screens.containsKey(key)) throw new RuntimeException("未找到此屏幕." + key.getSimpleName());
		return screens.get(key);
	}

	public boolean existsScreen(Class<?> key) {
		return screens.containsKey(key);
	}

	/**
	 * 获取当前屏幕
	 */
	public GScreen getCurScreen() {
		return curScreen;
	}

	public void setCurScreen(Class<? extends GScreen> key) {
		setCurScreen(key, false);
	}

	public void setCurScreen(GScreen screen) {
		// [修复] 自动依赖注入
		// 如果是临时 new 出来的屏幕，还没有绑定 Manager 或 Input，这里自动补全
		if (screen.getScreenManager() == null) {
			screen.setScreenManager(this);
		}
		if (screen.getImp() == null) {
			screen.setImp(new InputMultiplexer());
		}
		//如果屏幕未准备则初始化屏幕
		screen.initialize();
		//隐藏上个屏幕并切换到目标屏幕
		if (this.curScreen != null) {
			this.curScreen.hide();
			//如果为非popping状态，将旧屏幕推入历史堆栈以记录
			if (!popping) screenHistory.push(curScreen);
		}
		this.curScreen = screen;
		this.curScreen.show();
	}

	public void turnNewScreen(Class<? extends GScreen> key) {
		if(existsScreen(key)) {
			getScreen(key).dispose();
			removeScreen(key);
		}
		setCurScreen(key, true);
	}

	public void setCurScreen(Class<? extends GScreen> key, boolean autoCreate) {
		//自动加入管理屏幕中
		if (autoCreate && !existsScreen(key)) {
			try {
				addScreen(key.getConstructor().newInstance());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		setCurScreen(getScreen(key));
	}

	//回到上个屏幕
	public boolean popLastScreen() {
		if (screenHistory.isEmpty()) return false;
		GScreen lastScreen = screenHistory.pop();
		popping = true;
		setCurScreen(lastScreen);
		popping = false;
		return true;
	}

	public GScreen getLaunchScreen() {
		return launchScreen;
	}

	public ScreenManager setLaunchScreen(Class<? extends GScreen> key) {
		setLaunchScreen(getScreen(key));
		return this;
	}

	public ScreenManager setLaunchScreen(GScreen screen) {
		launchScreen = screen;
		if (!screen.isInitialized())
			setCurScreen(launchScreen);
		return this;
	}

	public void enableInput(InputMultiplexer screenImp) {
		imp.addProcessor(screenImp);
	}

	public void disableInput(InputMultiplexer screenImp) {
		imp.removeProcessor(screenImp);
	}

}
