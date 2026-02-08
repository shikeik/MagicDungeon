package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.SkewBar;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.ItemType;
import com.goldsprite.magicdungeon.entities.Monster;
import com.goldsprite.magicdungeon.entities.Player;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;

import com.goldsprite.magicdungeon.entities.ItemData;
import java.util.function.Predicate;
import static com.goldsprite.magicdungeon.core.screens.GameScreen.isPaused;

import com.goldsprite.magicdungeon.utils.Constants;

public class GameHUD {
	public Stage stage;
	private NeonBatch neonBatch;

	// Player Stats
	private SkewBar hpBar;
	private SkewBar manaBar;
	private SkewBar xpBar;
	private VisLabel hpLabel;
	private VisLabel manaLabel;
	private VisLabel xpLabel;
	private VisImage avatarImage;
	private VisLabel levelBadge;
	private VisLabel floorLabel;

	private VisLabel coinLabel; // New Coin Label

	// Quick Slots
	private QuickSlot hpQuickSlot;
	private QuickSlot mpQuickSlot;

	// System Log
	private VisLabel msgLabel;
	private List<String> logMessages = new ArrayList<>();

	// Monster Info
	private VisTable monsterInfoTable;
	private SkewBar monsterHpBar;
	private VisImage monsterHead;
	private VisLabel monsterNameLabel;
	private VisLabel monsterLvLabel; // Although monsters don't strictly have levels visible in code, we can show something or hide it
	private Monster currentTargetMonster;

	// UI Components
	private VisLabel pauseLabel;
	private InventoryDialog inventoryDialog;
	private VisTable inventoryList;
	private VisTable equipmentTable; // New: To hold equipment slots
	private VisTable statsTable;     // New: To hold stats
	private AvatarWidget avatarWidget; // New: Avatar
	private VisTextButton inventoryBtn;
	private VisTextButton saveBtn;
	private VisTextButton helpBtn;
	private BaseDialog helpWindow;

	// Android Controls
	private Touchpad touchpad;
	private VisTextButton attackBtn;
	private VisTextButton interactBtn;
	private boolean isAttackPressed;
	private boolean isInteractPressed;

	// Assets
	private Texture slotBgTexture;
	private Texture slotBorderTexture;
	private NinePatchDrawable slotBgDrawable;
	private NinePatchDrawable slotBorderDrawable;
	private Texture whiteTexture;
	private TextureRegionDrawable whiteDrawable;
	private TextureRegionDrawable logBgDrawable;

	private Player currentPlayer;
	private Runnable saveListener;
	private Runnable returnToCampListener;

	private Actor currentTooltip;

	private DragAndDrop dragAndDrop;

	// --- Inner Classes ---

