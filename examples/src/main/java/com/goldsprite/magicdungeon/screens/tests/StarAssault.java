package com.goldsprite.magicdungeon.screens.tests; // 你的包名，请根据项目实际情况修改

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/**
 * Star Assault 复刻版 - 单文件全功能版 (LibGDX 1.12.1)
 * 包含：核心物理、MVC结构、双端操作支持、内存纹理生成
 */
public class StarAssault extends GScreen {

    // --- 全局常量 ---
    public static final float CAMERA_WIDTH = 10f;  // 视口宽度（米）
    public static final float CAMERA_HEIGHT = 7f; // 视口高度（米）

    // --- 核心成员变量 ---
    private World world;
    private WorldController controller;
    private WorldRenderer renderer;

    @Override
    public void create() {
        // 1. 加载/生成资源
        Assets.load();
        
        // 2. 初始化 MVC
        world = new World();
        controller = new WorldController(world);
        renderer = new WorldRenderer(world);
    }

    @Override
    public void render0(float delta) {
        // 更新逻辑 (使用 Gdx.graphics.getDeltaTime())
        controller.update(Gdx.graphics.getDeltaTime());
        
        // 渲染画面
        renderer.render();
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        Assets.dispose();
    }

    // ========================================================================
    // 1. Model (模型层) - 数据定义
    // ========================================================================

    public static class Bob {
        public enum State { IDLE, WALKING, JUMPING, DYING }

        public static final float SIZE_W = 0.5f; // 半米宽
        public static final float SIZE_H = 0.8f; // 接近一米高

        public Vector2 position = new Vector2();
        public Vector2 velocity = new Vector2();
        public Vector2 acceleration = new Vector2();
        public Rectangle bounds = new Rectangle();
        public State state = State.IDLE;
        public float stateTime = 0;
        public boolean facingLeft = false;

        public Bob(Vector2 pos) {
            this.position = pos;
            this.bounds.width = SIZE_W;
            this.bounds.height = SIZE_H;
        }

        public void update(float delta) {
            stateTime += delta;
            bounds.setPosition(position);
        }
    }

    public static class Block {
        public static final float SIZE = 1f;
        public Rectangle bounds = new Rectangle();
        public Vector2 position = new Vector2();

        public Block(Vector2 pos) {
            this.position = pos;
            this.bounds.set(pos.x, pos.y, SIZE, SIZE);
        }
    }

    public static class World {
        public Bob bob;
        public Array<Block> blocks = new Array<>();

        public World() {
            createDemoLevel();
        }

        private void createDemoLevel() {
            bob = new Bob(new Vector2(7, 2));

            // 地板
            for (int i = 0; i < 10; i++) {
                blocks.add(new Block(new Vector2(i, 0)));
                blocks.add(new Block(new Vector2(i, 6))); // 天花板
            }
            // 墙壁和台阶
            blocks.add(new Block(new Vector2(9, 1)));
            blocks.add(new Block(new Vector2(9, 2)));
            blocks.add(new Block(new Vector2(9, 3)));
            blocks.add(new Block(new Vector2(6, 3)));
            blocks.add(new Block(new Vector2(6, 4)));
            blocks.add(new Block(new Vector2(6, 5)));
            
            blocks.add(new Block(new Vector2(0, 1)));
            blocks.add(new Block(new Vector2(0, 2)));
            blocks.add(new Block(new Vector2(0, 3)));
        }
    }

    // ========================================================================
    // 2. Controller (控制层) - 物理与输入
    // ========================================================================

    public static class WorldController {
        private World world;
        private Bob bob;

        // 物理参数
        private static final float MAX_VELOCITY = 4f;
        private static final float DAMP = 0.87f; // 阻尼
        private static final float ACCELERATION = 20f;
        private static final float GRAVITY = -22f;
        private static final float JUMP_VELOCITY = 9f;

        // 输入状态
        private boolean leftPressed, rightPressed, jumpPressed;

        // 触摸区域矩形 (归一化 0-1)
        private Rectangle touchLeft = new Rectangle(0, 0, 0.25f, 0.5f);
        private Rectangle touchRight = new Rectangle(0.25f, 0, 0.25f, 0.5f);
        
