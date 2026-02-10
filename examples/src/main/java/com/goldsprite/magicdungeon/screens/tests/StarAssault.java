package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.assets.ColorTextureUtils;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.InputAdapter;
import com.kotcrab.vis.ui.VisUI;
import com.badlogic.gdx.scenes.scene2d.ui.Value;

/**
 * Star Assault 1:1 Port
 * Based on Obviam's tutorial source code.
 */
public class StarAssault extends GScreen {

	private World world;
	private BobController controller;
	private WorldRenderer renderer;
	
    // --- 新增 UI 变量 ---
    private Stage uiStage;
    // ------------------

	@Override
	public void create() {
		// 初始化 MVC
		world = new World();
		renderer = new WorldRenderer(world, false); // Debug 设为 false，按 D 键开启
		controller = new BobController(world);

        // 2. 初始化 UI Stage
        uiStage = new Stage(new ExtendViewport(1280, 720));
        createTouchControls(); // 构建按钮
		
		// 注册输入处理器
		if (getImp() != null) {
            getImp().addProcessor(uiStage);
            getImp().addProcessor(controller);
		}
	}
	
	// --- 新增：创建 VisUI 按钮 ---
    private void createTouchControls() {
        // 仅在 Android 下显示，或者为了测试方便一直显示
        if (Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Android) {
            // 如果你想在电脑上也测试按钮，注释掉这行
            // return; 
        }

        VisTable root = new VisTable();
        root.setFillParent(true);
        root.bottom(); // 按钮在底部

        // 创建纯色背景 Drawable (半透明)
        Drawable solidColor = getColoredDrawable(new Color(1f, 1f, 1f, 0.3f)); // 30% 透明度白

        // 样式：背景用 window-noborder (实际是透明的)，但我们通过 setBackground 或 Image 覆盖颜色
        VisTextButton.VisTextButtonStyle style = new VisTextButton.VisTextButtonStyle(VisUI.getSkin().get(VisTextButton.VisTextButtonStyle.class));
        style.up = solidColor; 
        style.down = getColoredDrawable(new Color(0.8f, 0.8f, 0.8f, 0.5f)); // 按下变深

        // 左按钮
        VisTextButton btnLeft = new VisTextButton("LEFT", style);
        btnLeft.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					controller.leftPressed();
					return true;
				}
				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					controller.leftReleased();
				}
			});

        // 右按钮
        VisTextButton btnRight = new VisTextButton("RIGHT", style);
        btnRight.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					controller.rightPressed();
					return true;
				}
				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					controller.rightReleased();
				}
			});

        // 跳跃按钮
        VisTextButton btnJump = new VisTextButton("JUMP", style);
        btnJump.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					controller.jumpPressed();
					return true;
				}
				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					controller.jumpReleased();
				}
			});

        // 布局：左半边给 Left/Right，右半边给 Jump
        // 使用 Table 嵌套
        VisTable leftSide = new VisTable();
        leftSide.add(btnLeft).expand().fill();
        leftSide.add(btnRight).expand().fill();

		float viewWidth = uiStage.getViewport().getWorldWidth();
		float viewHeight = uiStage.getViewport().getWorldHeight();
        // 添加到根布局
        // 左边占宽度的 50%，右边占宽度的 50%，高度占屏幕高度的 25% 或固定值
        root.add(leftSide).size(viewWidth*0.4f, viewHeight*0.3f);
        root.add().growX();
        root.add(btnJump).size(viewWidth*0.3f, viewHeight*0.3f);

        uiStage.addActor(root);
		
        // 如果想看布局调试线，开启这个：
        root.setDebug(true, true);
    }

    // 辅助方法：生成纯色 Drawable
    private Drawable getColoredDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

	@Override
	public void render0(float delta) {
		// 逻辑更新
		controller.update(delta);
		// 渲染
		renderer.render();
		
		uiStage.act(delta);
		uiStage.draw();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		renderer.resize(width, height);
	}

	@Override
	public void dispose() {
		super.dispose();
		// 简单的资源清理
		if (renderer != null)
			renderer.dispose();
	}

	// ========================================================================
	// 1. Model: Block
	// ========================================================================
	public static class Block {
		public static final float SIZE = 1f;

		Vector2 position = new Vector2();
		Rectangle bounds = new Rectangle();

		public Block(Vector2 pos) {
			this.position = pos;
			this.bounds.setX(pos.x);
			this.bounds.setY(pos.y);
			this.bounds.width = SIZE;
			this.bounds.height = SIZE;
		}

		public Vector2 getPosition() {
			return position;
		}
		public Rectangle getBounds() {
			return bounds;
		}
	}

	// ========================================================================
	// 2. Model: Bob
	// ========================================================================
	public static class Bob {
		public enum State {
			IDLE, WALKING, JUMPING, DYING
		}

		public static final float SIZE = 0.5f; // half a unit

		Vector2 position = new Vector2();
		Vector2 acceleration = new Vector2();
		Vector2 velocity = new Vector2();
		Rectangle bounds = new Rectangle();
		State state = State.IDLE;
		boolean facingLeft = true;
		float stateTime = 0;
		boolean longJump = false;

		public Bob(Vector2 position) {
			this.position = position;
			this.bounds.x = position.x;
			this.bounds.y = position.y;
			this.bounds.height = SIZE;
			this.bounds.width = SIZE;
		}

		public void update(float delta) {
			stateTime += delta;
		}

		// Getters and Setters
		public boolean isFacingLeft() {
			return facingLeft;
		}
		public void setFacingLeft(boolean facingLeft) {
			this.facingLeft = facingLeft;
		}
		public Vector2 getPosition() {
			return position;
		}
		public Vector2 getAcceleration() {
			return acceleration;
		}
		public Vector2 getVelocity() {
			return velocity;
		}
		public Rectangle getBounds() {
			return bounds;
		}
		public State getState() {
			return state;
		}
		public void setState(State newState) {
			this.state = newState;
		}
		public float getStateTime() {
			return stateTime;
		}
		public void setStateTime(float stateTime) {
			this.stateTime = stateTime;
		}
		public boolean isLongJump() {
			return longJump;
		}
		public void setLongJump(boolean longJump) {
			this.longJump = longJump;
		}
		public void setPosition(Vector2 position) {
			this.position = position;
			this.bounds.setX(position.x);
			this.bounds.setY(position.y);
		}
	}

	// ========================================================================
	// 3. Model: Level & LevelLoader
	// ========================================================================
	public static class Level {
		private int width;
		private int height;
		private Block[][] blocks;
		private Vector2 spanPosition; // Bob 的出生点

		public Level() {
			// 这里仅仅初始化，具体数据由 World 调用 LevelLoader 填充
		}

		/**
		 * 纯代码生成关卡（备用/调试用）
		 * 原教程 Part 2-3 的逻辑
		 */
		public void loadDemoLevel() {
			width = 10;
			height = 7;
			blocks = new Block[width][height];
			for (int col = 0; col < width; col++) {
				for (int row = 0; row < height; row++) {
					blocks[col][row] = null;
				}
			}

			for (int col = 0; col < 10; col++) {
				blocks[col][0] = new Block(new Vector2(col, 0));
				blocks[col][6] = new Block(new Vector2(col, 6));
				if (col > 2) {
					blocks[col][1] = new Block(new Vector2(col, 1));
				}
			}
			blocks[9][2] = new Block(new Vector2(9, 2));
			blocks[9][3] = new Block(new Vector2(9, 3));
			blocks[9][4] = new Block(new Vector2(9, 4));
			blocks[9][5] = new Block(new Vector2(9, 5));

			blocks[6][3] = new Block(new Vector2(6, 3));
			blocks[6][4] = new Block(new Vector2(6, 4));
			blocks[6][5] = new Block(new Vector2(6, 5));

			// 默认出生点
			spanPosition = new Vector2(1, 2);
		}

		// Getters and Setters
		public int getWidth() {
			return width;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public Block[][] getBlocks() {
			return blocks;
		}
		public void setBlocks(Block[][] blocks) {
			this.blocks = blocks;
		}
		public Block get(int x, int y) {
			return blocks[x][y];
		}
		public Vector2 getSpanPosition() {
			return spanPosition;
		}
		public void setSpanPosition(Vector2 spanPosition) {
			this.spanPosition = spanPosition;
		}
	}

	public static class LevelLoader {
		// 修正：路径改为 bob/levels/
		private static final String LEVEL_PREFIX = "bob/levels/level-";

		private static final int BLOCK = 0x000000; // black
		private static final int START_POS = 0x0000ff; // blue

		public static Level loadLevel(int number) {
			Level level = new Level();

			// 读取 PNG
			String path = LEVEL_PREFIX + number + ".png";
			if (!Gdx.files.internal(path).exists()) {
				Gdx.app.error("LevelLoader", "Level file not found: " + path + ". Loading demo level instead.");
				level.loadDemoLevel();
				return level;
			}

			Pixmap pixmap = new Pixmap(Gdx.files.internal(path));

			level.setWidth(pixmap.getWidth());
			level.setHeight(pixmap.getHeight());

			Block[][] blocks = new Block[level.getWidth()][level.getHeight()];

			for (int col = 0; col < level.getWidth(); col++) {
				for (int row = 0; row < level.getHeight(); row++) {
					blocks[col][row] = null;
				}
			}

			// 遍历像素生成实体
			for (int row = 0; row < level.getHeight(); row++) {
				for (int col = 0; col < level.getWidth(); col++) {
					int rawPixel = pixmap.getPixel(col, row);
					// 2. 提取 Alpha 通道 (低 8 位)
					// 0 = 完全透明, 255 (0xff) = 完全不透明
					int alpha = rawPixel & 0x000000ff;
					int pixel = (rawPixel >>> 8) & 0xffffff;
					
					if(alpha == 0) continue; // 0 为空气
					
					// PNG 坐标 (0,0 在左上) -> World 坐标 (0,0 在左下)
					int iRow = level.getHeight() - 1 - row;

					if (pixel == BLOCK) {
						blocks[col][iRow] = new Block(new Vector2(col, iRow));
					} else if (pixel == START_POS) {
						level.setSpanPosition(new Vector2(col, iRow));
					}
				}
			}

			level.setBlocks(blocks);
			pixmap.dispose(); // 记得释放
			return level;
		}
	}

	// ========================================================================
	// 4. Model: World
	// ========================================================================
	public static class World {
		Bob bob;
		Level level;
		Array<Rectangle> collisionRects = new Array<Rectangle>();

		public World() {
			createWorld();
		}

		private void createWorld() {
			// 使用 LevelLoader 加载 level-1
			level = LevelLoader.loadLevel(1);
			// 如果 PNG 中没有设置出生点，给一个默认值
			Vector2 startPos = level.getSpanPosition();
			if (startPos == null)
				startPos = new Vector2(1, 2);
			bob = new Bob(startPos);
		}

		public Array<Rectangle> getCollisionRects() {
			return collisionRects;
		}
		public Bob getBob() {
			return bob;
		}
		public Level getLevel() {
			return level;
		}

		/** 
		 * 获取视口范围内的 Block，用于剔除渲染 (Culling)
		 * 1:1 还原原逻辑
		 */
		public List<Block> getDrawableBlocks(int width, int height) {
			int x = (int) bob.getPosition().x - width;
			int y = (int) bob.getPosition().y - height;
			if (x < 0)
				x = 0;
			if (y < 0)
				y = 0;

			int x2 = x + 2 * width;
			int y2 = y + 2 * height;

			// --- 修改开始 ---
			// 原代码: if (x2 > level.getWidth()) ...
			// 修复为 >=，确保索引不越界
			if (x2 >= level.getWidth())
				x2 = level.getWidth() - 1;
			if (y2 >= level.getHeight())
				y2 = level.getHeight() - 1;
			// --- 修改结束 ---

			List<Block> blocks = new ArrayList<Block>();
			Block block;
			for (int col = x; col <= x2; col++) {
				for (int row = y; row <= y2; row++) {
					block = level.getBlocks()[col][row];
					if (block != null) {
						blocks.add(block);
					}
				}
			}
			return blocks;
		}
	}

	// ========================================================================
	// 5. Controller: BobController (包含物理与输入)
	// ========================================================================
	public static class BobController extends InputAdapter {

		enum Keys {
			LEFT, RIGHT, JUMP, FIRE
		}

		private static final long LONG_JUMP_PRESS = 150l;
		private static final float ACCELERATION = 20f;
		private static final float GRAVITY = -20f;
		private static final float MAX_JUMP_SPEED = 7f;
		private static final float DAMP = 0.90f;
		private static final float MAX_VEL = 4f;

		private World world;
		private Bob bob;
		private long jumpPressedTime;
		private boolean jumpingPressed;
		private boolean grounded = false;

		private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
			@Override
			protected Rectangle newObject() {
				return new Rectangle();
			}
		};

		static Map<Keys, Boolean> keys = new HashMap<Keys, Boolean>();
		static {
			keys.put(Keys.LEFT, false);
			keys.put(Keys.RIGHT, false);
			keys.put(Keys.JUMP, false);
			keys.put(Keys.FIRE, false);
		};

		private Array<Block> collidable = new Array<Block>();

		public BobController(World world) {
			this.world = world;
			this.bob = world.getBob();
		}

		// --- Input Handling Methods ---
		public void leftPressed() {
			keys.put(Keys.LEFT, true);
		}
		public void rightPressed() {
			keys.put(Keys.RIGHT, true);
		}
		public void jumpPressed() {
			keys.put(Keys.JUMP, true);
		}
		public void firePressed() {
			keys.put(Keys.FIRE, true);
		}

		public void leftReleased() {
			keys.put(Keys.LEFT, false);
		}
		public void rightReleased() {
			keys.put(Keys.RIGHT, false);
		}
		public void jumpReleased() {
			keys.put(Keys.JUMP, false);
			jumpingPressed = false;
		}
		public void fireReleased() {
			keys.put(Keys.FIRE, false);
		}

		// --- Update Loop ---
		public void update(float delta) {
			processInput();

			if (grounded && bob.getState().equals(Bob.State.JUMPING)) {
				bob.setState(Bob.State.IDLE);
			}

			bob.getAcceleration().y = GRAVITY;
			bob.getAcceleration().scl(delta); // mul -> scl
			bob.getVelocity().add(bob.getAcceleration().x, bob.getAcceleration().y);

			checkCollisionWithBlocks(delta);

			bob.getVelocity().x *= DAMP;

			if (bob.getVelocity().x > MAX_VEL)
				bob.getVelocity().x = MAX_VEL;
			if (bob.getVelocity().x < -MAX_VEL)
				bob.getVelocity().x = -MAX_VEL;

			bob.update(delta);
		}

		private void checkCollisionWithBlocks(float delta) {
			bob.getVelocity().scl(delta); // mul -> scl

			Rectangle bobRect = rectPool.obtain();
			bobRect.set(bob.getBounds().x, bob.getBounds().y, bob.getBounds().width, bob.getBounds().height);

			int startX, endX;
			int startY = (int) bob.getBounds().y;
			int endY = (int) (bob.getBounds().y + bob.getBounds().height);

			if (bob.getVelocity().x < 0) {
				startX = endX = (int) Math.floor(bob.getBounds().x + bob.getVelocity().x);
			} else {
				startX = endX = (int) Math.floor(bob.getBounds().x + bob.getBounds().width + bob.getVelocity().x);
			}

			populateCollidableBlocks(startX, startY, endX, endY);

			bobRect.x += bob.getVelocity().x;
			world.getCollisionRects().clear();

			for (Block block : collidable) {
				if (block == null)
					continue;
				if (bobRect.overlaps(block.getBounds())) {
					bob.getVelocity().x = 0;
					world.getCollisionRects().add(block.getBounds());
					break;
				}
			}

			bobRect.x = bob.getPosition().x;

			startX = (int) bob.getBounds().x;
			endX = (int) (bob.getBounds().x + bob.getBounds().width);
			if (bob.getVelocity().y < 0) {
				startY = endY = (int) Math.floor(bob.getBounds().y + bob.getVelocity().y);
			} else {
				startY = endY = (int) Math.floor(bob.getBounds().y + bob.getBounds().height + bob.getVelocity().y);
			}

			populateCollidableBlocks(startX, startY, endX, endY);

			bobRect.y += bob.getVelocity().y;

			for (Block block : collidable) {
				if (block == null)
					continue;
				if (bobRect.overlaps(block.getBounds())) {
					if (bob.getVelocity().y < 0) {
						grounded = true;
					}
					bob.getVelocity().y = 0;
					world.getCollisionRects().add(block.getBounds());
					break;
				}
			}

			bobRect.y = bob.getPosition().y;
			bob.getPosition().add(bob.getVelocity());
			bob.getBounds().x = bob.getPosition().x;
			bob.getBounds().y = bob.getPosition().y;

			bob.getVelocity().scl(1 / delta);
			rectPool.free(bobRect); // 释放
		}

		private void populateCollidableBlocks(int startX, int startY, int endX, int endY) {
			collidable.clear();
			for (int x = startX; x <= endX; x++) {
				for (int y = startY; y <= endY; y++) {
					if (x >= 0 && x < world.getLevel().getWidth() && y >= 0 && y < world.getLevel().getHeight()) {
						collidable.add(world.getLevel().get(x, y));
					}
				}
			}
		}

		private boolean processInput() {
			if (keys.get(Keys.JUMP)) {
				if (!bob.getState().equals(Bob.State.JUMPING)) {
					jumpingPressed = true;
					jumpPressedTime = System.currentTimeMillis();
					bob.setState(Bob.State.JUMPING);
					bob.getVelocity().y = MAX_JUMP_SPEED;
					grounded = false;
				} else {
					if (jumpingPressed && ((System.currentTimeMillis() - jumpPressedTime) >= LONG_JUMP_PRESS)) {
						jumpingPressed = false;
					} else {
						if (jumpingPressed) {
							bob.getVelocity().y = MAX_JUMP_SPEED;
						}
					}
				}
			}
			if (keys.get(Keys.LEFT)) {
				bob.setFacingLeft(true);
				if (!bob.getState().equals(Bob.State.JUMPING)) {
					bob.setState(Bob.State.WALKING);
				}
				bob.getAcceleration().x = -ACCELERATION;
			} else if (keys.get(Keys.RIGHT)) {
				bob.setFacingLeft(false);
				if (!bob.getState().equals(Bob.State.JUMPING)) {
					bob.setState(Bob.State.WALKING);
				}
				bob.getAcceleration().x = ACCELERATION;
			} else {
				if (!bob.getState().equals(Bob.State.JUMPING)) {
					bob.setState(Bob.State.IDLE);
				}
				bob.getAcceleration().x = 0;
			}
			return false;
		}

		// --- InputProcessor Impl (整合自原 GameScreen) ---
		@Override
		public boolean keyDown(int keycode) {
			if (keycode == Input.Keys.LEFT)
				leftPressed();
			if (keycode == Input.Keys.RIGHT)
				rightPressed();
			if (keycode == Input.Keys.Z || keycode == Input.Keys.SPACE)
				jumpPressed();
			if (keycode == Input.Keys.X)
				firePressed();
			return true;
		}
		@Override
		public boolean keyUp(int keycode) {
			if (keycode == Input.Keys.LEFT)
				leftReleased();
			if (keycode == Input.Keys.RIGHT)
				rightReleased();
			if (keycode == Input.Keys.Z || keycode == Input.Keys.SPACE)
				jumpReleased();
			if (keycode == Input.Keys.X)
				fireReleased();
			return true;
		}
	}

	// ========================================================================
	// 6. View: WorldRenderer
	// ========================================================================
	public static class WorldRenderer {
		private static final float CAMERA_WIDTH = 10f;
		private static final float CAMERA_HEIGHT = 7f;
		private static final float RUNNING_FRAME_DURATION = 0.06f;
		// 1. 添加颜色常量和 ShapeRenderer 
		private static final Color TOUCH_COLOR = new Color(1, 1, 1, 0.4f); // 半透明白

		private World world;
		private OrthographicCamera cam;
		private Viewport viewport; // 新增：视口管理

		ShapeRenderer debugRenderer = new ShapeRenderer();

		// Textures
		private TextureRegion bobIdleLeft;
		private TextureRegion bobIdleRight;
		private TextureRegion blockTexture;
		private TextureRegion bobFrame;
		private TextureRegion bobJumpLeft;
		private TextureRegion bobFallLeft;
		private TextureRegion bobJumpRight;
		private TextureRegion bobFallRight;

		// Animations
		private Animation<TextureRegion> walkLeftAnimation;
		private Animation<TextureRegion> walkRightAnimation;

		private SpriteBatch spriteBatch;
		private boolean debug = false;

		public WorldRenderer(World world, boolean debug) {
			this.world = world;
			this.debug = debug;

			// 使用 ExtendViewport 适配屏幕
			this.cam = new OrthographicCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
			this.viewport = new ExtendViewport(CAMERA_WIDTH, CAMERA_HEIGHT, this.cam);

			spriteBatch = new SpriteBatch();
			loadTextures();
		}

		private void loadTextures() {
			// 修正：路径改为 bob/images/textures/textures.pack
			TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("bob/images/textures/textures.pack"));

			bobIdleLeft = atlas.findRegion("bob-01");
			bobIdleRight = new TextureRegion(bobIdleLeft);
			bobIdleRight.flip(true, false);

			blockTexture = atlas.findRegion("block");

			Array<TextureRegion> walkLeftFrames = new Array<TextureRegion>();
			for (int i = 0; i < 5; i++) {
				walkLeftFrames.add(atlas.findRegion("bob-0" + (i + 2)));
			}
			walkLeftAnimation = new Animation<TextureRegion>(RUNNING_FRAME_DURATION, walkLeftFrames);

			Array<TextureRegion> walkRightFrames = new Array<TextureRegion>();
			for (int i = 0; i < 5; i++) {
				TextureRegion frame = new TextureRegion(walkLeftFrames.get(i));
				frame.flip(true, false);
				walkRightFrames.add(frame);
			}
			walkRightAnimation = new Animation<TextureRegion>(RUNNING_FRAME_DURATION, walkRightFrames);

			bobJumpLeft = atlas.findRegion("bob-up"); // 原代码 bob-up，如果pack里是bob-06需要注意
			// 根据 ProjectCode.txt 提供的 textures.txt: bob-06 (jump), bob-up/down 未定义?
			// 原教程 textures.txt 定义了 bob-up 和 bob-down? 
			// 假设 pack 文件里有 bob-up, bob-down。如果没有，请用 bob-05, bob-06 替代
			if (bobJumpLeft == null)
				bobJumpLeft = atlas.findRegion("bob-06"); // Fallback

			bobJumpRight = new TextureRegion(bobJumpLeft);
			bobJumpRight.flip(true, false);

			bobFallLeft = atlas.findRegion("bob-down");
			if (bobFallLeft == null)
				bobFallLeft = atlas.findRegion("bob-05"); // Fallback

			bobFallRight = new TextureRegion(bobFallLeft);
			bobFallRight.flip(true, false);
		}

		public void render() {
			// 跟随 Bob 移动相机
			cam.position.set(world.getBob().getPosition().x, world.getBob().getPosition().y, 0);
			cam.update();

			spriteBatch.setProjectionMatrix(cam.combined);

			// 清屏 (使用 LibGDX 现代写法)
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			spriteBatch.begin();
			drawBlocks();
			drawBob();
			spriteBatch.end();

			// --- 新增：绘制触摸区域 UI ---
			if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
				drawTouchUI();
			}
			// ---------------------------

			if (debug) {
				drawCollisionBlocks();
				drawDebug();
			}
		}

		// 3. 实现 drawTouchUI 方法
		private void drawTouchUI() {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			// 临时切换到屏幕坐标系
			debugRenderer.setProjectionMatrix(viewport.getCamera().combined);

			debugRenderer.begin(ShapeType.Filled);
			debugRenderer.setColor(TOUCH_COLOR);

			float w = viewport.getWorldWidth();
			float h = viewport.getWorldWidth();

			// 绘制左下角 (移动区) - 分割为左右两块
			// 左移区域: 0 ~ 1/4 屏幕宽
			debugRenderer.rect(0, 0, w / 5, h / 3);
			// 右移区域: 1/4 ~ 1/2 屏幕宽
			debugRenderer.rect(w / 5, 0, w / 5, h / 3);

			// 绘制右下角 (跳跃区)
			// 跳跃区域: 1/2 ~ 全屏宽
			debugRenderer.rect(w / 3, 0, w / 3, h / 3);

			debugRenderer.end();
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}

		private void drawBlocks() {
			// 使用 ExtendViewport 不需要乘以 ppuX/Y，直接画世界坐标即可
			for (Block block : world.getDrawableBlocks((int) CAMERA_WIDTH, (int) CAMERA_HEIGHT)) {
				spriteBatch.draw(blockTexture, block.getPosition().x, block.getPosition().y, Block.SIZE, Block.SIZE);
			}
		}

		private void drawBob() {
			Bob bob = world.getBob();
			bobFrame = bob.isFacingLeft() ? bobIdleLeft : bobIdleRight;
			if (bob.getState().equals(Bob.State.WALKING)) {
				bobFrame = bob.isFacingLeft()
						? walkLeftAnimation.getKeyFrame(bob.getStateTime(), true)
						: walkRightAnimation.getKeyFrame(bob.getStateTime(), true);
			} else if (bob.getState().equals(Bob.State.JUMPING)) {
				if (bob.getVelocity().y > 0) {
					bobFrame = bob.isFacingLeft() ? bobJumpLeft : bobJumpRight;
				} else {
					bobFrame = bob.isFacingLeft() ? bobFallLeft : bobFallRight;
				}
			}
			spriteBatch.draw(bobFrame, bob.getPosition().x, bob.getPosition().y, Bob.SIZE, Bob.SIZE);
		}

		private void drawDebug() {
			debugRenderer.setProjectionMatrix(cam.combined);
			debugRenderer.begin(ShapeType.Line);
			for (Block block : world.getDrawableBlocks((int) CAMERA_WIDTH, (int) CAMERA_HEIGHT)) {
				Rectangle rect = block.getBounds();
				debugRenderer.setColor(new Color(1, 0, 0, 1));
				debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
			}
			Bob bob = world.getBob();
			Rectangle rect = bob.getBounds();
			debugRenderer.setColor(new Color(0, 1, 0, 1));
			debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
			debugRenderer.end();
		}

		private void drawCollisionBlocks() {
			debugRenderer.setProjectionMatrix(cam.combined);
			debugRenderer.begin(ShapeType.Filled);
			debugRenderer.setColor(Color.WHITE);
			for (Rectangle rect : world.getCollisionRects()) {
				debugRenderer.rect(rect.x, rect.y, rect.width, rect.height, Color.WHITE, Color.WHITE, Color.WHITE,
						Color.WHITE);
			}
			debugRenderer.end();
		}

		public void resize(int width, int height) {
			viewport.update(width, height);
		}

		public void dispose() {
			spriteBatch.dispose();
			debugRenderer.dispose();
		}
	}
}

