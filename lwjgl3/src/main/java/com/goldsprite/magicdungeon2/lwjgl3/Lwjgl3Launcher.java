package com.goldsprite.magicdungeon2.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.goldsprite.GdxLauncher;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon2.BuildConfig;

/**
 * Launches the desktop (LWJGL3) application.
 */
public class Lwjgl3Launcher {
	// 定义基础尺寸
	static float scl = 1.5f;
	public static final float WORLD_WIDTH = 960 * scl;
	public static final float WORLD_HEIGHT = 540 * scl;

	// 双开测试支持：通过系统属性区分实例
	public static final String INSTANCE_ID = System.getProperty("app.instance", "0");
	public static final String WINDOW_TITLE_SUFFIX = System.getProperty("app.window.title", "");
	public static final int WINDOW_X = Integer.getInteger("app.window.x", -1);
	public static final int WINDOW_Y = Integer.getInteger("app.window.y", -1);

	public static void main(String[] args) {
		if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.

		// [新增] 全局异常捕获，确保崩溃时终止程序和 Gradle 任务
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			System.err.println("[CRITICAL] Uncaught exception in thread " + t.getName());
			e.printStackTrace();
			System.exit(1); // 强制以错误码退出
		});

		createApplication();
	}

	private static Lwjgl3Application createApplication() {

		// 注入浏览器实现
//		PlatformImpl.webBrower = new DesktopWebBrowser();

		// [修改] 注入到游戏主入口
		return new Lwjgl3Application(new GdxLauncher(), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

		// 窗口标题：双开时附加实例标识
		String title = BuildConfig.PROJECT_NAME + " - V" + BuildConfig.DEV_VERSION;
		if (!WINDOW_TITLE_SUFFIX.isEmpty()) {
			title += "  [" + WINDOW_TITLE_SUFFIX + "]";
		}
		configuration.setTitle(title);

		Graphics.DisplayMode primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		configuration.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);

		// 双开时指定窗口位置，避免重叠
		if (WINDOW_X >= 0 && WINDOW_Y >= 0) {
			configuration.setWindowPosition(WINDOW_X, WINDOW_Y);
		}
		configuration.setDecorated(true);
		configuration.setResizable(true);

		//实现退出接口
		ScreenManager.exitGame.add(() -> {
			Gdx.app.exit();
		});

		// 实现全屏接口
		PlatformImpl.fullScreenEvent = (fullScreen) -> {
			if (fullScreen) {
				// 切换到全屏
				Gdx.graphics.setFullscreenMode(primaryMode);
				configuration.setDecorated(false); // 全屏模式下通常禁用窗口装饰
				configuration.setResizable(false); // 全屏模式下通常不允许调整大小
			} else {
				// 切换到窗口模式
				Gdx.graphics.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);
				configuration.setDecorated(true);
				configuration.setResizable(true);
			}
		};

		// 可选：设置全屏模式下的其他参数
		configuration.setInitialVisible(true);
		configuration.setAutoIconify(true); // 当失去焦点时最小化

		// 全屏模式下建议启用垂直同步以减少屏幕撕裂
		configuration.useVsync(false);
		// 全屏模式下使用显示器刷新率
//		configuration.setForegroundFPS(primaryMode.refreshRate);
		configuration.setForegroundFPS(240);

		//// 如果你想要窗口最大化模式：
		// configuration.setMaximized(true);

		//// You can change these files; they are in lwjgl3/src/main/resources/ .
		configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

		configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4); // r, g, b, a, depth, stencil, samples=4

		return configuration;
	}
}
