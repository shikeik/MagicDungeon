package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.Player;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import java.util.LinkedList;

public class GameHUD {
    public Stage stage;
    // Removed local skin generation, using VisUIHelper

    private VisLabel hpLabel;
    private VisLabel manaLabel;
    private VisLabel xpLabel;
    private VisLabel lvlLabel;
    private VisLabel floorLabel;
    private VisLabel msgLabel;

    private LinkedList<String> logMessages = new LinkedList<>();

    private VisWindow inventoryWindow;
    private VisTable inventoryList;

    public GameHUD(Viewport viewport) {
        stage = new Stage(viewport);

        VisTable root = new VisTable();
        root.top().left();
        root.setFillParent(true);

        hpLabel = new VisLabel("HP: 100/100");
        manaLabel = new VisLabel("MP: 50/50");
        xpLabel = new VisLabel("XP: 0/100");
        lvlLabel = new VisLabel("LVL: 1");
        floorLabel = new VisLabel("Floor: 1");
        msgLabel = new VisLabel("");
        showMessage("Welcome to Dungeon!");

        // Stats Row
        root.add(hpLabel).pad(10);
        root.add(manaLabel).pad(10);
        root.add(xpLabel).pad(10);
        root.add(lvlLabel).pad(10);
        root.add(floorLabel).pad(10);
        root.row();

        // Message Log Area (expanded)
        root.add(msgLabel).colspan(5).left().pad(10).expandX();

        stage.addActor(root);

        createInventoryWindow();
    }

    private void createInventoryWindow() {
        inventoryWindow = new VisWindow("Inventory");
        inventoryWindow.setSize(400, 500);
        inventoryWindow.setCenterOnAdd(true);
        inventoryWindow.setMovable(true);
        inventoryWindow.setVisible(false);
        inventoryWindow.setResizable(false);
        inventoryWindow.addCloseButton();

        inventoryList = new VisTable();
        inventoryList.top().left();

        inventoryWindow.add(inventoryList).expand().fill().top().left().pad(10);

        stage.addActor(inventoryWindow);
    }

    public void update(Player player, int floor) {
        hpLabel.setText("HP: " + player.stats.hp + "/" + player.stats.maxHp);
        manaLabel.setText("MP: " + player.stats.mana + "/" + player.stats.maxMana);
        xpLabel.setText("XP: " + player.stats.xp);
        lvlLabel.setText("LVL: " + player.stats.level);
        floorLabel.setText("Floor: " + floor);

        updateInventory(player);
        stage.act();
    }

    public void showMessage(String msg) {
        logMessages.addFirst(msg);
        if (logMessages.size() > 5) {
            logMessages.removeLast();
        }

        StringBuilder sb = new StringBuilder();
        for (String s : logMessages) {
            sb.append(s).append("\n");
        }
        msgLabel.setText(sb.toString());
    }

    public void toggleInventory() {
        inventoryWindow.setVisible(!inventoryWindow.isVisible());
        if (inventoryWindow.isVisible()) {
            inventoryWindow.centerWindow();
        }
    }

    private void updateInventory(Player player) {
        inventoryList.clear();
        if (player.inventory.isEmpty()) {
            inventoryList.add(new VisLabel("Empty")).pad(5);
        } else {
            for (ItemData item : player.inventory) {
                inventoryList.add(new VisLabel(item.name)).left().pad(2).row();
            }
        }
    }

    public void render() {
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }
}
