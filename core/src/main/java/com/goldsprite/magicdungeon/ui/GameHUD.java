package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.ItemType;
import com.goldsprite.magicdungeon.entities.Player;
import com.kotcrab.vis.ui.widget.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
	private BaseDialog helpWindow;

	private Texture slotBgTexture;
	private Texture slotBorderTexture;
	private NinePatchDrawable slotBgDrawable;
	private NinePatchDrawable slotBorderDrawable;

	private Player currentPlayer;

	// Save listener interface
	private Runnable saveListener;

	// Inventory Dialog class that extends BaseDialog
	private class InventoryDialog extends BaseDialog {
		public InventoryDialog() {
			super("背包");
			// Set size to 2/3 of the screen or fixed larger size
			float width = Math.max(800, stage.getWidth() * 0.66f);
			float height = Math.max(600, stage.getHeight() * 0.66f);
			setSize(width, height);
			setCenterOnAdd(true);
			autoPack = true;

			inventoryList = new VisTable();
			inventoryList.top().left();

			VisScrollPane inventoryScrollPane = new VisScrollPane(inventoryList);
			inventoryScrollPane.setScrollingDisabled(true, false);
			inventoryScrollPane.setFlickScroll(true);
			inventoryScrollPane.setFadeScrollBars(false);

			getContentTable().add(inventoryScrollPane).pad(10);
		}
	}

	private class InventorySlot extends VisTable {
		public InventorySlot(InventoryItem item, Player player) {
			boolean isEquipped = (player.equipment.weapon != null && player.equipment.weapon.equals(item)) ||
								 (player.equipment.armor != null && player.equipment.armor.equals(item));

			// Make sure the slot itself is touchable
			setTouchable(Touchable.enabled);

			// 1. Stack Layout
			Stack stack = new Stack();

			// 2. Background
			VisTable bgTable = new VisTable();
			bgTable.setBackground(slotBgDrawable);
			bgTable.setTouchable(Touchable.disabled); // Prevent blocking events
			stack.add(bgTable);

			// 3. Icon
			Texture tex = TextureManager.getInstance().getItem(item.data.name());
			if (tex != null) {
				VisImage icon = new VisImage(new TextureRegionDrawable(tex));
				VisTable iconTable = new VisTable();
				iconTable.add(icon).size(48, 48);
				iconTable.setTouchable(Touchable.disabled); // Prevent blocking events
				stack.add(iconTable);
			}

			// 4. Equipped Badge (Top-Right "E")
			if (isEquipped) {
				VisLabel badge = new VisLabel("E");
				badge.setColor(Color.YELLOW);
				badge.setFontScale(0.25f);

				VisTable badgeTable = new VisTable();
				badgeTable.bottom().right();
				badgeTable.add(badge).pad(1);
				badgeTable.setTouchable(Touchable.disabled); // Prevent blocking events
				stack.add(badgeTable);
			}

			add(stack).size(64, 64);

			// 5. Tooltip
			createTooltip(item, isEquipped);

			// 6. Click Listener
			addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					player.equip(item);
					String action = item.data.type == ItemType.POTION ? "使用" : (isEquipped ? "卸下" : "装备");
					showMessage(action + " " + item.data.name);
					updateInventory(player);
				}
			});
		}

		private void createTooltip(InventoryItem item, boolean isEquipped) {
			VisTable content = new VisTable();
			content.setBackground("list");
			content.pad(10);

			// Title
			VisLabel title = new VisLabel(item.data.name);
			if (item.data.color != null) title.setColor(item.data.color);
			content.add(title).left().row();

			//uuid
			int maxLen = 9;
			String shortId = item.id.length() > maxLen ? item.id.substring(item.id.length()-maxLen) : item.id;
			shortId = item.id;
			VisLabel uuid = new VisLabel("#"+shortId);
			uuid.setColor(Color.DARK_GRAY);
			content.add(uuid).right().row();

			content.add(new Separator()).growX().padBottom(15).row();

			// Type
			content.add(new VisLabel("类型: " + getTypeString(item.data.type))).left().row();

			// Stats
			if (item.data.atk > 0) content.add(new VisLabel("攻击: +" + item.data.atk)).left().row();
			if (item.data.def > 0) content.add(new VisLabel("防御: +" + item.data.def)).left().row();
			if (item.data.heal > 0) content.add(new VisLabel("回复: +" + item.data.heal)).left().row();

			// Status
			if (isEquipped) {
				VisLabel status = new VisLabel("已装备");
				status.setColor(Color.YELLOW);
				content.add(status).left().padTop(5).row();
			}

			// Create Tooltip
			new Tooltip.Builder(content).target(this).build();
		}

		private String getTypeString(ItemType type) {
			switch(type) {
				case WEAPON: return "武器";
				case ARMOR: return "防具";
				case POTION: return "药水";
				default: return "未知";
			}
		}
	}

	public GameHUD(Viewport viewport) {
		Tooltip.DEFAULT_APPEAR_DELAY_TIME = 0.2f; // 缩短Tooltip浮现时间

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

		// Initialize help window
		createHelpWindow();

		// Generate assets
		generateAssets();
	}

	private void generateAssets() {
		// 1. Slot Background (Rounded Rect with semi-transparent dark fill)
		Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		pixmap.setColor(0.2f, 0.2f, 0.2f, 0.8f);
		pixmap.fillRectangle(2, 2, 60, 60); // Inner fill
		pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
		pixmap.drawRectangle(0, 0, 64, 64); // Border
		pixmap.drawRectangle(1, 1, 62, 62); // Thicker border

		slotBgTexture = new Texture(pixmap);
		slotBgDrawable = new NinePatchDrawable(new NinePatch(slotBgTexture, 4, 4, 4, 4));
		pixmap.dispose();

		// 2. Equipped Border (Gold)
		Pixmap borderPm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		borderPm.setColor(1f, 0.8f, 0f, 1f); // Gold
		// Draw thick border
		for(int i=0; i<4; i++) {
			borderPm.drawRectangle(i, i, 64-i*2, 64-i*2);
		}

		slotBorderTexture = new Texture(borderPm);
		slotBorderDrawable = new NinePatchDrawable(new NinePatch(slotBorderTexture, 5, 5, 5, 5));
		borderPm.dispose();
	}

	// Removed createInventoryWindow method (replaced by InventoryDialog)

	private void createHelpWindow() {
		helpWindow = new BaseDialog("帮助");
		helpWindow.autoPack = false;
		helpWindow.setSize(400, 300);
		helpWindow.setCenterOnAdd(true);
		helpWindow.setMovable(true);
		helpWindow.setResizable(false);

		VisTable helpTable = new VisTable();
		helpTable.top().left();
		helpTable.pad(10);

		// Help content
		helpTable.add("游戏说明").center().padBottom(15).row();
		String helpMsg = ""
			+"移动/攻击: WASD 或方向键"
			+"\n"+"撞击怪物会自动攻击"
			+"\n"+"技能: SPACE 键使用治疗技能"
			+"\n"+"下一关: 找到楼梯并踩上去"
			+"\n"+"物品: 踩上去自动拾取，背包中点击装备"
			+"\n"+"存档: 点击保存按钮保存游戏进度"
			;
		VisLabel helpLabel = new VisLabel(helpMsg);
		helpLabel.setWrap(true);
		helpTable.add(helpLabel).minWidth(0).grow().left();

		// Add scroll pane to handle long content
		VisScrollPane scrollPane = new VisScrollPane(helpTable);
		scrollPane.setFlickScroll(true);
		scrollPane.setScrollingDisabled(true, false); // Only vertical scrolling

		helpWindow.getContentTable().add(scrollPane).expand().fill();
	}

	private void showHelp() {
		helpWindow.show(stage);
	}

	public void update(Player player, int floor) {
		this.currentPlayer = player;
		hpLabel.setText("HP: " + player.stats.hp + "/" + player.stats.maxHp);
		manaLabel.setText("MP: " + player.stats.mana + "/" + player.stats.maxMana);
		xpLabel.setText("XP: " + player.stats.xp);
		lvLabel.setText("LV: " + player.stats.level);
		floorLabel.setText("Floor: " + floor);

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
		if (currentPlayer != null) {
			updateInventory(currentPlayer);
		}
		// Show inventory dialog
		if(stage.getActors().contains(inventoryDialog, true)) inventoryDialog.remove();
		else inventoryDialog.show(stage);
	}

	// Method to update inventory dialog content
	public void updateInventoryDialog(Player player) {
		updateInventory(player);
	}

	public void setSaveListener(Runnable saveListener) {
		this.saveListener = saveListener;
	}

	public void updateInventory(Player player) {
		if (player == null) return;
		inventoryList.clear();
		if (player.inventory.isEmpty()) {
			inventoryList.add(new VisLabel("背包是空的")).pad(20);
		} else {
			int itemsPerRow = 5;
			int count = 0;

			for (InventoryItem item : player.inventory) {
				// Use new InventorySlot
				InventorySlot slot = new InventorySlot(item, player);
				inventoryList.add(slot).size(64, 64).pad(10);

				count++;
				if (count % itemsPerRow == 0) {
					inventoryList.row();
				}
			}
		}
	}

	public void render() {
		stage.draw();
	}

	public void dispose() {
		stage.dispose();
		if (slotBgTexture != null) slotBgTexture.dispose();
		if (slotBorderTexture != null) slotBorderTexture.dispose();
	}
}