        public WorldController(World world) {
            this.world = world;
            this.bob = world.bob;
        }

        public void update(float delta) {
            processInput();
            
            // 应用重力
            bob.acceleration.y = GRAVITY;
            
            // 物理计算
            bob.acceleration.scl(delta);
            bob.velocity.add(bob.acceleration);
            
            // 阻尼
            if (bob.state != Bob.State.JUMPING && !leftPressed && !rightPressed) {
                bob.velocity.x *= DAMP;
            }

            // 限制速度
            if (bob.velocity.x > MAX_VELOCITY) bob.velocity.x = MAX_VELOCITY;
            if (bob.velocity.x < -MAX_VELOCITY) bob.velocity.x = -MAX_VELOCITY;

            // 核心更新
            bob.update(delta);
            
            // 碰撞检测 (先X后Y)
            bob.position.x += bob.velocity.x * delta;
            bob.bounds.x = bob.position.x;
            checkCollisionX();

            bob.position.y += bob.velocity.y * delta;
            bob.bounds.y = bob.position.y;
            checkCollisionY();
        }

        private void processInput() {
            // 1. 重置按键状态
            leftPressed = false;
            rightPressed = false;
            jumpPressed = false;

            // 2. 键盘输入 (Desktop)
            if (Gdx.input.isKeyPressed(Keys.LEFT)) leftPressed = true;
            if (Gdx.input.isKeyPressed(Keys.RIGHT)) rightPressed = true;
            if (Gdx.input.isKeyPressed(Keys.SPACE)) jumpPressed = true;
            if (Gdx.input.isKeyPressed(Keys.Z)) jumpPressed = true;

            // 3. 触摸输入 (Android)
            // 遍历所有触摸点 (支持多点触控)
            for (int i = 0; i < 5; i++) {
                if (Gdx.input.isTouched(i)) {
                    float x = (float)Gdx.input.getX(i) / Gdx.graphics.getWidth();
                    float y = (float)Gdx.input.getY(i) / Gdx.graphics.getHeight();
                    // y坐标在 input 中通常是反的，但在归一化逻辑里我们只看区域
                    // 修正：Gdx input y是向下增长，我们用 1-y 翻转或者直接判断
                    
                    // 简单粗暴的区域判断
                    if (x < 0.2f) leftPressed = true; // 屏幕左边20%区域
                    else if (x > 0.2f && x < 0.4f) rightPressed = true; // 20%-40%区域
                    else jumpPressed = true; // 其他区域都是跳
                }
            }

            // 4. 应用输入到 Bob
            if (leftPressed) {
                bob.facingLeft = true;
                bob.state = Bob.State.WALKING;
                bob.velocity.x = -MAX_VELOCITY;
            } else if (rightPressed) {
                bob.facingLeft = false;
                bob.state = Bob.State.WALKING;
                bob.velocity.x = MAX_VELOCITY;
            } else {
                if (bob.state != Bob.State.JUMPING) {
                    bob.state = Bob.State.IDLE;
                }
            }

            if (jumpPressed && bob.state != Bob.State.JUMPING) {
                bob.state = Bob.State.JUMPING;
                bob.velocity.y = JUMP_VELOCITY;
            }
        }

        private void checkCollisionX() {
            for (Block block : world.blocks) {
                if (bob.bounds.overlaps(block.bounds)) {
                    if (bob.velocity.x > 0) bob.position.x = block.bounds.x - bob.bounds.width;
                    else bob.position.x = block.bounds.x + block.bounds.width;
                    bob.velocity.x = 0;
                    bob.bounds.x = bob.position.x;
                    break; 
                }
            }
        }

        private void checkCollisionY() {
            for (Block block : world.blocks) {
                if (bob.bounds.overlaps(block.bounds)) {
                    if (bob.velocity.y > 0) {
                        bob.position.y = block.bounds.y - bob.bounds.height;
                        bob.velocity.y = 0; // 撞头
                    } else {
                        bob.position.y = block.bounds.y + block.bounds.height;
                        bob.velocity.y = 0;
                        bob.state = Bob.State.IDLE; // 落地
                    }
                    bob.bounds.y = bob.position.y;
                    break;
                }
            }
        }
    }

