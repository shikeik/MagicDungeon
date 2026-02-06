package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.InventoryItem;
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

    private InventoryDialog inventoryDialog;
    private VisTable inventoryList;

    private VisTextButton inventoryBtn;
    private VisTextButton saveBtn;
    private VisTextButton helpBtn;
    private VisWindow helpWindow;

    // Save listener interface
    private Runnable saveListener;

    // Inventory Dialog class that extends BaseDialog
    private class InventoryDialog extends BaseDialog {
        public InventoryDialog() {
            super("背包");
            setSize(450, 550);
        }
    }

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

        // Initialize inventory dialog
        inventoryDialog = new InventoryDialog();
        inventoryList = new VisTable();
        inventoryList.top().left();
        inventoryDialog.add(inventoryList).expand().fill().top().left().pad(10);

        // Initialize help window
        createHelpWindow();
    }

    // Removed createInventoryWindow method (replaced by InventoryDialog)

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
        // Close other windows if open
        if (inventoryDialog != null && inventoryDialog.isVisible()) {
            inventoryDialog.hide();
        }
        helpWindow.setVisible(true);
        helpWindow.centerWindow();
        // Ensure the window is at the top
        helpWindow.toFront();
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
        // Close other windows if open
        if (helpWindow.isVisible()) {
            helpWindow.setVisible(false);
        }

        // Show inventory dialog
        inventoryDialog.show(stage);
    }

    // Method to update inventory dialog content
    public void updateInventoryDialog(Player player) {
        updateInventory(player);
    }

    public void setSaveListener(Runnable saveListener) {
        this.saveListener = saveListener;
    }

    public void updateInventory(Player player) {
        inventoryList.clear();
        if (player.inventory.isEmpty()) {
            inventoryList.add(new VisLabel("Empty")).pad(5);
        } else {
            for (InventoryItem item : player.inventory) {
                // Create a table for each item
                VisTable itemTable = new VisTable();
                itemTable.left();
                itemTable.pad(2);
                itemTable.setWidth(300);

                // Check if item is equipped
                // Now comparing InventoryItem instances (or by ID)
                boolean isEquipped = (player.equipment.weapon != null && player.equipment.weapon.equals(item)) || 
                                     (player.equipment.armor != null && player.equipment.armor.equals(item));

                // Item name with equipped status
                String equipIndicator = isEquipped ? " (Equipped)" : "";
                VisLabel nameLabel = new VisLabel(item.data.name + equipIndicator);
                if (isEquipped) {
                    nameLabel.setColor(1.0f, 0.7f, 0.0f, 1.0f); // Gold color for equipped items
                }

                // Item type
                VisLabel typeLabel = new VisLabel("Type: " + item.data.type.name());
                typeLabel.setColor(0.7f, 0.7f, 0.7f, 1.0f);

                // Item stats
                StringBuilder statsText = new StringBuilder();
                if (item.data.atk > 0) statsText.append("ATK: +").append(item.data.atk).append(" ");
                if (item.data.def > 0) statsText.append("DEF: +").append(item.data.def).append(" ");
                if (item.data.heal > 0) statsText.append("HEAL: ").append(item.data.heal);
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
                        String action = item.data.type == ItemType.POTION ? "Used" : (isEquipped ? "Unequipped" : "Equipped");
                        showMessage(action + " " + item.data.name);

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
