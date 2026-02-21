package com.goldsprite.magicdungeon2.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon2.ai.AIDrawCommand;
import com.goldsprite.magicdungeon2.ai.AIDrawMethodRegistry;
import com.goldsprite.magicdungeon2.ai.AIDrawPlan;
import com.goldsprite.magicdungeon2.utils.AssetUtils;
import com.goldsprite.magicdungeon2.utils.texturegenerator.NeonGenerator;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * AI 绘制回放编辑器
 *
 * 功能：
 * - 选择 JSON 绘制计划文件
 * - 逐步回放每条绘制命令
 * - 时间线控制: 单步前进/后退、进度条跳转、速率调节(1/2/4/8x)、播放/暂停
 * - 左侧预览区 + 右侧控制面板（复刻旧项目 NeonGenTestScreen 布局）
 */
public class AIDrawReplayScreen extends GScreen {
	private NeonBatch neonBatch;
	private SpriteBatch spriteBatch;
	private Stage stage;

	// 当前加载的绘制计划
	private AIDrawPlan currentPlan;
	private String currentFileName = "";
	private AIDrawCommand[] drawCommands; // 过滤掉 save 指令后的命令列表

	// 回放状态
	private int currentStep = 0;      // 当前执行到第几步 (0=无, 1=第一条, ...)
	private int totalSteps = 0;
	private boolean playing = false;
	private float playTimer = 0;
	private float[] speedOptions = {1f, 2f, 4f, 8f};
	private int speedIndex = 0;       // 默认 1x

	// 预览缓存
	private TextureRegion bakedRegion;
	private boolean needsRebake = true;

	// 显示设置
	private float displayScale = 256f;

	// UI 引用
	private VisTable gameArea;
	private VisLabel infoLabel;
	private VisLabel stepLabel;
	private VisSlider progressSlider;
	private VisTextButton playBtn;
	private boolean sliderDragging = false;

	// 可用文件列表
	private String[] fileNames;

	@Override
	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		stage = new Stage(getUIViewport(), spriteBatch);

		if (imp != null) imp.addProcessor(stage);

		loadFileList();
		setupUI();