    // ========================================================================
    // 3. Renderer (视图层) - 绘制
    // ========================================================================

    public static class WorldRenderer {
        private World world;
        private OrthographicCamera cam;
        private Viewport viewport;
        private SpriteBatch batch;
        private ShapeRenderer debugRenderer; // 用于绘制安卓按键提示

        public WorldRenderer(World world) {
            this.world = world;
            // 使用 FitViewport 保持比例
            cam = new OrthographicCamera();
            viewport = new ExtendViewport(CAMERA_WIDTH, CAMERA_HEIGHT, cam);
            viewport.apply();
            
            cam.position.set(CAMERA_WIDTH / 2f, CAMERA_HEIGHT / 2f, 0);
            batch = new SpriteBatch();
            debugRenderer = new ShapeRenderer();
        }

        public void render() {
            cam.update();
            batch.setProjectionMatrix(cam.combined);
            
            ScreenUtils.clear(0, 0, 0, 1);

            batch.begin();
            // 1. 绘制方块
            for (Block block : world.blocks) {
                batch.draw(Assets.blockTexture, block.position.x, block.position.y, Block.SIZE, Block.SIZE);
            }

            // 2. 绘制 Bob (根据状态选择帧)
            TextureRegion currentFrame = null;
            Bob bob = world.bob;

            switch (bob.state) {
                case IDLE: currentFrame = Assets.bobIdle.getKeyFrame(bob.stateTime); break;
                case WALKING: currentFrame = Assets.bobWalk.getKeyFrame(bob.stateTime); break;
                case JUMPING: currentFrame = Assets.bobJump.getKeyFrame(bob.stateTime); break;
                default: currentFrame = Assets.bobIdle.getKeyFrame(bob.stateTime); break;
            }

            // 处理左右翻转
            if (bob.facingLeft && !currentFrame.isFlipX()) {
                currentFrame.flip(true, false);
            } else if (!bob.facingLeft && currentFrame.isFlipX()) {
                currentFrame.flip(true, false);
            }

            batch.draw(currentFrame, bob.position.x, bob.position.y, Bob.SIZE_W, Bob.SIZE_H);
            batch.end();

            // 3. 绘制UI提示 (半透明层)
            // 切换到屏幕坐标系
            debugRenderer.setProjectionMatrix(batch.getProjectionMatrix().cpy().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            
            debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
            debugRenderer.setColor(1, 1, 1, 0.1f); // 极淡的白色
            
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            
            // 左按钮区域
            debugRenderer.rect(0, 0, w * 0.2f, h * 0.3f);
            // 右按钮区域
            debugRenderer.rect(w * 0.2f, 0, w * 0.2f, h * 0.3f);
            // 跳跃提示
            debugRenderer.setColor(1, 1, 1, 0.05f);
            debugRenderer.rect(w * 0.6f, 0, w * 0.4f, h * 0.5f);
            
            debugRenderer.end();
        }

        public void resize(int width, int height) {
            viewport.update(width, height);
            cam.position.set(CAMERA_WIDTH / 2f, CAMERA_HEIGHT / 2f, 0);
        }

        public void dispose() {
            batch.dispose();
            debugRenderer.dispose();
        }
    }

    // ========================================================================
    // 4. Assets (资源层) - 内存中生成像素素材
    // ========================================================================

    public static class Assets {
        public static Texture atlasTexture;
        public static TextureRegion blockTexture;
        public static Animation<TextureRegion> bobIdle;
        public static Animation<TextureRegion> bobWalk;
        public static Animation<TextureRegion> bobJump;

