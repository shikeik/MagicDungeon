package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.GScreen;

public class BaseDialog extends VisDialog {
	public boolean autoPack = false;

	public BaseDialog(String title) {
		super(title);
		getTitleLabel().setFontScale(0.95f); // 字体太大修正
		setModal(true);
		addCloseButton();
		// closeOnEscape(); // 移除自动关闭，交由 ScreenManager.handleBackKey 统一处理，防止 Escape 同时触发关闭窗口和退出屏幕
		TableUtils.setSpacingDefaults(this);
	}

	// 方便子类显示
	public VisDialog show(Stage stage) {
		if(autoPack) pack();
		centerWindow();
		stage.addActor(this.fadeIn());
		
		// 注册到当前屏幕的 Dialog 栈
		GScreen curScreen = ScreenManager.getInstance().getCurScreen();
		if (curScreen != null) {
			curScreen.pushDialog(this);
		}
		
		return this;
	}
	
	@Override
	public boolean remove() {
		// 从当前屏幕的 Dialog 栈移除
		GScreen curScreen = ScreenManager.getInstance().getCurScreen();
		if (curScreen != null) {
			curScreen.popDialog(this);
		}
		return super.remove();
	}
}
