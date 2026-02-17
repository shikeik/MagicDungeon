package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 纹理预览与浏览场景
 * 使用 VisUI ScrollPane 展示所有加载的纹理，支持搜索过滤
 */
public class TexturePreviewScreen extends GScreen {
    private Stage stage;
    private VisTable contentTable;
    private VisTextField filterField;
    private List<Map.Entry<String, TextureRegion>> allEntries;

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        stage = new Stage(getUIViewport());
        if (imp != null) imp.addProcessor(stage);

        // Load Data
        TextureManager tm = TextureManager.getInstance();
        allEntries = new ArrayList<>(tm.getAllTextures().entrySet());
        // Sort by Name
        Collections.sort(allEntries, new Comparator<Map.Entry<String, TextureRegion>>() {
            @Override
            public int compare(Map.Entry<String, TextureRegion> o1, Map.Entry<String, TextureRegion> o2) {
                return o1.getKey().compareToIgnoreCase(o2.getKey());
            }
        });

        buildUI();
        rebuildContent();
    }

    private void buildUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");

        // Top Bar: Title + Search
        VisTable topBar = new VisTable(true);
        topBar.add(new VisLabel("Texture Registry")).padRight(20);
        
        topBar.add(new VisLabel("Search:"));
        filterField = new VisTextField("");
        filterField.setMessageText("Filter by name...");
        filterField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rebuildContent();
            }
        });
        topBar.add(filterField).width(300);
        
        root.add(topBar).pad(10).fillX().row();

        // Content Area
        contentTable = new VisTable();
        contentTable.top().left();
        
        VisScrollPane scroll = new VisScrollPane(contentTable);
        scroll.setFlickScroll(true);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false); // Horizontal disabled, Vertical enabled

        root.add(scroll).expand().fill();

        stage.addActor(root);
    }

    private void rebuildContent() {
        contentTable.clear();
        String filter = filterField.getText().toLowerCase();
        
        // Calculate columns based on screen width approx?
        // Let's assume fixed item width ~150px
        float screenW = stage.getWidth();
        int itemW = 160;
        int maxCols = Math.max(1, (int)(screenW / itemW));
        // Or just use flow layout by row wrap? 
        // VisTable doesn't support automatic flow layout easily without calculation.
        // We stick to manual grid.
        
        int cols = 0;
        
        int count = 0;
        for (Map.Entry<String, TextureRegion> entry : allEntries) {
            String name = entry.getKey();
            if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) continue;
            
            TextureRegion reg = entry.getValue();
            
            // Create Cell
            VisTable cell = new VisTable();
            cell.setBackground("button"); // Frame
            
            // Image (Max 128x128)
            VisImage img = new VisImage(new TextureRegionDrawable(reg));
            img.setScaling(Scaling.fit);
            cell.add(img).size(128, 128).pad(5).row();
            
            // Label
            String dims = reg.getRegionWidth() + "x" + reg.getRegionHeight();
            VisLabel lbl = new VisLabel(name + "\n" + dims);
            lbl.setAlignment(Align.center);
            lbl.setWrap(true);
            lbl.setFontScale(0.8f); // Slightly smaller font
            
            cell.add(lbl).width(130).pad(5).growY();
            
            contentTable.add(cell).width(150).pad(5).top();
            
            cols++;
            count++;
            if (cols >= maxCols) {
                contentTable.row();
                cols = 0;
            }
        }
        
        // Fill empty cells to keep alignment if needed? Not necessary for top-left alignment.
        if (count == 0) {
            contentTable.add(new VisLabel("No textures found matching filter.")).pad(20);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        rebuildContent(); // Re-layout for new width
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
