package com.goldsprite.magicdungeon.screens.ecs.hub;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.magicdungeon.core.Gd;
import com.goldsprite.magicdungeon.core.config.MagicDungeonConfig;
import com.goldsprite.magicdungeon.ui.widget.BaseDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class SetupDialog extends BaseDialog {

	private final VisTextField pathField;
	private final Runnable onSuccess;

	public SetupDialog(Runnable onSuccess) {
		super("Welcome to MagicDungeon");
		this.onSuccess = onSuccess;

		getContentTable().add(new VisLabel("Initialize Engine Workspace")).padBottom(20).row();
		getContentTable().add(new VisLabel("Engine Root Directory:")).left().row();

		pathField = new VisTextField(MagicDungeonConfig.getRecommendedRoot());
		getContentTable().add(pathField).width(400).padBottom(20).row();

		VisTextButton btnConfirm = new VisTextButton("Initialize 和 Enter");
		btnConfirm.setColor(Color.CYAN);
		btnConfirm.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String path = pathField.getText().trim();
				if (path.isEmpty()) return;

				try {
					// 1. 初始化配置
					MagicDungeonConfig.initialize(path);

					// 2. 绑定到 Gd
					Gd.engineConfig = MagicDungeonConfig.getInstance();

					// 3. 回调跳转
					fadeOut();
					if (onSuccess != null) onSuccess.run();
				} catch (Exception e) {
					// 简单容错
					pathField.setText("Error creating dir!");
				}
			}
		});

		getContentTable().add(btnConfirm).growX().height(50);
		pack();
		centerWindow();
	}
}
