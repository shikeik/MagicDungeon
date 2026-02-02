package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.ItemType;
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
    private VisLabel lvLabel;
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
        lvLabel = new VisLabel("LV: 1");
        floorLabel = new VisLabel("Floor: 1");
        msgLabel = new VisLabel("");
        showMessage("Welcome to Dungeon!");

        // Stats Row
        root.add(hpLabel).pad(10);
        root.add(manaLabel).pad(10);
        root.add(xpLabel).pad(10);
        root.add(lvLabel).pad(10);
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
        lvLabel.setText("LV: " + player.stats.level);
        floorLabel.setText("Floor: " + floor);

        updateInventory(player);
        stage.act();
    }

    public void showMessage(String msg) {
        logMessages.add(msg);
        if (logMessages.size() > 5) {
            logMessages.removeFirst();
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
                // Create a table for each item
                VisTable itemTable = new VisTable();
                itemTable.left();
                itemTable.pad(2);
                itemTable.setWidth(300);

                // Check if item is equipped
                boolean isEquipped = (player.equipment.weapon == item) || (player.equipment.armor == item);

                // Item name with equipped status
                String equipIndicator = isEquipped ? " (Equipped)" : "";
                VisLabel nameLabel = new VisLabel(item.name + equipIndicator);
                if (isEquipped) {
                    nameLabel.setColor(1.0f, 0.7f, 0.0f, 1.0f); // Gold color for equipped items
                }

                // Item type
                VisLabel typeLabel = new VisLabel("Type: " + item.type.name());
                typeLabel.setColor(0.7f, 0.7f, 0.7f, 1.0f);

                // Item stats
                StringBuilder statsText = new StringBuilder();
                if (item.atk > 0) statsText.append("ATK: +").append(item.atk).append(" ");
                if (item.def > 0) statsText.append("DEF: +").append(item.def).append(" ");
                if (item.heal > 0) statsText.append("HEAL: ").append(item.heal);
                VisLabel statsLabel = new VisLabel(statsText.toString());
                statsLabel.setColor(0.0f, 0.8f, 0.0f, 1.0f);

                // Add to table
                itemTable.add(nameLabel).left().pad(2).expandX().row();
                itemTable.add(typeLabel).left().pad(2).row();
                itemTable.add(statsLabel).left().pad(2).row();

                // Add border
                itemTable.add(new VisTable()).padTop(5).row();

                // Add click listener
                itemTable.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                    @Override
                    public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                        // Equip or use item
                        player.equip(item);

                        // Show message
                        String action = item.type == ItemType.POTION ? "Used" : (isEquipped ? "Unequipped" : "Equipped");
                        showMessage(action + " " + item.name);

                        // Refresh inventory
                        updateInventory(player);
                        return true;
                    }
                });

                inventoryList.add(itemTable).left().expandX().fillX().pad(2).row();
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
