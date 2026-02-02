package com.goldsprite.magicdungeon.screens.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.magicdungeon.ecs.GameWorld;
import com.goldsprite.magicdungeon.ecs.entity.GObject;
import com.goldsprite.magicdungeon.ecs.component.NeonAnimatorComponent;
import com.goldsprite.magicdungeon.ecs.component.SkeletonComponent;
import com.goldsprite.magicdungeon.ecs.system.SkeletonSystem;
import com.goldsprite.magicdungeon.log.Debug;
import com.goldsprite.magicdungeon.screens.ScreenManager;
import com.goldsprite.magicdungeon.screens.basics.ExampleGScreen;
import com.goldsprite.magicdungeon.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon.neonbatch.NeonStage;
import com.goldsprite.magicdungeon.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.magicdungeon.ecs.skeleton.data.NeonJsonUtils;
import com.goldsprite.magicdungeon.ui.widget.BioCodeEditor;
import com.goldsprite.magicdungeon.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.magicdungeon.screens.ecs.skeleton.TestSkeletonFactory;
import com.goldsprite.magicdungeon.ecs.system.WorldRenderSystem;

public class JsonLiveEditScreen extends ExampleGScreen {

	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage;

	private NeonAnimatorComponent playerAnimator;
	private BioCodeEditor codeEditor;
	private ToastUI toast;

	// [修改] 替换默认模板：站立挥剑 (Slash)
	private final String DEFAULT_JSON = "{\n" +
	"  \"name\": \"Slash_Test\",\n" +
	"  \"duration\": 0.8,\n" +
	"  \"looping\": true,\n" +
	"  \"timelines\": [\n" +
	"    {\n" +
	"      \"bone\": \"Body\",\n" +
	"      \"prop\": \"ROTATION\",\n" +
	"      \"keys\": [\n" +
	"        { \"t\": 0.0, \"v\": 90.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.2, \"v\": 80.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.4, \"v\": 110.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.8, \"v\": 90.0, \"c\": \"SMOOTH\" }\n" +
	"      ]\n" +
	"    },\n" +
	"    {\n" +
	"      \"bone\": \"Arm_Front_Up\",\n" +
	"      \"prop\": \"ROTATION\",\n" +
	"      \"keys\": [\n" +
	"        { \"t\": 0.0, \"v\": -160.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.2, \"v\": -200.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.4, \"v\": 0.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.8, \"v\": -160.0, \"c\": \"SMOOTH\" }\n" +
	"      ]\n" +
	"    },\n" +
	"    {\n" +
	"      \"bone\": \"Arm_Front_Low\",\n" +
	"      \"prop\": \"ROTATION\",\n" +
	"      \"keys\": [\n" +
	"        { \"t\": 0.0, \"v\": 10.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.2, \"v\": 120.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.4, \"v\": 0.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.8, \"v\": 10.0, \"c\": \"SMOOTH\" }\n" +
	"      ]\n" +
	"    },\n" +
	"    {\n" +
	"      \"bone\": \"Arm_Front_Hand\",\n" +
	"      \"prop\": \"ROTATION\",\n" +
	"      \"keys\": [\n" +
	"        { \"t\": 0.0, \"v\": 0.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.2, \"v\": 45.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.4, \"v\": -20.0, \"c\": \"SMOOTH\" },\n" +
	"        { \"t\": 0.8, \"v\": 0.0, \"c\": \"SMOOTH\" }\n" +
	"      ]\n" +
	"    }\n" +
	"  ]\n" +
	"}";

	@Override
	public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

	@Override
	public String getIntroduction() {
		return "JSON 实时编辑器 (v1.7.0)\n左侧编辑 JSON，右侧实时预览\n支持 Hot-Reload (应用按钮)";
	}

	@Override
	public void show() {
		super.show();
		Debug.logT("VisualCheck", "Checking visual center for " + this.getClass().getSimpleName() + " Camera Pos: " + getWorldCamera().position);
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		world.setReferences(getUIViewport(), worldCamera);

		new SkeletonSystem();
		new WorldRenderSystem(neonBatch, getWorldCamera());

		createTestEntity();
		initUI();

		// 初始加载一次
		applyJson();

		// 相机调整 (给左边留出位置)
		//autoCenterWorldCamera = false; // Default is false now
		getWorldCamera().position.set(-150, 0, 0);
		getWorldCamera().update();
	}

	private void createTestEntity() {
		GObject player = new GObject("Player");
		player.transform.setPosition(0, -100);
		player.transform.setScale(1.5f);

		SkeletonComponent skelComp = player.addComponent(SkeletonComponent.class);
		playerAnimator = player.addComponent(NeonAnimatorComponent.class);

		// 使用工厂构建一个基础火柴人
		TestSkeletonFactory.buildStickman(skelComp.getSkeleton());
	}

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		uiStage.addActor(root);

		// --- Layout ---
		// Left: Editor (40% width)
		Table leftPanel = new Table();
		codeEditor = new BioCodeEditor();
		codeEditor.setText(DEFAULT_JSON);

		VisTextButton btnApply = new VisTextButton("APPLY (Run JSON)");
		btnApply.setColor(Color.valueOf("00eaff"));
		btnApply.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					applyJson();
				}
			});

		leftPanel.add(new VisLabel("Animation JSON")).left().row();
		leftPanel.add(codeEditor).grow().padBottom(5).row();
		leftPanel.add(btnApply).growX().height(50);

		root.add(leftPanel).width(getUIViewport().getWorldWidth() * 0.4f).growY().pad(10);

		// Right: Empty (Camera View)
		root.add().grow();

		// Toast
		toast = ToastUI.inst();
		toast.setPosition(getUIViewport().getWorldWidth()/2, 100);
		uiStage.addActor(toast);
	}

	private void applyJson() {
		String jsonStr = codeEditor.getText();
		try {
			// 1. 解析
			NeonAnimation anim = NeonJsonUtils.fromJson(jsonStr);

			// 2. 注册并播放
			playerAnimator.addAnimation(anim);
			playerAnimator.play(anim.name);

			toast.show("Applied: " + anim.name);
			Debug.log("JSON Parsed successfully.");

		} catch (Exception e) {
			String err = "JSON Error: " + e.getMessage();
			toast.show("Error!");
			Debug.log(err);
			e.printStackTrace();
		}
	}

	@Override
	public void render0(float delta) {
		world.update(delta);

		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(-1000, 0, 1000, 0, 2, Color.GRAY);
		neonBatch.end();

		world.render(neonBatch, worldCamera);

		uiStage.act(delta);
		uiStage.draw();
	}

	@Override
	public void dispose() {
		if(world!=null) world.dispose();
		if(neonBatch!=null) neonBatch.dispose();
		if(uiStage!=null) uiStage.dispose();
	}
}
