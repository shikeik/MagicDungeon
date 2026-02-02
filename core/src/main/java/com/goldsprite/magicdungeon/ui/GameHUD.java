package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.Player;
import java.util.LinkedList;

public class GameHUD {
    public Stage stage;
    private Skin skin;
    private Label hpLabel;
    private Label manaLabel;
    private Label xpLabel;
    private Label lvlLabel;
    private Label floorLabel;
    private Label msgLabel;

    private LinkedList<String> logMessages = new LinkedList<>();

    private Window inventoryWindow;
    private Table inventoryList;

    public GameHUD(FitViewport viewport) {
        stage = new Stage(viewport);

        createSkin();

        Table table = new Table();
        table.top().left();
        table.setFillParent(true);

        hpLabel = new Label("HP: 100/100", skin);
        manaLabel = new Label("MP: 50/50", skin);
        xpLabel = new Label("XP: 0/100", skin);
        lvlLabel = new Label("LVL: 1", skin);
        floorLabel = new Label("Floor: 1", skin);
        msgLabel = new Label("", skin);
        showMessage("Welcome to Dungeon!");

        table.add(hpLabel).pad(5).left();
        table.add(manaLabel).pad(5).left();
        table.add(xpLabel).pad(5).left();
        table.add(lvlLabel).pad(5).left();
        table.add(floorLabel).pad(5).left().row();
        table.add(msgLabel).pad(5).left().colspan(5);

        stage.addActor(table);

        createInventoryWindow();
    }

    private void createSkin() {
        skin = new Skin();

        // Generate a simple font
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // Generate a 1x1 white texture and store it in the skin named "white"
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        // Generate a simple label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Window Style
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = Color.YELLOW;
        windowStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", windowStyle);
    }

    private void createInventoryWindow() {
        inventoryWindow = new Window("Inventory", skin);
        inventoryWindow.setSize(300, 400);
        inventoryWindow.setPosition(stage.getWidth() / 2 - 150, stage.getHeight() / 2 - 200);
        inventoryWindow.setMovable(true);
        inventoryWindow.setVisible(false);

        inventoryList = new Table();
        inventoryList.top().left();

        inventoryWindow.add(inventoryList).expand().fill().top().left();

        stage.addActor(inventoryWindow);
    }

    public void toggleInventory() {
        inventoryWindow.setVisible(!inventoryWindow.isVisible());
    }

    public void update(Player player, int dungeonLevel) {
        hpLabel.setText("HP: " + player.stats.hp + "/" + player.stats.maxHp);
        manaLabel.setText("MP: " + player.stats.mana + "/" + player.stats.maxMana);
        xpLabel.setText("XP: " + player.stats.xp + "/" + player.stats.xpToNextLevel);
        lvlLabel.setText("LVL: " + player.stats.level);
        floorLabel.setText("Floor: " + dungeonLevel);

        if (inventoryWindow.isVisible()) {
            inventoryList.clear();
            if (player.inventory.isEmpty()) {
                inventoryList.add(new Label("Empty", skin)).pad(5).left();
            } else {
                for (ItemData item : player.inventory) {
                    Label itemLabel = new Label(item.name + " (" + item.type + ")", skin);
                    itemLabel.setColor(item.color);
                    inventoryList.add(itemLabel).pad(2).left().row();
                }
            }
        }
    }

    public void showMessage(String msg) {
        logMessages.addFirst(msg);
        if (logMessages.size() > 10) {
            logMessages.removeLast();
        }

        StringBuilder sb = new StringBuilder();
        for (String s : logMessages) {
            sb.append(s).append("\n");
        }
        msgLabel.setText(sb.toString());
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