        public static void load() {
            // 动态生成一个 128x128 的纹理，模仿原来的 sprites.png
            Pixmap pixmap = new Pixmap(128, 64, Pixmap.Format.RGBA8888);

            // --- 1. 画 Block (黄色方框) ---
            // 对应原 textures.txt: xy: 1, 11, size: 48, 48
            pixmap.setColor(Color.YELLOW);
            pixmap.drawRectangle(1, 11, 48, 48); // 外框
            pixmap.drawRectangle(2, 12, 46, 46); // 加粗一点
            // 中间点缀
            pixmap.setColor(Color.WHITE); 
            pixmap.fillRectangle(1, 11, 4, 4); // 角落装饰
            pixmap.fillRectangle(45, 11, 4, 4);
            pixmap.fillRectangle(1, 55, 4, 4);
            pixmap.fillRectangle(45, 55, 4, 4);

            // --- 2. 画 Bob (绿色像素人) ---
            // 为了简单，我们手动画几个简易帧
            pixmap.setColor(Color.GREEN);
            
            // Frame 1: Idle (xy: 51, 31, 24x28) - 站立
            drawBobFrame(pixmap, 51, 31, false);

            // Frame 2: Walk 1 (xy: 77, 31) - 迈腿
            drawBobFrame(pixmap, 77, 31, true);

            // Frame 3: Walk 2 (xy: 103, 31) - 迈另一条腿
            drawBobFrame(pixmap, 103, 31, false); 
            // 稍微改一点表示动态
            pixmap.setColor(Color.BLACK); 
            pixmap.fillRectangle(103+5, 31+20, 4, 8); 
            pixmap.setColor(Color.GREEN);

            // Frame 4: Jump (xy: 103, 1) - 跳跃姿势
            drawBobFrame(pixmap, 103, 1, true);

            // 生成 Texture
            atlasTexture = new Texture(pixmap);
            pixmap.dispose();

            // --- 3. 切割 Region (根据 textures.txt 的大致位置) ---
            blockTexture = new TextureRegion(atlasTexture, 1, 11, 48, 48);

            // 提取 Bob 帧
            TextureRegion idleFrame = new TextureRegion(atlasTexture, 51, 31, 24, 28);
            TextureRegion walk1 = new TextureRegion(atlasTexture, 77, 31, 24, 28);
            TextureRegion walk2 = new TextureRegion(atlasTexture, 103, 31, 24, 28);
            TextureRegion jumpFrame = new TextureRegion(atlasTexture, 103, 1, 24, 28);

            // --- 4. 创建动画 ---
            bobIdle = new Animation<>(0.2f, idleFrame);
            bobWalk = new Animation<>(0.15f, walk1, idleFrame, walk2, idleFrame);
            bobWalk.setPlayMode(Animation.PlayMode.LOOP);
            bobJump = new Animation<>(0.2f, jumpFrame);
        }

        // 辅助方法：画一个简单的火柴人 Bob
        private static void drawBobFrame(Pixmap p, int x, int y, int w, int h) {
             p.fillRectangle(x + 6, y, 12, 12); // 头
             p.fillRectangle(x + 8, y + 12, 8, 10); // 身体
             p.fillRectangle(x + 2, y + 12, 6, 6); // 左手
             p.fillRectangle(x + 16, y + 12, 6, 6); // 右手
             p.fillRectangle(x + 6, y + 22, 4, 6); // 左脚
             p.fillRectangle(x + 14, y + 22, 4, 6); // 右脚
        }
        
        // 简单画法封装
        private static void drawBobFrame(Pixmap p, int x, int y, boolean legUp) {
            // 坐标系在 Pixmap 里是左上角为0
            // 头
            p.fillRectangle(x + 6, y, 12, 10); 
            // 身
            p.fillRectangle(x + 8, y + 10, 8, 10);
            // 手
            p.fillRectangle(x + 4, y + 10, 4, 8);
            p.fillRectangle(x + 16, y + 10, 4, 8);
            // 脚
            if (legUp) {
                p.fillRectangle(x + 4, y + 20, 6, 4); // 抬脚
                p.fillRectangle(x + 14, y + 20, 4, 8); // 站立脚
            } else {
                p.fillRectangle(x + 6, y + 20, 4, 8);
                p.fillRectangle(x + 14, y + 20, 4, 8);
            }
        }

        public static void dispose() {
            if (atlasTexture != null) atlasTexture.dispose();
        }
    }
}