		// 默认加载第一个文件
		if (fileNames.length > 0) loadPlan(fileNames[0]);
	}

	private void loadFileList() {
		AssetUtils.loadIndex();
		Array<String> jsonFiles = new Array<>();
		scanDirectory("ai_draw_cmds", "", jsonFiles);
		jsonFiles.sort();
		fileNames = jsonFiles.toArray(String.class);
	}

	/**
	 * 递归扫描目录，收集所有 JSON 文件名（带路径前缀）
	 * @param dirPath  assets 相对路径（如 "ai_draw_cmds" 或 "ai_draw_cmds/ui"）
	 * @param prefix   显示名前缀（如 "" 或 "ui/"）
	 * @param outFiles 输出列表
	 */
	private void scanDirectory(String dirPath, String prefix, Array<String> outFiles) {
		String[] raw = AssetUtils.listNames(dirPath);
		for (String f : raw) {
			if (f.endsWith(".json")) {
				outFiles.add(prefix + f.replace(".json", ""));
			} else {
				// 可能是子目录，递归扫描
				scanDirectory(dirPath + "/" + f, prefix + f + "/", outFiles);
			}
		}
	}

	private void loadPlan(String name) {
		currentFileName = name;
		try {
			String jsonText = Gdx.files.internal("ai_draw_cmds/" + name + ".json").readString("UTF-8");
			Json json = new Json();
			currentPlan = json.fromJson(AIDrawPlan.class, jsonText);
		} catch (Exception e) {
			currentPlan = null;
		}

		// 过滤掉 save 指令
		if (currentPlan != null && currentPlan.commands != null) {
			Array<AIDrawCommand> filtered = new Array<>();
			for (AIDrawCommand cmd : currentPlan.commands) {
				if (cmd != null && cmd.type != null
					&& !cmd.type.equalsIgnoreCase("save")
					&& !cmd.type.equalsIgnoreCase("saves")) {
					filtered.add(cmd);
				}
			}
			drawCommands = filtered.toArray(AIDrawCommand.class);
			totalSteps = drawCommands.length;
		} else {
			drawCommands = new AIDrawCommand[0];
			totalSteps = 0;
		}

		// 重置回放状态
		currentStep = totalSteps; // 显示完整纹理
		playing = false;
		needsRebake = true;
		updateSlider();
		updateStepLabel();
	}

	private void setupUI() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// ====== 右侧控制面板 ======
		VisTable controls = new VisTable(true);
		controls.top().pad(10);

		controls.add(new VisLabel("AI 绘制回放")).expandX().row();
		controls.addSeparator().padBottom(8).expandX().fillX().row();

		// 文件选择
		controls.add(new VisLabel("绘制计划:")).left().row();
		VisSelectBox<String> fileSelect = new VisSelectBox<>();
		fileSelect.setItems(fileNames);
		if (fileNames.length > 0) fileSelect.setSelected(fileNames[0]);
		fileSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				loadPlan(fileSelect.getSelected());
			}
		});
		controls.add(fileSelect).expandX().fillX().row();

		controls.addSeparator().pad(8).expandX().fillX().row();

		// ====== 时间线控制 ======
		controls.add(new VisLabel("回放控制")).left().row();

		// 步数标签
		stepLabel = new VisLabel("0 / 0");
		controls.add(stepLabel).left().row();

		// 进度条
		progressSlider = new VisSlider(0, 1, 1, false);
		progressSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (sliderDragging || !playing) {
					int val = (int) progressSlider.getValue();
					if (val != currentStep) {
						currentStep = val;
						needsRebake = true;
						updateStepLabel();
					}
				}
			}
		});
		// 检测拖拽状态
		progressSlider.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
			@Override
			public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				sliderDragging = true;
				playing = false;
				playBtn.setText("▶ 播放");
				return false;
			}
			@Override
			public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				sliderDragging = false;
			}
		});
		controls.add(progressSlider).expandX().fillX().row();

		// 按钮行: |< < ▶/❚❚ > >|
		VisTable btnRow = new VisTable(true);

		VisTextButton firstBtn = new VisTextButton("|<");
		firstBtn.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent e, Actor a) { jumpTo(0); }
		});

		VisTextButton prevBtn = new VisTextButton("<");
		prevBtn.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent e, Actor a) { stepBack(); }
		});

		playBtn = new VisTextButton("▶ 播放");
		playBtn.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent e, Actor a) { togglePlay(); }
		});

		VisTextButton nextBtn = new VisTextButton(">");
		nextBtn.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent e, Actor a) { stepForward(); }
		});

		VisTextButton lastBtn = new VisTextButton(">|");
		lastBtn.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent e, Actor a) { jumpTo(totalSteps); }
		});

		btnRow.add(firstBtn).width(36);
		btnRow.add(prevBtn).width(36);
		btnRow.add(playBtn).expandX().fillX();
		btnRow.add(nextBtn).width(36);
		btnRow.add(lastBtn).width(36);
		controls.add(btnRow).expandX().fillX().row();

		// 速率选择
		VisTable speedRow = new VisTable(true);
		speedRow.add(new VisLabel("速率:")).left();
		String[] speedLabels = {"1x", "2x", "4x", "8x"};
		VisSelectBox<String> speedSelect = new VisSelectBox<>();
		speedSelect.setItems(speedLabels);
		speedSelect.setSelected(speedLabels[speedIndex]);
		speedSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String sel = speedSelect.getSelected();
				for (int i = 0; i < speedLabels.length; i++) {
					if (speedLabels[i].equals(sel)) { speedIndex = i; break; }
				}
			}
		});
		speedRow.add(speedSelect).expandX().fillX();
		controls.add(speedRow).expandX().fillX().row();

		controls.addSeparator().pad(8).expandX().fillX().row();

		// 显示设置
		controls.add(new VisLabel("预览大小:")).left().row();
		VisSlider scaleSlider = new VisSlider(64, 512, 32, false);
		scaleSlider.setValue(displayScale);
		VisLabel scaleLabel = new VisLabel(String.valueOf((int) displayScale));
		scaleSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				displayScale = scaleSlider.getValue();
				scaleLabel.setText(String.valueOf((int) displayScale));
			}
		});
		VisTable scaleRow = new VisTable(true);
		scaleRow.add(scaleSlider).expandX().fillX();
		scaleRow.add(scaleLabel).width(40);
		controls.add(scaleRow).expandX().fillX().row();

		controls.addSeparator().pad(8).expandX().fillX().row();

		// 当前命令信息
		infoLabel = new VisLabel("");
		controls.add(infoLabel).left().growY().top();

		HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(controls);
		scrollPane.setFlickScroll(false);
		scrollPane.setFadeScrollBars(false);

		// ====== 左侧预览区 ======
		gameArea = new VisTable();
		gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.12f, 0.12f, 0.15f, 1f)));

		VisSplitPane split = new VisSplitPane(gameArea, scrollPane, false);
		split.setSplitAmount(0.7f);
		split.setMinSplitAmount(0.3f);
		split.setMaxSplitAmount(0.85f);

		root.add(split).expand().fill();
		stage.addActor(root);
	}

	// ====== 回放控制方法 ======

	private void togglePlay() {
		playing = !playing;
		if (playing) {
			playBtn.setText("❚❚ 暂停");
			playTimer = 0;
			// 如果已经到末尾，从头开始
			if (currentStep >= totalSteps) {
				currentStep = 0;
				needsRebake = true;
			}
		} else {
			playBtn.setText("▶ 播放");
		}
	}

	private void stepForward() {
		if (currentStep < totalSteps) {
			currentStep++;
			needsRebake = true;
			updateSlider();
			updateStepLabel();
		}
	}

	private void stepBack() {
		if (currentStep > 0) {
			currentStep--;
			needsRebake = true;
			updateSlider();
			updateStepLabel();
		}
	}

	private void jumpTo(int step) {
		currentStep = MathUtils.clamp(step, 0, totalSteps);
		playing = false;
		playBtn.setText("▶ 播放");
		needsRebake = true;
		updateSlider();
		updateStepLabel();
	}

	private void updateSlider() {
		progressSlider.setRange(0, Math.max(totalSteps, 1));
		progressSlider.setValue(currentStep);
	}

	private void updateStepLabel() {
		stepLabel.setText(currentStep + " / " + totalSteps);

		// 更新当前命令信息
		StringBuilder sb = new StringBuilder();
		sb.append("文件: ").append(currentFileName).append('\n');
		if (currentPlan != null) {
			int w = currentPlan.width != null ? currentPlan.width : 256;
			int h = currentPlan.height != null ? currentPlan.height : 256;
			sb.append("尺寸: ").append(w).append('x').append(h).append('\n');
		}
		sb.append("步骤: ").append(currentStep).append('/').append(totalSteps).append('\n');
		sb.append("速率: ").append(speedOptions[speedIndex]).append("x\n\n");

		if (currentStep > 0 && currentStep <= drawCommands.length) {
			AIDrawCommand cmd = drawCommands[currentStep - 1];
			sb.append("当前命令 #").append(currentStep).append(":\n");
			sb.append("  类型: ").append(cmd.type).append('\n');
			if (cmd.color != null) sb.append("  颜色: ").append(cmd.color).append('\n');
			if (cmd.color2 != null) sb.append("  副色: ").append(cmd.color2).append('\n');
			if (cmd.filled != null) sb.append("  填充: ").append(cmd.filled).append('\n');
			if (cmd.segments != null) sb.append("  分段: ").append(cmd.segments).append('\n');
			if (cmd.args != null) {
				sb.append("  参数: [");
				for (int i = 0; i < cmd.args.length; i++) {
					if (i > 0) sb.append(", ");
					sb.append(String.format("%.3f", cmd.args[i]));
				}
				sb.append("]\n");
			}
		}
		infoLabel.setText(sb.toString());
	}

	// ====== 渲染部分纹理（执行前 N 步命令） ======

	private void rebakePartial() {
		if (currentPlan == null || drawCommands.length == 0) {
			bakedRegion = null;
			return;
		}

		int w = currentPlan.width != null ? currentPlan.width : 256;
		int h = currentPlan.height != null ? currentPlan.height : 256;
		final int steps = currentStep;

		// 释放旧纹理
		if (bakedRegion != null && bakedRegion.getTexture() != null) {
			bakedRegion.getTexture().dispose();
			bakedRegion = null;
		}

		bakedRegion = NeonGenerator.getInstance().generate(w, h, (NeonBatch batch) -> {
			for (int i = 0; i < steps && i < drawCommands.length; i++) {
				AIDrawMethodRegistry.invoke(batch, drawCommands[i]);
			}
		});

		needsRebake = false;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 播放逻辑
		if (playing && totalSteps > 0) {
			float interval = 0.5f / speedOptions[speedIndex]; // 基础间隔 0.5s
			playTimer += delta;
			while (playTimer >= interval && playing) {
				playTimer -= interval;
				currentStep++;
				needsRebake = true;
				if (currentStep >= totalSteps) {
					currentStep = totalSteps;
					playing = false;
					playBtn.setText("▶ 播放");
					break;
				}
			}
			updateSlider();
			updateStepLabel();
		}

		// 按需重新烘焙
		if (needsRebake) rebakePartial();

		stage.act(delta);
		stage.draw();

		// 在预览区域绘制纹理
		drawPreview();
	}

	private void drawPreview() {
		if (bakedRegion == null) return;

		Vector2 pos = gameArea.localToStageCoordinates(new Vector2(0, 0));
		float x = pos.x, y = pos.y;
		float w = gameArea.getWidth(), h = gameArea.getHeight();

		Rectangle scissor = new Rectangle();
		Rectangle clipBounds = new Rectangle(x, y, w, h);
		ScissorStack.calculateScissors(stage.getCamera(), spriteBatch.getTransformMatrix(), clipBounds, scissor);

		if (ScissorStack.pushScissors(scissor)) {
			float rw = bakedRegion.getRegionWidth();
			float rh = bakedRegion.getRegionHeight();

			// 等比缩放到 displayScale
			float ratio = rw / rh;
			float cw, ch;
			if (ratio >= 1) { cw = displayScale; ch = displayScale / ratio; }
			else { ch = displayScale; cw = displayScale * ratio; }

			float cx = x + (w - cw) / 2;
			float cy = y + (h - ch) / 2;

			spriteBatch.setProjectionMatrix(stage.getCamera().combined);
			spriteBatch.begin();
			spriteBatch.draw(bakedRegion, cx, cy, cw, ch);
			spriteBatch.end();

			ScissorStack.popScissors();
		}
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (spriteBatch != null) spriteBatch.dispose();
		if (neonBatch != null) neonBatch.dispose();
		if (stage != null) stage.dispose();
		if (bakedRegion != null && bakedRegion.getTexture() != null) {
			bakedRegion.getTexture().dispose();
		}
	}
}