	private static class GameOverWindow extends BaseDialog {
		public GameOverWindow(Runnable onRestart, Runnable onQuit) {
			super("GAME OVER");
			setModal(true);
			setMovable(false);
			setResizable(false);

			Table content = getContentTable();
			content.pad(20);

			VisLabel title = new VisLabel("你死掉了!");
			title.setColor(Color.RED);
			title.setFontScale(1.5f);
			content.add(title).padBottom(20).row();

			VisTextButton restartBtn = new VisTextButton("重新开始");
			restartBtn.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					onRestart.run();
					hide();
				}
			});

			VisTextButton quitBtn = new VisTextButton("退出游戏");
			quitBtn.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					onQuit.run();
				}
			});

			VisTable btnTable = new VisTable();
			btnTable.add(restartBtn).width(120).padRight(20);
			btnTable.add(quitBtn).width(120);

			content.add(btnTable);

			pack();
			centerWindow();
		}
	}

	private class AvatarWidget extends VisTable {
		private Image avatarImage;
		private Player player;

		public AvatarWidget() {
			avatarImage = new Image();
			add(avatarImage).size(128, 128);
		}

		public void update(Player player) {
			this.player = player;
			if (player == null) return;
			// Generate texture based on equipment
			String mainHand = player.equipment.mainHand != null ? player.equipment.mainHand.data.name() : null;
			String offHand = player.equipment.offHand != null ? player.equipment.offHand.data.name() : null;
			String helmet = player.equipment.helmet != null ? player.equipment.helmet.data.name() : null;
			String armor = player.equipment.armor != null ? player.equipment.armor.data.name() : null;
			String boots = player.equipment.boots != null ? player.equipment.boots.data.name() : null;

			Texture tex = SpriteGenerator.generateCharacterTexture(mainHand, offHand, helmet, armor, boots);
			avatarImage.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
		}
	}

	private class InventoryDialog extends BaseDialog {
		public InventoryDialog() {
			super("背包");
			float width = Math.max(900, stage.getWidth() * 0.8f);
			float height = stage.getHeight() * 0.8f;

			setSize(width, height);
			setCenterOnAdd(true);
			autoPack = false;

			VisTable mainTable = new VisTable();
			mainTable.setFillParent(true);
			mainTable.pad(20);

			// Split Pane Layout
			// Left: Character (40%)
			VisTable leftCol = new VisTable();
			leftCol.setBackground(slotBorderDrawable); // Border for area

			// Right: Inventory (60%)
			VisTable rightCol = new VisTable();
			rightCol.setBackground(slotBorderDrawable); // Border for area

			// --- Left Column Setup ---
			equipmentTable = new VisTable();
			statsTable = new VisTable();
			avatarWidget = new AvatarWidget();

			leftCol.add(new VisLabel("角色状态")).pad(10).top().row();
			leftCol.add(equipmentTable).expand().fill().row();
			leftCol.add(statsTable).growX().pad(10).bottom();

			// --- Right Column Setup ---
			inventoryList = new VisTable();
			inventoryList.top().left();

			VisScrollPane inventoryScrollPane = new VisScrollPane(inventoryList);
			inventoryScrollPane.setScrollingDisabled(true, false);
			inventoryScrollPane.setFlickScroll(true);
			inventoryScrollPane.setFadeScrollBars(false);

			rightCol.add(new VisLabel("物品栏")).pad(10).top();

			// Coin info inside inventory
			coinLabel = new VisLabel("金币: 0");
			coinLabel.setColor(Color.GOLD);
			rightCol.add(coinLabel).pad(10).right().row();

			rightCol.add(inventoryScrollPane).grow().pad(10).colspan(2);

			// Add columns to main table
			// Modified: Adjusted ratio to 30:70 to fit 8 columns in inventory
			mainTable.add(leftCol).width(width * 0.3f).growY().padRight(10);
			mainTable.add(rightCol).width(width * 0.7f).growY();

			getContentTable().add(mainTable).grow();
		}
	}

	private class InventorySlot extends VisTable {
		public InventorySlot(InventoryItem item, Player player, DragAndDrop dragAndDrop) {
			boolean isEquipped = (item != null) && checkIsEquipped(player, item);

			setTouchable(Touchable.enabled);

			Stack stack = new Stack();

			// Background
			VisTable bgTable = new VisTable();
			bgTable.setBackground(slotBgDrawable);
			bgTable.setTouchable(Touchable.disabled);
			stack.add(bgTable);

			if (item != null) {
				// Icon
				VisTable iconTable = ItemRenderer.createItemIcon(item, 48);
				iconTable.setTouchable(Touchable.disabled);
				stack.add(iconTable);

				// Equipped Badge
				if (isEquipped) {
					VisLabel badge = new VisLabel("E");
					badge.setColor(Color.YELLOW);
					badge.setFontScale(0.25f);
					VisTable badgeTable = new VisTable();
					badgeTable.top().right();
					badgeTable.add(badge).pad(1);
					badgeTable.setTouchable(Touchable.disabled);
					stack.add(badgeTable);
				}

				// Count Badge
				if (item.count > 1) {
					VisLabel countLabel = new VisLabel(String.valueOf(item.count));
					countLabel.setFontScale(0.4f);
					VisTable countTable = new VisTable();
					countTable.bottom().right();
					countTable.add(countLabel).pad(2);
					countTable.setTouchable(Touchable.disabled);
					stack.add(countTable);
				}
			}

			add(stack).size(64, 64);

			if (Gdx.app.getType() == ApplicationType.Android) {
				if (item != null) {
					ActorGestureListener listener = new ActorGestureListener() {
						@Override
						public void tap(InputEvent event, float x, float y, int count, int button) {
							handleEquipAction(item, isEquipped, player);
							hideAndroidTooltip(); // 确保点击时清除可能残留的 tooltip
						}

						@Override
						public boolean longPress(Actor actor, float x, float y) {
							showAndroidTooltip(InventorySlot.this, item, isEquipped);
							return true;
						}

						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							super.touchUp(event, x, y, pointer, button);
							// 长按后松开，或者点击后松开，都尝试隐藏
							hideAndroidTooltip();
						}
					};
					listener.getGestureDetector().setLongPressSeconds(0.18f); // 缩短长按时间
					addListener(listener);
				}
			} else {
				if (item != null) {
					VisTable tooltipContent = createItemTooltipTable(item, isEquipped);
					new Tooltip.Builder(tooltipContent).target(this).build();

					// Add Right Click Listener BEFORE DragAndDrop to capture event
					addListener(new InputListener() {
						@Override
						public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
							if (button == Input.Buttons.RIGHT) {
								return true; // Capture right click
							}
							return false;
						}

						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							if (button == Input.Buttons.RIGHT) {
								showContextMenu(item, isEquipped, player, event.getStageX(), event.getStageY());
							}
						}
					});
				}
			}

			// Configure Drag Source
			if (dragAndDrop != null && item != null) {
				dragAndDrop.addSource(new Source(this) {
					@Override
					public Payload dragStart(InputEvent event, float x, float y, int pointer) {
						// Prevent drag if right button (just in case)
						if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) return null;

						Payload payload = new Payload();
						payload.setObject(item);

						// Create drag actor (copy of the icon)
						VisTable dragActor = ItemRenderer.createItemIcon(item, 48);
						payload.setDragActor(dragActor);

						// Optional: set invalid/valid drag actors
						return payload;
					}
				});
			}
		}
	}

	private class EquipmentSlot extends VisTable {
		public EquipmentSlot(InventoryItem item, String placeholder, Player player, ItemType slotType, int slotIndex, DragAndDrop dragAndDrop) {
			setBackground(slotBgDrawable);

			Stack stack = new Stack();

			// Background / Placeholder
			VisTable bg = new VisTable();
			if (item == null) {
				VisLabel label = new VisLabel(placeholder);
				label.setFontScale(0.4f);
				label.setColor(Color.GRAY);
				label.setAlignment(Align.center);
				bg.add(label);
			}
			stack.add(bg);

			if (item != null) {
				// Icon
				VisTable icon = ItemRenderer.createItemIcon(item, 48);
				stack.add(icon);

				// Tooltip
				if (Gdx.app.getType() == ApplicationType.Android) {
					ActorGestureListener listener = new ActorGestureListener() {
						@Override
						public void tap(InputEvent event, float x, float y, int count, int button) {
							// No action on tap for now, or maybe show info
							hideAndroidTooltip();
						}
						@Override
						public boolean longPress(Actor actor, float x, float y) {
							showAndroidTooltip(EquipmentSlot.this, item, true);
							return true;
						}
						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							super.touchUp(event, x, y, pointer, button);
							hideAndroidTooltip();
						}
					};
					listener.getGestureDetector().setLongPressSeconds(0.18f);
					addListener(listener);
				} else {
					VisTable tooltipContent = createItemTooltipTable(item, true);
					new Tooltip.Builder(tooltipContent).target(this).build();
				}

				// Click to Unequip (Standard click behavior)
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						player.equip(item, slotIndex);
						updateInventory(player);
					}
				});
			}

			add(stack).size(64, 64);

			// Configure Drag Target
			if (dragAndDrop != null) {
				dragAndDrop.addTarget(new Target(this) {
					@Override
					public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
						Object obj = payload.getObject();
						if (obj instanceof InventoryItem) {
							InventoryItem draggedItem = (InventoryItem) obj;
							// Check if item type matches slot type
							if (draggedItem.data.type == slotType) {
								getActor().setColor(Color.GREEN); // Highlight
								return true;
							}
						}
						getActor().setColor(Color.RED); // Invalid
						return false;
					}

					@Override
					public void drop(Source source, Payload payload, float x, float y, int pointer) {
						Object obj = payload.getObject();
						if (obj instanceof InventoryItem) {
							InventoryItem draggedItem = (InventoryItem) obj;
							if (draggedItem.data.type == slotType) {
								player.equip(draggedItem, slotIndex);
								updateInventory(player);
							}
						}
						getActor().setColor(Color.WHITE); // Reset color
					}

					@Override
					public void reset(Source source, Payload payload) {
						getActor().setColor(Color.WHITE);
					}
				});
			}
		}
	}

	private class QuickSlot extends VisTable {
		private VisImage iconImage;
		private VisLabel countLabel;
		private InventoryItem currentItem;
		private Predicate<InventoryItem> filter;
		private ItemData targetItemData; // Specific item to look for
		private Player player;

		public QuickSlot(Predicate<InventoryItem> filter) {
			this.filter = filter;

			setBackground(slotBgDrawable);
			setTouchable(Touchable.enabled);

			Stack stack = new Stack();

			// Icon Container
			VisTable iconContainer = new VisTable();
			iconImage = new VisImage();
			iconImage.setVisible(false);
			iconContainer.add(iconImage).size(48, 48);
			stack.add(iconContainer);

			// Count Container
			VisTable countContainer = new VisTable();
			countContainer.bottom().right();
			countLabel = new VisLabel("");
			countLabel.setFontScale(0.4f);
			countLabel.setVisible(false);
			countContainer.add(countLabel).pad(2);
			stack.add(countContainer);

			add(stack).size(64, 64);

			addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					useItem();
				}
			});
		}

		public void setTargetItem(ItemData data) {
			this.targetItemData = data;
			this.filter = (item) -> item.data == data;
			if (player != null) update(player);
		}

		public void update(Player player) {
			this.player = player;
			if (player == null) return;

			// Find item in inventory
			currentItem = null;
			if (player.inventory != null) {
				for (InventoryItem item : player.inventory) {
					if (filter.test(item)) {
						currentItem = item;
						break;
					}
				}
			}

			if (currentItem != null) {
				TextureRegion tex = TextureManager.getInstance().getItem(currentItem.data.name());
				if (tex != null) {
					iconImage.setDrawable(new TextureRegionDrawable(tex));
					iconImage.setVisible(true);
				}
				countLabel.setText(String.valueOf(currentItem.count));
				countLabel.setVisible(true);
			} else {
				iconImage.setVisible(false);
				countLabel.setVisible(false);
			}
		}

		private void useItem() {
			if (currentItem != null && player != null) {
				InventoryItem itemToUse = currentItem; // Cache reference
				player.equip(itemToUse);
				showMessage("使用了 " + itemToUse.data.name);
				updateInventory(player); // Refresh Inventory Dialog if open
				update(player); // Refresh self (currentItem might become null here)

				// Update Bars Immediately
				if (itemToUse.data == ItemData.Health_Potion) {
					hpBar.setValue(player.stats.hp);
					hpLabel.setText(player.stats.hp + "/" + player.stats.maxHp);
				} else if (itemToUse.data == ItemData.Mana_Potion) {
					manaBar.setValue(player.stats.mana);
					manaLabel.setText(player.stats.mana + "/" + player.stats.maxMana);
				}
			} else {
				// showMessage("没有可用的物品!");
			}
		}
	}

		private void handleEquipAction(InventoryItem item, boolean isEquipped, Player player) {
		player.equip(item);
		String action = item.data.type == ItemType.POTION ? "使用" : (isEquipped ? "卸下" : "装备");
		showMessage(action + " " + item.data.name);
		updateInventory(player);
	}

	private void showContextMenu(InventoryItem item, boolean isEquipped, Player player, float x, float y) {
			PopupMenu menu = new PopupMenu();

			// Equip / Unequip / Use
			if (item.data.type == ItemType.POTION || item.data.type == ItemType.ETC) {
				MenuItem useItem = new MenuItem("使用", new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						handleEquipAction(item, isEquipped, player);
					}
				});
				menu.addItem(useItem);

				// Potion Quick Slot
				if (item.data.type == ItemType.POTION) {
					// Add Quick Slot Options
					MenuItem quickSlot1 = new MenuItem("设为快捷栏 1 (HP)", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							if (hpQuickSlot != null) {
								hpQuickSlot.setTargetItem(item.data);
								showMessage("快捷栏 1 已设置为: " + item.data.name);
							}
						}
					});
					menu.addItem(quickSlot1);

					MenuItem quickSlot2 = new MenuItem("设为快捷栏 2 (MP)", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							if (mpQuickSlot != null) {
								mpQuickSlot.setTargetItem(item.data);
								showMessage("快捷栏 2 已设置为: " + item.data.name);
							}
						}
					});
					menu.addItem(quickSlot2);
				}

			} else {
				if (isEquipped) {
					MenuItem unequipItem = new MenuItem("卸下", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							handleEquipAction(item, isEquipped, player);
						}
					});
					menu.addItem(unequipItem);
				} else {
					MenuItem equipItem = new MenuItem("穿戴", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							handleEquipAction(item, isEquipped, player);
						}
					});
					menu.addItem(equipItem);
				}
			}

			// Sell (Only if NOT equipped)
			if (!isEquipped) {
				MenuItem sellItem = new MenuItem("出售 (" + item.getValue() + "硬币)", new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						player.sellItem(item);
						showMessage("出售了 " + item.data.name + " 获得 " + item.getValue() + " 硬币");
						updateInventory(player);
					}
				});
				menu.addItem(sellItem);
			}

			menu.showMenu(stage, x, y);
		}

	private VisTable createItemTooltipTable(InventoryItem item, boolean isEquipped) {
		VisTable borderTable = new VisTable();
		// 复用 slotBorderDrawable 来作为 tooltip 的边框
		if (slotBorderDrawable != null) {
			borderTable.setBackground(slotBorderDrawable);
			borderTable.setColor(item.quality.color); // 边框颜色跟随品质
		}

		VisTable content = new VisTable();
		content.setBackground("list");
		content.pad(10);
		borderTable.add(content).grow().pad(2); // pad(2) 是为了露出背景的边框

		// === 布局重构：左右分栏 ===
		// 主容器：水平布局
		VisTable mainContainer = new VisTable();
		content.add(mainContainer).growX().row();

		// [左侧] 物品高清大图 (独占一列)
		// 尺寸调整为 160px
		VisTable leftCol = new VisTable();
		VisTable bigIcon = ItemRenderer.createItemIcon(item, 160);
		leftCol.add(bigIcon).top().left();
		mainContainer.add(leftCol).top().left().padRight(15);

		// [右侧] 物品信息
		VisTable rightCol = new VisTable();
		mainContainer.add(rightCol).growX().top().left();

		// 1. 标题 (名称 + 品质)
		String titleText = "[" + item.quality.name + "] " + item.data.name;
		VisLabel title = new VisLabel(titleText);
		title.setColor(item.quality.color);
		title.setWrap(true); // 防止标题过长
		rightCol.add(title).growX().left().row();

		// 2. ID
		int maxLen = 9;
		String shortId = item.id.length() > maxLen ? item.id.substring(item.id.length()-maxLen) : item.id;
		VisLabel uuid = new VisLabel("#"+shortId);
		uuid.setColor(Color.DARK_GRAY);
		// uuid.setFontScale(1.0f); // 保持一致，移除之前的缩放设置
		rightCol.add(uuid).left().padBottom(5).row();

		// 3. 分割线
		rightCol.add(new Separator()).growX().padBottom(8).row();

		// 4. 类型
		rightCol.add(new VisLabel("类型: " + getTypeString(item.data.type))).left().padBottom(2).row();

		// 5. 属性
		if (item.atk > 0) rightCol.add(new VisLabel("攻击: +" + item.atk)).left().padBottom(2).row();
		if (item.def > 0) rightCol.add(new VisLabel("防御: +" + item.def)).left().padBottom(2).row();
		
		if (item.data == ItemData.Mana_Potion) {
			rightCol.add(new VisLabel("回魔: +" + item.heal)).left().padBottom(2).row();
		} else if (item.data == ItemData.Elixir) {
			rightCol.add(new VisLabel("回血: +" + item.heal)).left().padBottom(2).row();
			rightCol.add(new VisLabel("回魔: +" + item.heal)).left().padBottom(2).row();
		} else {
			String hpRegenLabel = item.data.type == ItemType.POTION ? "回血: +" : "自然回血: +";
			String mpRegenLabel = item.data.type == ItemType.POTION ? "回魔: +" : "自然回魔: +";
			
			if (item.heal > 0) rightCol.add(new VisLabel(hpRegenLabel + item.heal)).left().padBottom(2).row();
			if (item.manaRegen > 0) rightCol.add(new VisLabel(mpRegenLabel + item.manaRegen)).left().padBottom(2).row();
		}

		// 6. 状态 (已装备)
		if (isEquipped) {
			VisLabel status = new VisLabel("已装备");
			status.setColor(Color.YELLOW);
			rightCol.add(status).left().padTop(8).row();
		}

		// 7. 价值 (最底部)
		VisLabel valueLabel = new VisLabel("价值: " + item.getValue() + " 硬币");
		valueLabel.setColor(Color.GOLD);
		// 字体大小与其他一致，不进行缩放
		rightCol.add(valueLabel).left().padTop(5).row();
		return borderTable;
	}

	private void showAndroidTooltip(Actor target, InventoryItem item, boolean isEquipped) {
		// Remove existing tooltip if any
		hideAndroidTooltip();

		VisTable tooltip = createItemTooltipTable(item, isEquipped);
		// 不需要额外的背景，因为 createItemTooltipTable 已经创建了带边框的背景
		tooltip.setTouchable(Touchable.disabled); // Don't block touches

		stage.addActor(tooltip);

		// Position above the target
		Vector2 pos = target.localToStageCoordinates(new Vector2(0, 0));
		float x = pos.x;
		float y = pos.y + target.getHeight();

		// Ensure within screen bounds
		tooltip.pack();
		float w = tooltip.getWidth();
		float h = tooltip.getHeight();

		// Clamp X
		if (x + w > stage.getWidth()) x = stage.getWidth() - w;
		if (x < 0) x = 0;

		// Clamp Y (if top of screen, show below)
		if (y + h > stage.getHeight()) {
			y = pos.y - h;
		}

		tooltip.setPosition(x, y);

		currentTooltip = tooltip;
	}

	private void hideAndroidTooltip() {
		if (currentTooltip != null) {
			currentTooltip.remove();
			currentTooltip = null;
		}
	}

	private String getTypeString(ItemType type) {
		switch(type) {
			case MAIN_HAND: return "主手";
			case OFF_HAND: return "副手";
			case HELMET: return "头盔";
			case ARMOR: return "铠甲";
			case BOOTS: return "鞋子";
			case ACCESSORY: return "饰品";
			case POTION: return "药水";
			case ETC: return "杂物";
			default: return "未知";
		}
	}

	public GameHUD(Viewport viewport) {
		Tooltip.DEFAULT_APPEAR_DELAY_TIME = 0.2f;

		// Coin info
		coinLabel = new VisLabel("Coins: 0");

		// Use NeonBatch for SkewBar support
		this.neonBatch = new NeonBatch();
		this.stage = new Stage(viewport, this.neonBatch);

		generateAssets();

		// Initialize Styles
		initStyles();

		// Root Table
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.top();

		// 1. Toolbar (Top, Full Width)
		VisTable toolbar = new VisTable();
		toolbar.setBackground(logBgDrawable); // Optional background
		VisTable buttonGroup = new VisTable();
		createButtons(buttonGroup);
		toolbar.add(buttonGroup).growX().right().pad(5);
		root.add(toolbar).growX().top().row();

		// 2. System Log (Top Left, Overlay)
		// We use a separate container for Log to allow it to be an overlay or just flow.
		// Since root is table based, we can put it in the next row, left aligned.
		VisTable logContainer = new VisTable();
		logContainer.setBackground(logBgDrawable);
		msgLabel = new VisLabel("欢迎来到地下城!", "small");
		msgLabel.setFontScale(0.3f);
		msgLabel.setWrap(true);
		msgLabel.setAlignment(Align.topLeft);
		logContainer.add(msgLabel).width(500).pad(10).top().left();

		// Add Log to root (Left aligned, expand Y to push bottom HUD down, but don't take all space)
		// Actually, we want log to be top-left, but not push other things too much.
		// Let's use an intermediate table for the "Middle" area
		VisTable middleTable = new VisTable();
		middleTable.top().left();
		middleTable.add(logContainer).top().left().pad(10);
		root.add(middleTable).expand().fill().row();

		// 3. Bottom Area: Player HUD
		VisTable playerHud = createPlayerHud();
		root.add(playerHud).bottom().padBottom(20);

		stage.addActor(root);

		// --- Overlay: Monster Info (Top Right) ---
		createMonsterInfo();
		// Manually position or add to stage.
		// Since we want it fixed at Top-Right, we can add it to stage and set position in resize or update.
		// Or use a root-like table that fills parent?
		// Let's add directly to stage and set position.
		stage.addActor(monsterInfoTable);

		// --- Overlay: Pause Label ---
		createPauseLabel();
		stage.addActor(pauseLabel.getParent()); // Add the container table

		// --- Windows ---
		inventoryDialog = new InventoryDialog();
		dragAndDrop = new DragAndDrop();
		createHelpWindow();

		// --- Android Controls ---
		if (Gdx.app.getType() == ApplicationType.Android) {
			createAndroidControls();
		}
	}

	private void createAndroidControls() {
		// Touchpad (Bottom Left)
		touchpad = new Touchpad(10, VisUI.getSkin());
		touchpad.setSize(200, 200);
		// Manual positioning for overlay feel
		touchpad.setPosition(20, 20);
		// Make it semi-transparent
		touchpad.setColor(1f, 1f, 1f, 0.7f);
		stage.addActor(touchpad);

		// Action Buttons (Bottom Right)
		VisTable actionTable = new VisTable();
		actionTable.bottom().right();

		// Interact Button (E)
		interactBtn = new VisTextButton("交互"); // Interact
		interactBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				isInteractPressed = true;
				return true;
			}
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				isInteractPressed = false;
			}
		});

		// Attack/Use Button (Space)
		attackBtn = new VisTextButton("动作"); // Action
		attackBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				isAttackPressed = true;
				return true;
			}
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				isAttackPressed = false;
			}
		});

		// Layout buttons
		// [Interact]
		//        [Attack]
		// Stacked or side-by-side?
		// Ideally: Attack is big, Interact is smaller nearby.

		// Let's use absolute positioning for better ergonomic control on mobile
		float screenW = stage.getWidth();

		attackBtn.setSize(120, 120);
		attackBtn.setPosition(screenW - 140, 40);
		attackBtn.setColor(1f, 1f, 1f, 0.7f);
		stage.addActor(attackBtn);

		interactBtn.setSize(90, 90);
		interactBtn.setPosition(screenW - 140 - 100, 20); // To the left of Attack
		interactBtn.setColor(1f, 1f, 1f, 0.7f);
		stage.addActor(interactBtn);
	}

	private void initStyles() {
		// Can be used to init bar styles if shared
	}

	private VisTable createPlayerHud() {
		VisTable hud = new VisTable();

		// 1. Avatar Area
		Stack avatarStack = new Stack();

		// Avatar Image
		// Use TextureManager to get the shared "PLAYER" texture which is dynamically updated
		TextureRegion avatarTex = TextureManager.getInstance().get("PLAYER");
		if (avatarTex == null) {
			// Fallback if not loaded yet
			avatarTex = new TextureRegion(SpriteGenerator.createAvatar());
		}
		// Crop initial avatar
		TextureRegion headRegion = new TextureRegion(avatarTex, 88, 10, 80, 80);
		avatarImage = new VisImage(new TextureRegionDrawable(headRegion));

		// Frame (Simple border)
		VisTable frame = new VisTable();
		frame.add(avatarImage).size(80, 80);

		// Level Badge
		levelBadge = new VisLabel("Lv1");
		levelBadge.setColor(Color.YELLOW);
		levelBadge.setFontScale(0.5f);
		VisTable container = new VisTable();
		VisTable badgeTable = new VisTable();
		container.add(badgeTable).size(30, 20);
//		badgeTable.setBackground("window-noborder");
		badgeTable.bottom().right();
		badgeTable.add(levelBadge).pad(2);

		avatarStack.add(frame);
		avatarStack.add(container);

		hud.add(avatarStack).size(150, 150).padRight(15);

		// 2. Bars Area
		VisTable barsTable = new VisTable();

		// HP Bar
		SkewBar.BarStyle hpStyle = new SkewBar.BarStyle();
		hpStyle.gradientStart = Color.valueOf("ff5252"); // Red
		hpStyle.gradientEnd = Color.valueOf("b71c1c");   // Dark Red
		hpStyle.skewDeg = -20f;
		hpBar = new SkewBar(0, 100, hpStyle);
		hpLabel = new VisLabel("100/100");
		hpLabel.setAlignment(Align.center);
		barsTable.add(createBarWithLabel(hpBar, hpLabel, 200, 20)).padBottom(5).row();

		// Mana Bar
		SkewBar.BarStyle mpStyle = new SkewBar.BarStyle();
		mpStyle.gradientStart = Color.valueOf("40c4ff"); // Light Blue
		mpStyle.gradientEnd = Color.valueOf("01579b");   // Dark Blue
		mpStyle.skewDeg = -20f;
		manaBar = new SkewBar(0, 50, mpStyle);
		manaLabel = new VisLabel("50/50");
		manaLabel.setAlignment(Align.center);
		barsTable.add(createBarWithLabel(manaBar, manaLabel, 200, 20)).padBottom(5).row();

		// XP Bar
		SkewBar.BarStyle xpStyle = new SkewBar.BarStyle();
		xpStyle.gradientStart = Color.valueOf("ffd740"); // Amber
		xpStyle.gradientEnd = Color.valueOf("ff6f00");   // Dark Amber
		xpStyle.skewDeg = -20f;
		xpBar = new SkewBar(0, 100, xpStyle);
		xpLabel = new VisLabel("0/100");
		xpLabel.setAlignment(Align.center);
		barsTable.add(createBarWithLabel(xpBar, xpLabel, 200, 15)).row();

		hud.add(barsTable);

		// Quick Slots
		VisTable quickTable = new VisTable();
		hpQuickSlot = new QuickSlot((item) -> item.data == ItemData.Health_Potion || item.data == ItemData.Elixir);
		mpQuickSlot = new QuickSlot((item) -> item.data == ItemData.Mana_Potion);

		quickTable.add(hpQuickSlot).size(64,64).pad(5);
		quickTable.add(mpQuickSlot).size(64,64).pad(5);

		hud.add(quickTable).padLeft(20);

		// Floor info
		floorLabel = new VisLabel("Floor 1");
		hud.add(floorLabel).padLeft(20);

		return hud;
	}

	private Actor createBarWithLabel(SkewBar bar, VisLabel label, float width, float height) {
		Stack stack = new Stack();
		bar.setSize(width, height);
		stack.add(bar);

		Table labelTable = new Table();
		labelTable.add(label).center();
		labelTable.setTouchable(Touchable.disabled);
		stack.add(labelTable);

		return stack;
	}

	private VisLabel monsterHpLabel;

	private void createMonsterInfo() {
		monsterInfoTable = new VisTable();
		monsterInfoTable.setBackground(logBgDrawable); // Reuse semi-transparent bg
		monsterInfoTable.setVisible(false);
		monsterInfoTable.setTouchable(Touchable.disabled);

		// Layout
		// [Head] [Name Lv.X]
		//        [HP Bar]

		VisTable stackContainer = new VisTable();
		Stack headStack = new Stack();
		monsterHead = new VisImage();
		headStack.add(monsterHead);

		monsterLvLabel = new VisLabel("1");
		monsterLvLabel.setColor(Color.CYAN);
		monsterLvLabel.setFontScale(0.3f);
		VisTable badgeTable = new VisTable();
		badgeTable.bottom().right();
		badgeTable.add(monsterLvLabel).pad(0);
		headStack.add(badgeTable);

		stackContainer.add(headStack);
		monsterInfoTable.add(stackContainer).size(48, 48).left().pad(5);

		VisTable info = new VisTable();
		monsterNameLabel = new VisLabel("Monster");

		VisTable nameRow = new VisTable();
		nameRow.add(monsterNameLabel).left().padRight(10);
		// monsterLvLabel moved to head
		info.add(nameRow).left().row();

		SkewBar.BarStyle mobHpStyle = new SkewBar.BarStyle();
		mobHpStyle.gradientStart = Color.valueOf("ff5252");
		mobHpStyle.gradientEnd = Color.valueOf("b71c1c");
		mobHpStyle.skewDeg = -15f;
		monsterHpBar = new SkewBar(0, 100, mobHpStyle);
		monsterHpLabel = new VisLabel("100/100");
		monsterHpLabel.setFontScale(0.4f);

		info.add(createBarWithLabel(monsterHpBar, monsterHpLabel, 120, 15)).left();

		monsterInfoTable.add(info).pad(5);

		// Position handled in showMonsterInfo or update
//		monsterInfoTable.pack();
		monsterInfoTable.pad(10);
	}

	private void createPauseLabel() {
		pauseLabel = new VisLabel("PAUSED");
		pauseLabel.setColor(Color.YELLOW);
		pauseLabel.setFontScale(2.0f);
		pauseLabel.setVisible(false);

		VisTable pauseTable = new VisTable();
		pauseTable.setFillParent(true);
		pauseTable.center();
		pauseTable.add(pauseLabel);
	}

	private void createButtons(VisTable container) {
		inventoryBtn = new VisTextButton("背包");
		inventoryBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				toggleInventory();
				return true;
			}
		});
		container.add(inventoryBtn).pad(5);

		VisTextButton campBtn = new VisTextButton("回城");
		campBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (returnToCampListener != null) returnToCampListener.run();
				return true;
			}
		});
		container.add(campBtn).pad(5);

		saveBtn = new VisTextButton("保存");
		saveBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (saveListener != null) saveListener.run();
				return true;
			}
		});
		container.add(saveBtn).pad(5);

		helpBtn = new VisTextButton("帮助");
		helpBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				showHelp();
				return true;
			}
		});
		container.add(helpBtn).pad(5);

		VisTextButton pauseBtn = new VisTextButton("暂停");
		pauseBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				isPaused = !isPaused;
				setPaused(isPaused);
				return true;
			}
		});
		container.add(pauseBtn).pad(5);
	}

	private void generateAssets() {
		// 1. Slot Background
		Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		pixmap.setColor(0.2f, 0.2f, 0.2f, 0.8f);
		pixmap.fillRectangle(2, 2, 60, 60);
		pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
		pixmap.drawRectangle(0, 0, 64, 64);
		pixmap.drawRectangle(1, 1, 62, 62);
		slotBgTexture = new Texture(pixmap);
		slotBgDrawable = new NinePatchDrawable(new NinePatch(slotBgTexture, 4, 4, 4, 4));
		pixmap.dispose();

		// 2. Equipped Border
		Pixmap borderPm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		borderPm.setColor(1f, 0.8f, 0f, 1f);
		for(int i=0; i<4; i++) borderPm.drawRectangle(i, i, 64-i*2, 64-i*2);
		slotBorderTexture = new Texture(borderPm);
		slotBorderDrawable = new NinePatchDrawable(new NinePatch(slotBorderTexture, 5, 5, 5, 5));
		borderPm.dispose();

		// 3. White pixel
		Pixmap whitePm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		whitePm.setColor(Color.WHITE);
		whitePm.fill();
		whiteTexture = new Texture(whitePm);
		whiteDrawable = new TextureRegionDrawable(whiteTexture);
		whitePm.dispose();

		// 4. Log Background (Semi-transparent black)
		Pixmap logPm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		logPm.setColor(0f, 0f, 0f, 0.5f);
		logPm.fill();
		Texture logTex = new Texture(logPm);
		logBgDrawable = new TextureRegionDrawable(logTex);
		logPm.dispose();
	}

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

		helpTable.add("游戏说明").center().padBottom(15).row();
		String helpMsg = "移动/攻击: WASD 或方向键\n撞击怪物会自动攻击\n技能: SPACE 键使用治疗技能\n下一关: 找到楼梯并踩上去\n物品: 踩上去自动拾取，背包中点击装备\n存档: 点击保存按钮保存游戏进度\n点击怪物查看详情";
		VisLabel helpLabel = new VisLabel(helpMsg);
		helpLabel.setWrap(true);
		helpTable.add(helpLabel).minWidth(0).grow().left();

		VisScrollPane scrollPane = new VisScrollPane(helpTable);
		scrollPane.setFlickScroll(true);
		scrollPane.setScrollingDisabled(true, false);

		helpWindow.getContentTable().add(scrollPane).expand().fill();
	}

	private void showHelp() {
		helpWindow.show(stage);
	}

	public void update(Player player, int floor) {
		this.currentPlayer = player;

		// Update Quick Slots
		if (hpQuickSlot != null) hpQuickSlot.update(player);
		if (mpQuickSlot != null) mpQuickSlot.update(player);

		// Update Bars (Fix Range Bug)
		hpBar.setRange(0, player.stats.maxHp);
		hpBar.setValue(player.stats.hp);
		hpLabel.setText(player.stats.hp + "/" + player.stats.maxHp);

		manaBar.setRange(0, player.stats.maxMana);
		manaBar.setValue(player.stats.mana);
		manaLabel.setText(player.stats.mana + "/" + player.stats.maxMana);

		xpBar.setRange(0, player.stats.xpToNextLevel);
		xpBar.setValue(player.stats.xp);
		xpLabel.setText(player.stats.xp + "/" + player.stats.xpToNextLevel);

		levelBadge.setText("Lv"+String.valueOf(player.stats.level));
		floorLabel.setText("层数: " + floor);
		coinLabel.setText("金币: " + player.coins);

		// Update HUD Avatar (Top Left)
		// We want to use the same texture as the player entity ("PLAYER")
		// The Player entity updates "PLAYER" texture in TextureManager when equipment changes.
		// So we just fetch it.
		TextureRegion playerTex = TextureManager.getInstance().get("PLAYER");
		if (playerTex != null) {
			// Crop to Head (Center X=128, Top Y=10-20. Head W=76, H=64)
			// Let's take a square region around the head.
			// X: 128 - 40 = 88. W=80.
			// Y: 10. H=80.
			TextureRegion headRegion = new TextureRegion(playerTex, 88, 10, 80, 80);
			avatarImage.setDrawable(new TextureRegionDrawable(headRegion));
		}

		// Update Monster Info if visible
		if (monsterInfoTable.isVisible() && currentTargetMonster != null) {
			if (currentTargetMonster.hp <= 0) {
				hideMonsterInfo();
			} else {
				// Ensure range is up to date (in case of buffs?)
				monsterHpBar.setRange(0, currentTargetMonster.maxHp);
				monsterHpBar.setValue(currentTargetMonster.hp);
				monsterHpLabel.setText(currentTargetMonster.hp + "/" + currentTargetMonster.maxHp);
			}
		}

		stage.act();
	}

	public void showMonsterInfo(Monster monster) {
		this.currentTargetMonster = monster;
		monsterNameLabel.setText(monster.name);
		// Estimate level or use simple calc
		int estLevel = 1 + (monster.maxHp / 20);
		monsterLvLabel.setText(String.valueOf(estLevel));

		// Update Visuals
		TextureRegion monsterTex = TextureManager.getInstance().getMonster(monster.type.name());
		if (monsterTex != null) {
			monsterHead.setDrawable(new TextureRegionDrawable(monsterTex));
		}

		monsterInfoTable.setVisible(true);
		monsterInfoTable.pack(); // Ensure size is correct

		// Position at Top Right (UI Viewport)
		// x = right edge - width - padding
		// y = top edge - toolbar height - height - padding
		float padding = 20;
		float toolbarHeight = 50; // Approx or calculate
		monsterInfoTable.setPosition(
			stage.getWidth() - monsterInfoTable.getWidth() - padding - 20,
			stage.getHeight() - monsterInfoTable.getHeight() - padding - 60 // offset from toolbar
		);

		// Set initial value INSTANTLY
		monsterHpBar.getStyle().skewDeg = 15;
		monsterHpBar.setRange(0, monster.maxHp);
		monsterHpBar.setInstantValue(monster.hp);
		monsterHpLabel.setText(monster.hp + "/" + monster.maxHp);
	}

	public void hideMonsterInfo() {
		monsterInfoTable.setVisible(false);
		currentTargetMonster = null;
	}

	public void reset() {
		if (inventoryDialog.getParent() != null) inventoryDialog.remove();
		if (helpWindow.getParent() != null) helpWindow.remove();
		hideMonsterInfo();
		logMessages.clear();
		msgLabel.setText("");
		setPaused(false);
	}

	public void showMessage(String msg) {
		logMessages.add(msg);
		if (logMessages.size() > 8) {
			logMessages.remove(0);
		}
		msgLabel.setText(String.join("\n", logMessages));
	}

	public void showGameOver(Runnable onRestart, Runnable onQuit) {
		new GameOverWindow(onRestart, onQuit).show(stage);
	}

	public void setPaused(boolean paused) {
		if (pauseLabel != null) {
			pauseLabel.setVisible(paused);
		}
	}

	public void toggleInventory() {
		if (currentPlayer != null) {
			updateInventory(currentPlayer);
		}
		if(stage.getActors().contains(inventoryDialog, true)){
			inventoryDialog.remove();
		}
		else{
			inventoryDialog.show(stage);
		}
	}

	public void updateInventoryDialog(Player player) {
		updateInventory(player);
	}

	public Vector2 getMovementDirection() {
		if (touchpad == null) return new Vector2(0,0);
		return new Vector2(touchpad.getKnobPercentX(), touchpad.getKnobPercentY());
	}

	public boolean isAttackPressed() {
		return isAttackPressed;
	}

	public boolean isInteractPressed() {
		return isInteractPressed;
	}

	public void setSaveListener(Runnable saveListener) {
		this.saveListener = saveListener;
	}

	public void setReturnToCampListener(Runnable listener) {
		this.returnToCampListener = listener;
	}

	public void showLevelSelection(int maxDepth, Consumer<Integer> onSelect) {
		BaseDialog dialog = new BaseDialog("选择层数");
		dialog.addCloseButton();

		VisTable content = new VisTable();
		content.top().left();

		// Grid of buttons
		for (int i = 1; i <= maxDepth; i++) {
			final int level = i;
			VisTextButton btn = new VisTextButton("第 " + level + " 层");
			btn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					onSelect.accept(level);
					dialog.fadeOut();
				}
			});
			content.add(btn).size(80, 40).pad(5);
			if (i % 4 == 0) content.row();
		}

		VisScrollPane scroll = new VisScrollPane(content);
		scroll.setFlickScroll(true);
		scroll.setFadeScrollBars(false);

		dialog.add(scroll).size(400, 300);
		dialog.show(stage);
	}

	public void updateInventory(Player player) {
		if (player == null) return;

		// 1. Update Equipment Panel (Left Column)
		if (equipmentTable != null) {
			equipmentTable.clear();

			// Layout:
			//      [Helm]
			//   [Acc] [Acc] [Acc]
			// [Main] Avatar [Off]
			//      [Armor]
			//      [Boots]

			// Top: Helmet
			equipmentTable.add(new EquipmentSlot(player.equipment.helmet, "头盔", player, ItemType.HELMET, -1, dragAndDrop)).colspan(3).padBottom(5).row();

			// Accessories (Moved to below Helmet)
			VisTable accTable = new VisTable();
			for(int i=0; i<3; i++) {
				InventoryItem item = (player.equipment.accessories != null && i < player.equipment.accessories.length) ? player.equipment.accessories[i] : null;
				accTable.add(new EquipmentSlot(item, "饰品", player, ItemType.ACCESSORY, i, dragAndDrop)).pad(2);
			}
			equipmentTable.add(accTable).colspan(3).padBottom(5).row();

			// Middle: Main, Avatar, Off
			equipmentTable.add(new EquipmentSlot(player.equipment.mainHand, "主手", player, ItemType.MAIN_HAND, -1, dragAndDrop)).padRight(10);
			if (avatarWidget != null) {
				equipmentTable.add(avatarWidget).size(128, 128); // Avatar
			} else {
				equipmentTable.add().size(128, 128);
			}
			equipmentTable.add(new EquipmentSlot(player.equipment.offHand, "副手", player, ItemType.OFF_HAND, -1, dragAndDrop)).padLeft(10).row();

			// Bottom: Armor
			equipmentTable.add(new EquipmentSlot(player.equipment.armor, "铠甲", player, ItemType.ARMOR, -1, dragAndDrop)).colspan(3).padTop(5).row();

			// Feet: Boots
			equipmentTable.add(new EquipmentSlot(player.equipment.boots, "鞋子", player, ItemType.BOOTS, -1, dragAndDrop)).colspan(3).padTop(5).row();
		}

		// 2. Update Stats
		if (statsTable != null) {
			statsTable.clear();
			statsTable.setBackground(slotBorderDrawable); // Add border
			statsTable.pad(10); // Add padding inside border

			// Helper for small text
			float fontScale = 0.25f;

			// Layout: 2 Columns with Separator
			// [Left Table] | [Right Table]
			
			VisTable container = new VisTable();
			
			// Left Table (Level, HP, MP, Atk)
			VisTable leftStats = new VisTable();
			leftStats.defaults().left().padBottom(5);
			
			VisLabel lvlLabel = new VisLabel("等级: " + player.stats.level);
			lvlLabel.setFontScale(fontScale);
			leftStats.add(lvlLabel).row();
			
			VisLabel hpLabel = new VisLabel("生命: " + player.stats.hp + "/" + player.stats.maxHp);
			hpLabel.setFontScale(fontScale);
			leftStats.add(hpLabel).row();
			
			VisLabel mpLabel = new VisLabel("魔法: " + player.stats.mana + "/" + player.stats.maxMana);
			mpLabel.setFontScale(fontScale);
			leftStats.add(mpLabel).row();
			
			VisLabel atkLabel = new VisLabel("攻击: " + player.stats.atk);
			atkLabel.setFontScale(fontScale);
			leftStats.add(atkLabel).row();
			
			container.add(leftStats).padRight(15).top();
			
			// Vertical Separator
			container.add(new Separator("vertical")).growY().padRight(15);
			
			// Right Table (XP, HP Regen, MP Regen, Def)
			VisTable rightStats = new VisTable();
			rightStats.defaults().left().padBottom(5);
			
			VisLabel xpLabel = new VisLabel("经验: " + player.stats.xp + "/" + player.stats.xpToNextLevel);
			xpLabel.setFontScale(fontScale);
			rightStats.add(xpLabel).row();
			
			VisLabel hpRegenLabel = new VisLabel("自然回血: " + player.stats.hpRegen + "/5s");
			hpRegenLabel.setFontScale(fontScale);
			rightStats.add(hpRegenLabel).row();
			
			VisLabel mpRegenLabel = new VisLabel("自然回魔: " + player.stats.manaRegen + "/5s");
			mpRegenLabel.setFontScale(fontScale);
			rightStats.add(mpRegenLabel).row();
			
			VisLabel defLabel = new VisLabel("防御: " + player.stats.def);
			defLabel.setFontScale(fontScale);
			rightStats.add(defLabel).row();
			
			container.add(rightStats).top();
			
			statsTable.add(container).grow();
		}

		// 3. Update Avatar
		if (avatarWidget != null) {
			avatarWidget.update(player);
		}

		// 4. Update Inventory List (Right Column)
		inventoryList.clear();

		// Collect display items (ALL items, even equipped ones)
		List<InventoryItem> displayItems = new ArrayList<>();
		if (player.inventory != null) {
			displayItems.addAll(player.inventory);
		}

		int maxSlots = Constants.MAX_INVENTORY_SLOTS;
		int itemsPerRow = 8;

		for (int i = 0; i < maxSlots; i++) {
			InventoryItem item = (i < displayItems.size()) ? displayItems.get(i) : null;
			InventorySlot slot = new InventorySlot(item, player, dragAndDrop);
			inventoryList.add(slot).size(64, 64).pad(5);
			if ((i + 1) % itemsPerRow == 0) inventoryList.row();
		}
	}

	private boolean checkIsEquipped(Player player, InventoryItem item) {
		if (player.equipment.mainHand != null && player.equipment.mainHand.id.equals(item.id)) return true;
		if (player.equipment.offHand != null && player.equipment.offHand.id.equals(item.id)) return true;
		if (player.equipment.helmet != null && player.equipment.helmet.id.equals(item.id)) return true;
		if (player.equipment.armor != null && player.equipment.armor.id.equals(item.id)) return true;
		if (player.equipment.boots != null && player.equipment.boots.id.equals(item.id)) return true;
		if (player.equipment.accessories != null) {
			for (InventoryItem acc : player.equipment.accessories) {
				if (acc != null && acc.id.equals(item.id)) return true;
			}
		}
		return false;
	}

	public void render() {
		stage.draw();
	}

	public void dispose() {
		stage.dispose();
		if (slotBgTexture != null) slotBgTexture.dispose();
		if (slotBorderTexture != null) slotBorderTexture.dispose();
		if (whiteTexture != null) whiteTexture.dispose();
		if (logBgDrawable != null) logBgDrawable.getRegion().getTexture().dispose();
		// Avatar texture is managed by SpriteGenerator? No, createAvatar creates new Texture.
		// Should dispose avatarImage texture if possible.
	}

	// Accessor for MonsterHPBar to set range (will add method to SkewBar)
	public SkewBar getMonsterHpBar() {
		return monsterHpBar;
	}
}
