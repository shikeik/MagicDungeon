package com.goldsprite.gdengine.log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon.BuildConfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DLog {
	public static final String passStr = "Y";
	private static final float LOGICAL_SHORT = 540f;
	private static final float LOGICAL_LONG = 960f;
	public static boolean singleMode = false;
	public static String singleTag = "Default";
	// [新增] 黑白名单模式控制
	public static boolean isBlackListMode = true; // 默认黑名单模式
	public static List<String> blackList = new CopyOnWriteArrayList<>(); // 黑名单列表
	public static String[] showTags = {
		"Default Y",
		"拦截 N",

		//GDEngine
		"ToastUI Y",
		"ZipDownLoader Y",

		//MagicDungeon
		"VirtualKeyboard Y",
		"Settings Y",
		"InputManager Y",

		// Test
		"TEST Y",
		"VERIFY Y",
		"Test1 Y",
		"VisualCheck Y",
	};

	public static String LOG_TAG = BuildConfig.PROJECT_NAME;
	private static final Logger logger = new Logger(LOG_TAG);

	// 数据层 (构造时即可用)
	public static List<String> logMessages = new CopyOnWriteArrayList<>();
	public static boolean showDebugUI = true;
	public static boolean shortcuts = true;
	static int maxLogsCache = 100;
	// 视口配置
	static float scl = 2.5f;
	private static DLog instance;
	private static final List<String> logInfos = new CopyOnWriteArrayList<>();

	static {
		blackList.add("拦截");
		blackList.add("InputManager");
	}

	public DebugConsole console;
	// 表现层 (延后初始化)
	private Stage stage;

	// [修改] 构造函数只做最基础的数据准备，绝对不碰 UI
	public DLog() {
		instance = this;
	}

	public static DLog getInstance() {
		if (instance == null) new DLog();
		return instance;
	}

	public static List<String> getAllLogs() {
		return logMessages;
	}

	public static List<String> getLogs() {
		getInstance();
		return logMessages;
	}

	// --- 数据接口 ---

	public static String getInfoString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BuildConfig.PROJECT_NAME).append(": V").append(BuildConfig.DEV_VERSION);
		sb.append("\nHeap: ").append(Gdx.app.getJavaHeap() / 1024 / 1024).append("MB");
		sb.append("\nFPS: ").append(Gdx.graphics.getFramesPerSecond());

		getInstance();
		if (!logInfos.isEmpty()) {
			sb.append("\n--- Monitors ---\n");
			getInstance();
			sb.append(String.join("\n", logInfos));
		}
		return sb.toString();
	}

	public static void clearInfo() {
		if (getInstance() != null) {
			getInstance();
			logInfos.clear();
		}
	}

	public static void log(Object... values) {
		logT("Default", values);
	}

	public static void logT(String tag, Object... values) {
		if (banTag(tag)) {
			if (showTags[1].equals("拦截 Y"))
				logT("拦截", "被拦截的: " + formatString(values));
			return;
		}

		String msg = String.format("[%s] %s", tag, formatString(values));
		msg = String.format("[%s] %s", formatTime("HH:mm:ss:SSS"), msg);// 添加时间戳
		if (tag.equals("UserProject")) msg = "[ORANGE]" + msg; // (临时代码, 便于用户区分日志) 橙色标记用户项目日志
		msg = "[WHITE]" + msg; // 重置颜色标记

		logger.setLevel(Logger.NONE);
		logger.info(msg);

		//提供给UI
		logMessages.add(/*"NONE: " + */msg);
		//打印到控制台
		System.out.println(msg);
	}

	public static void logErr(Object... values) {
		logErrT("Default", values);
	}

	public static void logErrT(String tag, Object... values) {
		values[0] = "[RED]" + values[0];
		logT(tag, values);
	}

	public static void info(Object... values) {
		infoT("Default", values);
	}

	public static void infoT(String tag, Object... values) {
		if (banTag(tag)) return;

		String msg = String.format("[%s] %s", tag, formatString(values));

		logInfos.add(msg);
	}

	public static boolean banTag(String tag) {
		if (singleMode) {
			return !singleTag.equals(tag);
		}

		// [修改] 黑名单模式逻辑
		if (isBlackListMode) {
			// 如果在黑名单中，则拦截 (return true)
			// 否则放行 (return false)
			return blackList.contains(tag);
		}

		// 白名单模式逻辑 (原有)
		for (String tagInfo : showTags) {
			String[] splits = tagInfo.split(" ");
			if (splits.length < 2) continue;

			String key = splits[0];
			String show = splits[1];
			if (key.equals(tag))
				return !passStr.equals(show);
		}

		return true; // 白名单模式下，未找到则默认拦截
	}

	/**
	 * 格式化当前时间（Java 8+ 推荐）
	 *
	 * @param pattern 时间格式，如 "HH:mm:ss:SSS"
	 * @return 格式化后的时间字符串
	 */
	public static String formatTime(String pattern) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		return LocalTime.now().format(formatter);
	}

	public static String formatString(Object... values) {
		String msg;
		if (values.length == 1) {
			msg = String.valueOf(values[0]);
		} else {
			try {
				String format = String.valueOf(values[0]);
				Object[] args = Arrays.copyOfRange(values, 1, values.length);
				msg = String.format(format, args);
			} catch (Exception e) {
				msg = values[0] + " <FmtErr> " + Arrays.toString(Arrays.copyOfRange(values, 1, values.length));
			}
		}
		return msg;
	}

	public static void setIntros(String text) {
		// [修改] 增加判空，防止在 initUI 之前调用导致的崩溃
		if (getInstance().console != null) {
			getInstance().console.setIntros(text);
		}
	}

	/**
	 * [新增] 显式 UI 初始化方法
	 * 必须在 VisUIHelper.loadWithChineseFont() 之后调用
	 */
	public void initUI() {
		if (stage != null) return; // 防止重复初始化

		// [修改] 初始默认横屏
		stage = new Stage(new ExtendViewport(LOGICAL_LONG * scl, LOGICAL_SHORT * scl));

		console = new DebugConsole();
		stage.addActor(console);

		registerInput();

		// 打印一条调试信息验证顺序
//		log("DebugUI UI Initialized.");
	}

	private void registerInput() {
		Gdx.app.postRunnable(() -> {
			try {
				ScreenManager sm = ScreenManager.getInstance();
				if (sm != null && sm.getImp() != null) {
					sm.getImp().addProcessor(0, stage);
//					log("DebugUI Input Registered at Top.");
				} else {
//					log("Warning: ScreenManager not ready for DebugUI input.");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void render() {
		// [修改] 如果 UI 还没初始化，直接跳过渲染，但数据收集依然正常工作
		if (!showDebugUI || stage == null) return;

		stage.getViewport().apply(true);
		stage.act();
		stage.draw();
	}

	public void resize(int w, int h) {
		if (stage == null) return;

		scl = PlatformImpl.isAndroidUser() ? 1.4f : 2f;
		ExtendViewport vp = (ExtendViewport) stage.getViewport();
		if (h > w) {
			vp.setMinWorldWidth(LOGICAL_SHORT * scl);
			vp.setMinWorldHeight(LOGICAL_LONG * scl);
		} else {
			vp.setMinWorldWidth(LOGICAL_LONG * scl);
			vp.setMinWorldHeight(LOGICAL_SHORT * scl);
		}
		vp.update(w, h, true);
	}

	public void dispose() {
		if (stage != null) stage.dispose();
		if (console != null) console.dispose();
	}
}
