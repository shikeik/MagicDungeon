package com.goldsprite.gdengine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.Debug;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class VisUIHelper {
	public static BitmapFont cnFont;
	public static BitmapFont cnFontSmall;

	private static Skin gameSkin;

	/**
	 * 初始化 UI (VisUI + Game Skin) 并注入中文字体
	 */
	public static void loadWithChineseFont() {
		init();
	}

	public static void init() {
		if (VisUI.isLoaded()) return;

		try {
			Debug.log("开始初始化 UI 系统...");

			// 1. 准备字体
			// 大小设为 24，清晰度较高
			cnFont = FontUtils.generateAutoClarity(24);
			cnFont.getData().setScale(1.0f); // 重置缩放，根据需要调整
			
			// 小字体
			BitmapFont smFont = FontUtils.generate(20);
			cnFontSmall = smFont;

			// 2. 加载主皮肤 (Shimmer UI)
			Skin mainSkin = new Skin();
			// 添加 TenPatch 支持 (如果项目中使用了 com.ray3k.tenpatch)
			// 注意：如果 TenPatch 类不存在，这里可能会报错，但我们假设依赖已添加
			try {
				// 尝试通过反射注册，避免编译错误如果依赖尚未刷新
				Class<?> tenPatchClass = Class.forName("com.ray3k.tenpatch.TenPatchDrawable");
				// Skin 不需要显式注册类，只要 JSON 里写了全限定名且 Classpath 里有即可
				Debug.log("检测到 TenPatch 库，支持动态切片皮肤。");
			} catch (ClassNotFoundException e) {
				Debug.log("未检测到 TenPatch 库，加载 Shimmer UI 可能会失败。");
			}

			TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui_skins/shimmer-ui/shimmer-ui.atlas"));
			mainSkin.addRegions(atlas);
			mainSkin.load(Gdx.files.internal("ui_skins/shimmer-ui/shimmer-ui.json"));

			// 3. 注入字体到主皮肤
			injectFonts(mainSkin, cnFont, cnFontSmall);

			// 4. 将标准样式映射为 VisUI 样式 (因为 Shimmer 是标准 LibGDX 皮肤)
			mapStandardStylesToVisStyles(mainSkin);

			// 5. 加载 VisUI
			VisUI.load(mainSkin);
			Debug.log("VisUI (Shimmer) 加载成功。");

			// 6. 修复一些细节 (保留之前的逻辑)
			fixHandleSize();
			addGlobalAssetsStyles(mainSkin, cnFont, cnFontSmall);
			
			// [新增] 修复 Window 样式，去除不需要的背景或调整
			// Shimmer 的 window 样式通常比较现代，可能不需要额外去边框，但保留逻辑以防万一
			if (!mainSkin.has("window-noborder", Drawable.class)) {
				// 创建一个透明 drawable 作为 noborder
				mainSkin.add("window-noborder", new TextureRegionDrawable(ColorTextureUtils.createColorTexture(new Color(0,0,0,0.5f))));
			}
			
			// 7. 加载游戏内容皮肤 (Neutralizer)
			loadGameSkin();

		} catch (Exception e) {
			e.printStackTrace();
			Debug.log("UI 初始化失败: " + e.getMessage());
			// 失败回退
			if (!VisUI.isLoaded()) VisUI.load();
		}
	}

	public static Skin getGameSkin() {
		if (gameSkin == null) {
			loadGameSkin();
		}
		return gameSkin;
	}

	private static void loadGameSkin() {
		try {
			gameSkin = new Skin();
			TextureAtlas gameAtlas = new TextureAtlas(Gdx.files.internal("ui_skins/Neutralizer_UI_Skin/neutralizerui/neutralizer-ui.atlas"));
			gameSkin.addRegions(gameAtlas);
			gameSkin.load(Gdx.files.internal("ui_skins/Neutralizer_UI_Skin/neutralizerui/neutralizer-ui.json"));
			
			// 注入字体
			injectFonts(gameSkin, cnFont, cnFontSmall);
			
			Debug.log("游戏内容皮肤 (Neutralizer) 加载成功。");
		} catch (Exception e) {
			Debug.log("游戏内容皮肤加载失败: " + e.getMessage());
			gameSkin = VisUI.getSkin(); // 回退到主皮肤
		}
	}

	private static void injectFonts(Skin skin, BitmapFont font, BitmapFont smallFont) {
		// 注册字体资源
		skin.add("default-font", font, BitmapFont.class);
		skin.add("font", font, BitmapFont.class); // Shimmer 常用
		skin.add("small-font", smallFont, BitmapFont.class);
		skin.add("small", smallFont, BitmapFont.class); // Shimmer 常用

		// 替换所有已知样式的字体
		for (Label.LabelStyle style : skin.getAll(Label.LabelStyle.class).values()) {
			if (style.font != null) style.font = font; 
			// 如果是 small 样式，使用小字体 (简单判断)
			// 无法直接判断 style 名称，只能遍历 map
		}
		
		// 针对特定名称的修正
		if (skin.has("small", Label.LabelStyle.class)) {
			skin.get("small", Label.LabelStyle.class).font = smallFont;
		}
		
		for (TextButton.TextButtonStyle style : skin.getAll(TextButton.TextButtonStyle.class).values()) {
			style.font = font;
		}
		for (TextField.TextFieldStyle style : skin.getAll(TextField.TextFieldStyle.class).values()) {
			style.font = font;
		}
		for (CheckBox.CheckBoxStyle style : skin.getAll(CheckBox.CheckBoxStyle.class).values()) {
			style.font = font;
		}
		for (List.ListStyle style : skin.getAll(List.ListStyle.class).values()) {
			style.font = font;
		}
		for (SelectBox.SelectBoxStyle style : skin.getAll(SelectBox.SelectBoxStyle.class).values()) {
			style.font = font;
			style.listStyle.font = font;
		}
		for (Window.WindowStyle style : skin.getAll(Window.WindowStyle.class).values()) {
			style.titleFont = font;
		}
	}

	private static void mapStandardStylesToVisStyles(Skin skin) {
		// VisUI 组件通常需要特定的 Vis*Style。如果皮肤里没有，我们需要从标准样式复制。
		// 1. TextButton -> VisTextButton
		ObjectMap<String, TextButton.TextButtonStyle> textButtonStyles = skin.getAll(TextButton.TextButtonStyle.class);
		for (String name : textButtonStyles.keys()) {
			TextButton.TextButtonStyle original = textButtonStyles.get(name);
			VisTextButton.VisTextButtonStyle visStyle = new VisTextButton.VisTextButtonStyle();
			visStyle.up = original.up;
			visStyle.down = original.down;
			visStyle.over = original.over;
			visStyle.checked = original.checked;
			visStyle.checkedOver = original.checkedOver;
			visStyle.disabled = original.disabled;
			visStyle.font = original.font;
			visStyle.fontColor = original.fontColor;
			visStyle.downFontColor = original.downFontColor;
			visStyle.overFontColor = original.overFontColor;
			visStyle.checkedFontColor = original.checkedFontColor;
			visStyle.checkedOverFontColor = original.checkedOverFontColor;
			visStyle.disabledFontColor = original.disabledFontColor;
			// Vis 特有字段，默认给个 null 或者 reusing up
			visStyle.focusBorder = original.up; 
			skin.add(name, visStyle);
		}

		// 2. CheckBox -> VisCheckBox
		ObjectMap<String, CheckBox.CheckBoxStyle> checkBoxStyles = skin.getAll(CheckBox.CheckBoxStyle.class);
		for (String name : checkBoxStyles.keys()) {
			CheckBox.CheckBoxStyle original = checkBoxStyles.get(name);
			VisCheckBox.VisCheckBoxStyle visStyle = new VisCheckBox.VisCheckBoxStyle();
			visStyle.font = original.font;
			visStyle.fontColor = original.fontColor;
			visStyle.disabledFontColor = original.disabledFontColor;
			// Vis 特有
			visStyle.focusBorder = null;
			visStyle.checkBackground = original.checkboxOff; // Mapping
			visStyle.tick = original.checkboxOn; // Mapping
			skin.add(name, visStyle);
		}

		// 3. ImageButton -> VisImageButton
		ObjectMap<String, ImageButton.ImageButtonStyle> imageButtonStyles = skin.getAll(ImageButton.ImageButtonStyle.class);
		for (String name : imageButtonStyles.keys()) {
			ImageButton.ImageButtonStyle original = imageButtonStyles.get(name);
			VisImageButton.VisImageButtonStyle visStyle = new VisImageButton.VisImageButtonStyle();
			visStyle.up = original.up;
			visStyle.down = original.down;
			visStyle.over = original.over;
			visStyle.checked = original.checked;
			visStyle.checkedOver = original.checkedOver;
			visStyle.disabled = original.disabled;
			visStyle.imageUp = original.imageUp;
			visStyle.imageDown = original.imageDown;
			visStyle.imageOver = original.imageOver;
			visStyle.imageChecked = original.imageChecked;
			visStyle.imageCheckedOver = original.imageCheckedOver;
			visStyle.imageDisabled = original.imageDisabled;
			visStyle.focusBorder = original.up;
			skin.add(name, visStyle);
		}

		// 4. TextField -> VisTextField
		ObjectMap<String, TextField.TextFieldStyle> textFieldStyles = skin.getAll(TextField.TextFieldStyle.class);
		for (String name : textFieldStyles.keys()) {
			TextField.TextFieldStyle original = textFieldStyles.get(name);
			VisTextField.VisTextFieldStyle visStyle = new VisTextField.VisTextFieldStyle();
			visStyle.font = original.font;
			visStyle.fontColor = original.fontColor;
			visStyle.background = original.background;
			visStyle.cursor = original.cursor;
			visStyle.selection = original.selection;
			visStyle.messageFont = original.messageFont;
			visStyle.messageFontColor = original.messageFontColor;
			// Vis 特有
			visStyle.errorBorder = original.background; // Fallback
			visStyle.focusBorder = original.background; // Fallback
			skin.add(name, visStyle);
		}
		
		// 5. Window -> VisWindow (其实 VisWindow 用的是 WindowStyle，但需要检查)
		// VisWindow uses WindowStyle, so no mapping needed if standard WindowStyle exists.
		
		// 6. SplitPane -> VisSplitPane
		ObjectMap<String, SplitPane.SplitPaneStyle> splitStyles = skin.getAll(SplitPane.SplitPaneStyle.class);
		for (String name : splitStyles.keys()) {
			SplitPane.SplitPaneStyle original = splitStyles.get(name);
			VisSplitPane.VisSplitPaneStyle visStyle = new VisSplitPane.VisSplitPaneStyle();
			visStyle.handle = original.handle;
			// VisSplitPaneStyle defines handleOver but standard doesn't
			visStyle.handleOver = original.handle;
			
			// VisUI expects specific names "default-vertical" and "default-horizontal"
			// Shimmer might only have "default-horizontal" and "default-vertical" or just "default"
			// We'll map whatever we find
			skin.add(name, visStyle);
		}
		
		// Ensure default-vertical/horizontal exist for VisSplitPane
		if (!skin.has("default-vertical", VisSplitPane.VisSplitPaneStyle.class) && skin.has("default-vertical", SplitPane.SplitPaneStyle.class)) {
			// Already handled by loop
		}
	}

	private static void addGlobalAssetsStyles(Skin skin, BitmapFont font, BitmapFont smallFont) {
		// Logger Style
		Label.LabelStyle loggerLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
		loggerLabelStyle.font = font; 
		skin.add("loggerLabelStyle", loggerLabelStyle);

		// Title
		if (!skin.has("title", Label.LabelStyle.class)) {
			Label.LabelStyle titleLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
			titleLabelStyle.font = font;
			skin.add("title", titleLabelStyle);
		} else {
			skin.get("title", Label.LabelStyle.class).font = font;
		}

		// Subtitle
		if (!skin.has("subtitle", Label.LabelStyle.class)) {
			Label.LabelStyle subtitleLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
			subtitleLabelStyle.font = smallFont;
			skin.add("subtitle", subtitleLabelStyle);
		} else {
			skin.get("subtitle", Label.LabelStyle.class).font = smallFont;
		}

		// No Background TextField
		TextField.TextFieldStyle textFieldStyle = skin.get(TextField.TextFieldStyle.class);
		TextField.TextFieldStyle noBackgroundTextFieldStyle = new TextField.TextFieldStyle(textFieldStyle);
		noBackgroundTextFieldStyle.background = null;
		skin.add("nobackground", noBackgroundTextFieldStyle);

		// No Background ScrollPane
		ScrollPane.ScrollPaneStyle defaultScrollStyle = skin.get(ScrollPane.ScrollPaneStyle.class);
		ScrollPane.ScrollPaneStyle noBackgroundScrollStyle = new ScrollPane.ScrollPaneStyle(defaultScrollStyle);
		noBackgroundScrollStyle.background = null;
		skin.add("nobackground", noBackgroundScrollStyle);
	}

	private static void fixHandleSize() {
		Skin skin = VisUI.getSkin();
		float size = 20f;
		float splitBarThickness = PlatformImpl.isAndroidUser() ? 15f : 8f;
		float g = 0.4f;
		TextureRegionDrawable cDrawable = ColorTextureUtils.createColorDrawable(new Color(g, g, g, 0.4f));

		// Fix SplitPane handles if they exist
		if (skin.has("default-vertical", SplitPane.SplitPaneStyle.class)) {
			SplitPane.SplitPaneStyle splitPaneStyle = skin.get("default-vertical", SplitPane.SplitPaneStyle.class);
			splitPaneStyle.handle = cDrawable;
			splitPaneStyle.handle.setMinHeight(splitBarThickness);
		}
		
		if (skin.has("default-horizontal", SplitPane.SplitPaneStyle.class)) {
			SplitPane.SplitPaneStyle splitPaneStyleHori = skin.get("default-horizontal", SplitPane.SplitPaneStyle.class);
			splitPaneStyleHori.handle = cDrawable;
			splitPaneStyleHori.handle.setMinWidth(splitBarThickness);
		}

		if (skin.has("default-vertical", VisSplitPane.VisSplitPaneStyle.class)) {
			VisSplitPane.VisSplitPaneStyle visSplitPaneStyle = skin.get("default-vertical", VisSplitPane.VisSplitPaneStyle.class);
			visSplitPaneStyle.handle = cDrawable;
			visSplitPaneStyle.handle.setMinWidth(splitBarThickness);
		}

		if (skin.has("default-horizontal", VisSplitPane.VisSplitPaneStyle.class)) {
			VisSplitPane.VisSplitPaneStyle visSplitPaneStyleHori = skin.get("default-horizontal", VisSplitPane.VisSplitPaneStyle.class);
			visSplitPaneStyleHori.handle = cDrawable;
			visSplitPaneStyleHori.handle.setMinHeight(splitBarThickness);
		}

		// VisTree fixes
		if (skin.has("default", VisTree.TreeStyle.class)) {
			VisTree.TreeStyle treeStyle = skin.get("default", VisTree.TreeStyle.class);
			if (treeStyle.plus != null) {
				treeStyle.plus.setMinWidth(size);
				treeStyle.plus.setMinHeight(size);
			}
			if (treeStyle.minus != null) {
				treeStyle.minus.setMinWidth(size);
				treeStyle.minus.setMinHeight(size);
			}
		}

		// VisCheckBox fixes
		size = 30;
		if (skin.has("default", VisCheckBox.VisCheckBoxStyle.class)) {
			VisCheckBox.VisCheckBoxStyle checkBoxStyle = skin.get(VisCheckBox.VisCheckBoxStyle.class);
			if (checkBoxStyle.checkBackground != null) {
				checkBoxStyle.checkBackground.setMinWidth(size);
				checkBoxStyle.checkBackground.setMinHeight(size);
			}
			// ... other checks
		}
	}
}
