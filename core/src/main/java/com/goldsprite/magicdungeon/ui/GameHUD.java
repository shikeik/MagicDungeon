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
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisScrollPane;
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

    private VisTextButton inventoryBtn;
    private VisTextButton saveBtn;
    private VisTextButton helpBtn;
    private VisWindow helpWindow;

    // Save listener interface
    private Runnable saveListener;

    public GameHUD(Viewport viewport) {
        stage = new Stage(viewport);

        // Main root table
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

        // Top bar with stats and buttons
        VisTable topBar = new VisTable();
        topBar.top();
        topBar.setFillParent(true);

        // Stats group (left side)
        VisTable statGroup = new VisTable();
        statGroup.left();
        statGroup.add(hpLabel).pad(10).row();
        statGroup.add(manaLabel).pad(10).row();
        statGroup.add(xpLabel).pad(10).row();
        statGroup.add(lvLabel).pad(10).row();
        statGroup.add(floorLabel).pad(10).row();

        // Button group (right side)
        VisTable buttonGroup = new VisTable();
        buttonGroup.right();

        // Inventory button
        inventoryBtn = new VisTextButton("背包");
        inventoryBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                toggleInventory();
                return true;
            }
        });
        buttonGroup.add(inventoryBtn).pad(5);

        // Save button
        saveBtn = new VisTextButton("保存");
        saveBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (saveListener != null) {
                    saveListener.run();
                }
                return true;
            }
        });
        buttonGroup.add(saveBtn).pad(5);

        // Help button
        helpBtn = new VisTextButton("帮助");
        helpBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                showHelp();
                return true;
            }
        });
        buttonGroup.add(helpBtn).pad(5);

        // Arrange top bar
        topBar.add(statGroup).expandX().left();
        topBar.add(buttonGroup).expandX().right();
        topBar.row();

        // Message Log Area
        topBar.add(msgLabel).colspan(2).left().pad(10, 10, 0, 10).expandX();

        stage.addActor(topBar);

        createInventoryWindow();
        createHelpWindow();
    }

    private void createInventoryWindow() {
        inventoryWindow = new VisWindow("背包");
        inventoryWindow.setSize(450, 550);
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

    private void createHelpWindow() {
        helpWindow = new VisWindow("帮助");
        helpWindow.setSize(400, 300);
        helpWindow.setCenterOnAdd(true);
        helpWindow.setMovable(true);
        helpWindow.setVisible(false);
        helpWindow.setResizable(false);
        helpWindow.addCloseButton();

        VisTable helpTable = new VisTable();
        helpTable.top().left();
        helpTable.pad(10);

        // Help content
        helpTable.add(new VisLabel("游戏说明")).center().padBottom(15).row();
        helpTable.add(new VisLabel("移动/攻击: WASD 或方向键")).left().padBottom(5).row();
        helpTable.add(new VisLabel("撞击怪物会自动攻击")).left().padBottom(5).row();
        helpTable.add(new VisLabel("技能: SPACE 键使用治疗技能")).left().padBottom(5).row();
        helpTable.add(new VisLabel("下一关: 找到楼梯并踩上去")).left().padBottom(5).row();
        helpTable.add(new VisLabel("物品: 踩上去自动拾取，背包中点击装备")).left().padBottom(5).row();
        helpTable.add(new VisLabel("存档: 点击保存按钮保存游戏进度")).left().padBottom(5).row();

        // Add scroll pane to handle long content
        VisScrollPane scrollPane = new VisScrollPane(helpTable);
        scrollPane.setFlickScroll(false);
        scrollPane.setScrollingDisabled(false, true); // Only vertical scrolling

        helpWindow.add(scrollPane).expand().fill();
        stage.addActor(helpWindow);
    }

    private void showHelp() {
        helpWindow.setVisible(true);
        helpWindow.centerWindow();
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

    public void setSaveListener(Runnable saveListener) {
        this.saveListener = saveListener;
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
