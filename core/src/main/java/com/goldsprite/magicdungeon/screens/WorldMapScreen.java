package com.goldsprite.magicdungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon.core.screens.MainMenuScreen;
import com.goldsprite.magicdungeon.core.screens.GameScreen;
import com.goldsprite.magicdungeon.world.DungeonTheme;

import java.util.function.Consumer;

public class WorldMapScreen extends GScreen {

    // [新增] 调试配置：临时解锁所有区域
    public static boolean DEBUG_UNLOCK_ALL = false;

    private NeonBatch neonBatch;
    private SpriteBatch batch;
    private BitmapFont font;
    private Array<DungeonNode> nodes = new Array<>();
    private DungeonNode hoveredNode = null;
    private float[] backgroundDecorations;
    
    private Consumer<DungeonNode> onNodeSelected;

    // 羊皮卷背景色
    private final Color PAPER_COLOR = Color.valueOf("#f4e4bc");
    private final Color INK_COLOR = Color.valueOf("#4e342e");

    public WorldMapScreen() {
        this(null);
    }

    public WorldMapScreen(Consumer<DungeonNode> onNodeSelected) {
        this.onNodeSelected = onNodeSelected;
    }

    
    @Override
    public void create() {
        neonBatch = new NeonBatch();
        batch = new SpriteBatch();
        font = VisUIHelper.cnFont;
        if (font == null) {
            // Fallback if not initialized (though it should be)
            font = new BitmapFont();
        }
        
        // 初始化地牢节点
        initNodes();
        
        // 输入处理
        getImp().addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT && hoveredNode != null) {
                    enterDungeon(hoveredNode);
                    return true;
                }
                return false;
            }
        });
    }
    
    private void initNodes() {
        // 1. 初始营地 (Camp) - 中心
        DungeonNode camp = new DungeonNode("camp", "幸存者营地", 0.5f, 0.5f, Color.valueOf("#8d6e63"), 1, 1);
        camp.unlocked = true;
        // 简单的五边形区域
        camp.setRegion(new float[]{
            -50, -40,
            50, -40,
            60, 20,
            0, 60,
            -60, 20
        });
        nodes.add(camp);
        
        // 2. 迷雾森林 (Forest) - 左上
        DungeonNode forest = new DungeonNode("forest", "迷雾森林", 0.25f, 0.7f, Color.valueOf("#2e7d32"), 3, 10);
        forest.theme = DungeonTheme.FOREST;
        forest.unlocked = true;
        forest.setRegion(new float[]{
            -70, -60,
            60, -40,
            80, 50,
            -20, 80,
            -90, 20
        });
        nodes.add(forest);
        
        // 3. 灼热沙漠 (Desert) - 右下
        DungeonNode desert = new DungeonNode("desert", "灼热沙漠", 0.75f, 0.3f, Color.valueOf("#fbc02d"), 10, 20);
        desert.theme = DungeonTheme.DESERT;
        desert.unlocked = false; // 假设未解锁
        desert.setRegion(new float[]{
            -60, -50,
            70, -70,
            90, 20,
            20, 60,
            -80, 40
        });
        nodes.add(desert);
        
        // 4. 深渊城堡 (Castle) - 顶部
        DungeonNode castle = new DungeonNode("castle", "深渊城堡", 0.5f, 0.85f, Color.valueOf("#4527a0"), 20, 40);
        castle.theme = DungeonTheme.CASTLE;
        castle.unlocked = false;
        castle.setRegion(new float[]{
            -50, -50,
            50, -50,
            50, 50,
            0, 80,
            -50, 50
        });
        nodes.add(castle);
        
        // 生成背景装饰 (随机曲线，模拟地形纹理)
        backgroundDecorations = new float[200]; // 50 lines * 4 coords
        for(int i=0; i<50; i++) {
            backgroundDecorations[i*4] = MathUtils.random(); // x1
            backgroundDecorations[i*4+1] = MathUtils.random(); // y1
            backgroundDecorations[i*4+2] = MathUtils.random(); // x2
            backgroundDecorations[i*4+3] = MathUtils.random(); // y2
        }
    }
    
    @Override
    public void render(float delta) {
        // 1. 清屏 (使用深色背景衬托羊皮卷)
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        
        // 更新鼠标悬浮状态
        updateHover();
        
        float w = getUIViewport().getWorldWidth();
        float h = getUIViewport().getWorldHeight();
        
        neonBatch.setProjectionMatrix(getUIViewport().getCamera().combined);
        neonBatch.begin();
        
        // 2. 绘制羊皮卷背景
        // 留出边缘
        float pad = 40;
        float mapX = pad;
        float mapY = pad;
        float mapW = w - pad * 2;
        float mapH = h - pad * 2;
        
        // 绘制纸张底色
        neonBatch.drawRect(mapX, mapY, mapW, mapH, 0, 0, PAPER_COLOR, true);
        // 绘制纸张边框
        neonBatch.drawRect(mapX, mapY, mapW, mapH, 0, 4, INK_COLOR, false);
        
        // 绘制装饰性网格线 (经纬线)
        Color gridColor = INK_COLOR.cpy();
        gridColor.a = 0.1f;
        for(int i=1; i<10; i++) {
            float x = mapX + (mapW / 10) * i;
            neonBatch.drawLine(x, mapY, x, mapY + mapH, 1, gridColor);
        }
        for(int i=1; i<8; i++) {
            float y = mapY + (mapH / 8) * i;
            neonBatch.drawLine(mapX, y, mapX + mapW, y, 1, gridColor);
        }
        
        // 绘制随机背景纹理 (模拟山脉/河流等地形)
        Color decorColor = INK_COLOR.cpy();
        decorColor.a = 0.05f;
        if (backgroundDecorations != null) {
            for(int i=0; i<50; i++) {
                float x1 = mapX + backgroundDecorations[i*4] * mapW;
                float y1 = mapY + backgroundDecorations[i*4+1] * mapH;
                float x2 = mapX + backgroundDecorations[i*4+2] * mapW;
                float y2 = mapY + backgroundDecorations[i*4+3] * mapH;
                // 简单的贝塞尔曲线
                float cx = (x1+x2)/2;
                float cy = (y1+y2)/2;
                // 简单的偏移，不保存状态的话每帧都会跳动，所以这里我们只画直线或者需要在 init 里生成控制点
                // 为了简单且静态，我们直接画直线，或者用固定的偏移计算
                // neonBatch.drawLine(x1, y1, x2, y2, 1, decorColor);
                
                // 使用基于坐标的伪随机偏移来保持静态
                float seed = x1 + y1;
                float offX = MathUtils.sin(seed) * 20;
                float offY = MathUtils.cos(seed) * 20;
                neonBatch.drawQuadraticBezier(x1, y1, cx + offX, cy + offY, x2, y2, 1, decorColor, 5);
            }
        }

        // 3. 绘制连接线 (路径)
        // 简单起见，从 Camp 连接到其他所有点
        DungeonNode camp = nodes.first(); // 假设第一个是 Camp
        Color pathColor = INK_COLOR.cpy();
        pathColor.a = 0.5f;
        for (int i = 1; i < nodes.size; i++) {
            DungeonNode target = nodes.get(i);
            float x1 = mapX + camp.x * mapW;
            float y1 = mapY + camp.y * mapH;
            float x2 = mapX + target.x * mapW;
            float y2 = mapY + target.y * mapH;
            
            // 绘制虚线路径 (用多个短线段模拟)
            drawDashedLine(x1, y1, x2, y2, pathColor);
        }
        
        // 4. 绘制区域节点
        for (DungeonNode node : nodes) {
            float cx = mapX + node.x * mapW;
            float cy = mapY + node.y * mapH;
            
            // 转换相对顶点到屏幕坐标
            float[] screenVerts = new float[node.regionVertices.length];
            for(int i=0; i<node.regionVertices.length; i+=2) {
                screenVerts[i] = cx + node.regionVertices[i];
                screenVerts[i+1] = cy + node.regionVertices[i+1];
            }
            
            // 填充颜色 (根据解锁状态)
            Color fillColor = node.unlocked ? node.regionColor.cpy() : Color.GRAY.cpy();
            fillColor.a = 0.6f;
            neonBatch.drawPolygon(screenVerts, 0, fillColor, true);
            
            // 描边 (默认)
            neonBatch.drawPolygon(screenVerts, 2, INK_COLOR, false);
            
            // 悬浮高亮 (Neon Effect)
            if (node == hoveredNode) {
                // 发光外边框
                Color glowColor = Color.GOLD.cpy();
                glowColor.a = 0.8f + 0.2f * MathUtils.sin(Gdx.graphics.getFrameId() * 0.1f);
                neonBatch.drawPolygon(screenVerts, 4, glowColor, false);
                
                // 内部稍微亮一点
                neonBatch.drawPolygon(screenVerts, 0, new Color(1, 1, 1, 0.2f), true);
            }
            
            // 绘制中心点
            neonBatch.drawCircle(cx, cy, 5, 0, INK_COLOR, 16, true);
            
            // [新增] 绘制锁图标 (如果未解锁)
            if (!node.unlocked && !DEBUG_UNLOCK_ALL) {
                drawLockIcon(cx, cy);
            }
        }
        
        neonBatch.end();
        
        // 5. 绘制文字信息
        batch.setProjectionMatrix(getUIViewport().getCamera().combined);
        batch.begin();
        for (DungeonNode node : nodes) {
            float cx = mapX + node.x * mapW;
            float cy = mapY + node.y * mapH;
            
            // Draw name below the node
            GlyphLayout layout = new GlyphLayout(font, node.name);
            font.setColor(INK_COLOR);
            font.draw(batch, node.name, cx - layout.width / 2, cy - 30);
            
            // Draw level range
            String lvText = "Lv." + node.minLv + "-" + node.maxLv;
            // Use small font if available, else use main font scaled
            BitmapFont smFont = VisUIHelper.cnFontSmall != null ? VisUIHelper.cnFontSmall : font;
            GlyphLayout lvLayout = new GlyphLayout(smFont, lvText);
            smFont.setColor(INK_COLOR);
            smFont.draw(batch, lvText, cx - lvLayout.width / 2, cy - 50);
        }
        batch.end();
    }
    
    private void drawDashedLine(float x1, float y1, float x2, float y2, Color color) {
        float dist = Vector2.dst(x1, y1, x2, y2);
        float dashLen = 10;
        float gapLen = 5;
        float steps = dist / (dashLen + gapLen);
        
        float dx = (x2 - x1) / dist;
        float dy = (y2 - y1) / dist;
        
        for(int i=0; i<steps; i++) {
            float start = i * (dashLen + gapLen);
            float end = start + dashLen;
            if (end > dist) end = dist;
            
            float sx = x1 + dx * start;
            float sy = y1 + dy * start;
            float ex = x1 + dx * end;
            float ey = y1 + dy * end;
            
            neonBatch.drawLine(sx, sy, ex, ey, 2, color);
        }
    }
    
    private void drawLockIcon(float cx, float cy) {
        // 简单的锁形状
        float w = 16;
        float h = 12;
        float y = cy - 5;
        
        // 锁身 (矩形)
        neonBatch.drawRect(cx - w/2, y, w, h, 0, 0, Color.BLACK, true);
        
        // 锁梁 (半圆环)
        float r = 6;
        float ly = y + h;
        neonBatch.drawArc(cx, ly, r, 0, 180, 2, Color.BLACK, 16);
        
        // 锁孔
        neonBatch.drawCircle(cx, y + h/2, 2, 0, Color.WHITE, 8, true);
    }

    private void updateHover() {
        Vector2 mouse = getUIViewport().unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        hoveredNode = null;
        
        float w = getUIViewport().getWorldWidth();
        float h = getUIViewport().getWorldHeight();
        float pad = 40;
        float mapX = pad;
        float mapY = pad;
        float mapW = w - pad * 2;
        float mapH = h - pad * 2;

        for (DungeonNode node : nodes) {
            float cx = mapX + node.x * mapW;
            float cy = mapY + node.y * mapH;
            
            float[] screenVerts = new float[node.regionVertices.length];
            for(int i=0; i<node.regionVertices.length; i+=2) {
                screenVerts[i] = cx + node.regionVertices[i];
                screenVerts[i+1] = cy + node.regionVertices[i+1];
            }
            Polygon poly = new Polygon(screenVerts);
            if (poly.contains(mouse.x, mouse.y)) {
                hoveredNode = node;
                break; // 只选中最上面的一个
            }
        }
    }
    
    private void enterDungeon(DungeonNode node) {
        if (!node.unlocked && !DEBUG_UNLOCK_ALL) {
            DLog.log("该区域尚未解锁: " + node.name);
            return;
        }
        
        DLog.log("选择区域: " + node.name + " (Lv." + node.minLv + "-" + node.maxLv + ")");
        
        if (onNodeSelected != null) {
            onNodeSelected.accept(node);
        } else {
            // Fallback: 默认行为 (如果需要独立运行)
            // 生成随机种子
            long seed = (long)(Math.random() * Long.MAX_VALUE);
            
            if (screenManager != null) {
                screenManager.playTransition(() -> {
                    GameScreen gameScreen = new GameScreen(seed);
                    // TODO: 根据 node.id 设置地牢类型/难度
                    screenManager.turnScreen(gameScreen);
                });
            }
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (neonBatch != null) neonBatch.dispose();
        if (batch != null) batch.dispose();
    }
    
    // --- 内部类 ---
    public static class DungeonNode {
        public String id;
        public String name;
        public float x, y; // 0-1 相对坐标
        public Color regionColor;
        public float[] regionVertices; // 相对坐标多边形
        public boolean unlocked;
        public int minLv, maxLv;
        public DungeonTheme theme = DungeonTheme.DEFAULT;
        
        public DungeonNode(String id, String name, float x, float y, Color color, int minLv, int maxLv) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.regionColor = color;
            this.minLv = minLv;
            this.maxLv = maxLv;
        }
        
        public void setRegion(float[] vertices) {
            this.regionVertices = vertices;
        }
    }
}
