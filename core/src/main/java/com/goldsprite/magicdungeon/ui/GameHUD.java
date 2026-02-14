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
import com.goldsprite.magicdungeon.core.screens.MainMenuScreen;
import com.goldsprite.magicdungeon.entities.*;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;
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
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

import java.util.function.Predicate;
import static com.goldsprite.magicdungeon.core.screens.GameScreen.isPaused;

import com.goldsprite.magicdungeon.utils.Constants;

import com.badlogic.gdx.utils.Array;

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
	private ChestDialog chestDialog;

	private Array<VisTextButton> toolbarButtons = new Array<>();
	private int toolbarFocusIndex = -1;
	private boolean isToolbarFocused = false;

	// Android Controls
	// Deprecated
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
	private TextureRegionDrawable circleDrawable; // New for controller icons

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

	// 抽象接口：可聚焦的 UI 元素
	interface FocusableUI {
		void setFocused(boolean focused);
		void simulateClick();
		// 用于计算导航位置 (简单起见，使用 Actor 坐标，或者 Grid 坐标)
		// 这里我们简化处理，InventoryDialog 维护两个列表
	}

	private class InventoryDialog extends BaseDialog {
		// Navigation State
		enum FocusArea { EQUIPMENT, INVENTORY }
		FocusArea currentArea = FocusArea.INVENTORY;

		// Slot Lists
		List<InventorySlot> inventorySlots = new ArrayList<>();
		List<EquipmentSlot> equipmentSlots = new ArrayList<>();

		int currentInvIndex = 0;
		int currentEquipIndex = 0;

		int invItemsPerRow = 8;

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

			// ... (keep existing layout code) ...

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
			// Modified: Adjusted ratio to 30:70, accounting for padding (Total pad: 20*2 + 10 = 50)
			float availableWidth = width - 50;
			mainTable.add(leftCol).width(availableWidth * 0.3f).growY().padRight(10);
			mainTable.add(rightCol).width(availableWidth * 0.7f).growY();

			getContentTable().add(mainTable).grow();
		}

		@Override
		public boolean remove() {
			GameHUD.this.hideTooltip();
			return super.remove();
		}

		private void navigate(int dx, int dy) {
			if (currentArea == FocusArea.INVENTORY) {
				if (dx == 1) currentInvIndex++;
				if (dx == -1) {
					if (currentInvIndex % invItemsPerRow == 0) {
						// Left edge -> Switch to Equipment
						currentArea = FocusArea.EQUIPMENT;
						updateFocus();
						return;
					}
					currentInvIndex--;
				}
				if (dy == 1) currentInvIndex -= invItemsPerRow; // Up
				if (dy == -1) currentInvIndex += invItemsPerRow; // Down

				// Clamp Inventory
				if (currentInvIndex < 0) currentInvIndex = 0;
				if (inventorySlots.size() > 0) {
					if (currentInvIndex >= inventorySlots.size()) currentInvIndex = inventorySlots.size() - 1;
				} else {
					currentInvIndex = 0;
				}

			} else {
				// Equipment Navigation (Manual Logic based on layout)
				int newIndex = currentEquipIndex;

				if (dy == 1) { // Up
					if (newIndex == 4 || newIndex == 5) newIndex = 1; // Main/Off -> Acc
					else if (newIndex == 6) newIndex = 4; // Armor -> Main
					else if (newIndex == 7) newIndex = 6; // Boots -> Armor
					else if (newIndex >= 1 && newIndex <= 3) newIndex = 0; // Acc -> Helm
				}
				else if (dy == -1) { // Down
					if (newIndex == 0) newIndex = 1; // Helm -> Acc
					else if (newIndex >= 1 && newIndex <= 3) newIndex = 4; // Acc -> Main
					else if (newIndex == 4 || newIndex == 5) newIndex = 6; // Main/Off -> Armor
					else if (newIndex == 6) newIndex = 7; // Armor -> Boots
				}
				else if (dx == 1) { // Right
					if (newIndex == 0) newIndex = 1;      // Helm -> Acc1
					else if (newIndex == 1) newIndex = 2; // Acc1 -> Acc2
					else if (newIndex == 2) newIndex = 3; // Acc2 -> Acc3
					else if (newIndex == 3) newIndex = 4; // Acc3 -> Main
					else if (newIndex == 4) newIndex = 5; // Main -> Off
					else if (newIndex == 5) newIndex = 6; // Off -> Armor
					else if (newIndex == 6) newIndex = 7; // Armor -> Boots
					else if (newIndex == 7) {
						// Boots -> Inventory (Only last item exits)
						currentArea = FocusArea.INVENTORY;
						currentInvIndex = 0;
						updateFocus();
						return;
					}
				}
				else if (dx == -1) { // Left
					if (newIndex == 3) newIndex = 2;
					else if (newIndex == 2) newIndex = 1;
					else if (newIndex == 5) newIndex = 4;
				}

				currentEquipIndex = newIndex;
				// Clamp just in case
				if (equipmentSlots.size() > 0) {
					if (currentEquipIndex < 0) currentEquipIndex = 0;
					if (currentEquipIndex >= equipmentSlots.size()) currentEquipIndex = equipmentSlots.size() - 1;
				} else {
					currentEquipIndex = 0;
				}
			}
			updateFocus();
		}

		public void clearFocus() {
			for (int i = 0; i < inventorySlots.size(); i++) {
				inventorySlots.get(i).setFocused(false);
			}
			for (int i = 0; i < equipmentSlots.size(); i++) {
				equipmentSlots.get(i).setFocused(false);
			}
		}

		@Override
		public void act(float delta) {
			super.act(delta);

			// Handle Input Navigation
			InputManager input = InputManager.getInstance();

			if (input.isJustPressed(InputAction.UI_RIGHT)) {
				navigate(1, 0);
			}
			if (input.isJustPressed(InputAction.UI_LEFT)) {
				navigate(-1, 0);
			}
			if (input.isJustPressed(InputAction.UI_UP)) {
				navigate(0, 1);
			}
			if (input.isJustPressed(InputAction.UI_DOWN)) {
				navigate(0, -1);
			}

			// Tab Navigation (Switch Area)
			if (input.isJustPressed(InputAction.TAB)) {
				if (currentArea == FocusArea.INVENTORY) {
					currentArea = FocusArea.EQUIPMENT;
				} else {
					currentArea = FocusArea.INVENTORY;
				}
				updateFocus();
			}

			// Confirm Action
			if (input.isJustPressed(InputAction.UI_CONFIRM)) {
				if (currentArea == FocusArea.INVENTORY) {
					if (currentInvIndex >= 0 && currentInvIndex < inventorySlots.size()) {
						inventorySlots.get(currentInvIndex).simulateClick();
					}
				} else {
					if (currentEquipIndex >= 0 && currentEquipIndex < equipmentSlots.size()) {
						equipmentSlots.get(currentEquipIndex).simulateClick();
					}
				}
			}

			// Cancel / Close
			if (input.isJustPressed(InputAction.UI_CANCEL)) {
				hide();
			}
		}

		public void updateFocus() {
			for (int i = 0; i < inventorySlots.size(); i++) {
				inventorySlots.get(i).setFocused(currentArea == FocusArea.INVENTORY && i == currentInvIndex);
			}
			for (int i = 0; i < equipmentSlots.size(); i++) {
				equipmentSlots.get(i).setFocused(currentArea == FocusArea.EQUIPMENT && i == currentEquipIndex);
			}
		}

		@Override
		public VisDialog show(Stage stage) {
			super.show(stage);
			// Reset selection on open
			currentArea = FocusArea.INVENTORY;
			currentInvIndex = 0;
			currentEquipIndex = 0;
			// Delay focus update to ensure layout is ready for tooltip positioning
			Gdx.app.postRunnable(this::updateFocus);
			return this;
		}
	}

	Color fColor = new Color(0f, 0.8f, 1f, 1f);
	private class InventorySlot extends VisTable implements FocusableUI {
		private Chest chestContext;
		private ChestDialog dialogContext;
		private boolean isChestItem;
		private InventoryItem item; // Stored reference
		private boolean isEquipped; // Stored reference

		private VisImage focusBorder;
		private ClickListener clickListener;

		public InventorySlot(InventoryItem item, Player player, DragAndDrop dragAndDrop) {
			this(item, player, dragAndDrop, null, null, false);
		}

		public InventorySlot(InventoryItem item, Player player, DragAndDrop dragAndDrop, Chest chest, ChestDialog dialog, boolean isChestItem) {
			this.item = item;
			this.chestContext = chest;
			this.dialogContext = dialog;
			this.isChestItem = isChestItem;
			this.isEquipped = (item != null) && checkIsEquipped(player, item);

			boolean isEquipped = this.isEquipped; // Local for anonymous classes (redundant but keeps existing code working)

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
					badge.setFontScale(0.7f);
					VisTable badgeTable = new VisTable();
					badgeTable.top().right();
					badgeTable.add(badge).pad(1);
					badgeTable.setTouchable(Touchable.disabled);
					stack.add(badgeTable);
				}

				// Count Badge
				if (item.count > 1) {
					VisLabel countLabel = new VisLabel(String.valueOf(item.count));
					countLabel.setFontScale(0.9f);
					VisTable countTable = new VisTable();
					countTable.bottom().right();
					countTable.add(countLabel).pad(2);
					countTable.setTouchable(Touchable.disabled);
					stack.add(countTable);
				}
			}

			// Focus Border
			if (whiteDrawable != null) {
				focusBorder = new VisImage(whiteDrawable);
				focusBorder.setColor(fColor); // Semi-transparent yellow
				focusBorder.setTouchable(Touchable.disabled);
				focusBorder.setVisible(false);
				stack.addActorAt(1, focusBorder);
			}

			add(stack).size(64, 64);

			if (Gdx.app.getType() == ApplicationType.Android) {
				if (item != null) {
					ActorGestureListener listener = new ActorGestureListener() {
						@Override
						public void tap(InputEvent event, float x, float y, int count, int button) {
							if (chestContext != null) {
								handleChestTransaction(item, isChestItem, dialogContext, chestContext, player);
							} else {
								handleEquipAction(item, isEquipped, player);
							}
							hideTooltip(); // 确保点击时清除可能残留的 tooltip
						}

						@Override
						public boolean longPress(Actor actor, float x, float y) {
							// showAndroidTooltip(InventorySlot.this, item, isEquipped);
							if (chestContext == null) {
								Vector2 pos = actor.localToStageCoordinates(new Vector2(x, y));
								showContextMenu(item, isEquipped, player, pos.x, pos.y);
							}
							return true;
						}

						@Override
						public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
							super.pan(event, x, y, deltaX, deltaY);
							hideTooltip();
						}

						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							super.touchUp(event, x, y, pointer, button);
							// 长按后松开，或者点击后松开，都尝试隐藏
							hideTooltip();
						}
					};
					listener.getGestureDetector().setLongPressSeconds(0.18f); // 缩短长按时间
					addListener(listener);

					// Store ref for simulation (simplified)
					this.clickListener = new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							listener.tap(event, x, y, 1, 0);
						}
					};
				}
			} else {
				if (item != null) {
					// PC Click Listener
					this.clickListener = new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							hideTooltip(); // Added fix
							if (chestContext != null) {
								handleChestTransaction(item, isChestItem, dialogContext, chestContext, player);
							} else {
								// Original behavior
								handleEquipAction(item, isEquipped, player);
							}
						}
					};
					addListener(clickListener);

                    // Mouse Hover Tooltip
                    addListener(new InputListener() {
                        @Override
                        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                            if (pointer == -1 && InputManager.getInstance().getInputMode() == InputManager.InputMode.MOUSE) {
                                showTooltip(InventorySlot.this, item);
                            }
                        }
                        @Override
                        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                             if (pointer == -1 && InputManager.getInstance().getInputMode() == InputManager.InputMode.MOUSE) {
                                hideTooltip();
                            }
                        }
                    });

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
								if (chestContext == null) {
									showContextMenu(item, isEquipped, player, event.getStageX(), event.getStageY());
								} else {
									// Right click in chest = same as left click (transfer)
									handleChestTransaction(item, isChestItem, dialogContext, chestContext, player);
								}
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

			// Configure Drag Target (For Chest Interaction)
			if (dragAndDrop != null && chestContext != null) {
				dragAndDrop.addTarget(new Target(this) {
					@Override
					public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
						return true;
					}

					@Override
					public void drop(Source source, Payload payload, float x, float y, int pointer) {
						Object obj = payload.getObject();
						if (obj instanceof InventoryItem) {
							InventoryItem droppedItem = (InventoryItem) obj;

							boolean itemInInventory = player.inventory.contains(droppedItem);
							boolean itemInChest = chestContext.items.contains(droppedItem);

							if (isChestItem && itemInInventory) {
								// Store (Drop on Chest Slot from Inventory)
								handleChestTransaction(droppedItem, false, dialogContext, chestContext, player);
							} else if (!isChestItem && itemInChest) {
								// Loot (Drop on Inventory Slot from Chest)
								handleChestTransaction(droppedItem, true, dialogContext, chestContext, player);
							} else if (isChestItem && itemInChest) {
								// Reorder in Chest? (Not implemented yet)
							} else if (!isChestItem && itemInInventory) {
								// Reorder in Inventory? (Not implemented yet)
							}
						}
					}
				});
			}
		}

		public void setFocused(boolean focused) {
			boolean actualFocus = focused && InputManager.getInstance().getInputMode() == InputManager.InputMode.KEYBOARD;

			if (focusBorder != null) {
				focusBorder.setVisible(actualFocus);
				if (actualFocus) {
					focusBorder.setColor(fColor); // Lower alpha for background highlight
				}
			}

			if (actualFocus && item != null) {
				showTooltip(this, item);
			} else if (actualFocus) {
				// Focused but empty slot -> hide tooltip
				hideTooltip();
			}
		}

		public void simulateClick() {
			if (clickListener != null) {
				// Trigger the click logic manually
				InputEvent event = new InputEvent();
				event.setType(InputEvent.Type.touchDown);
				event.setButton(Input.Buttons.LEFT);
				event.setStageX(getX());
				event.setStageY(getY());
				clickListener.clicked(event, 0, 0);
			}
		}
	}

	private class EquipmentSlot extends VisTable implements FocusableUI {
		private VisImage focusBorder;
		private ClickListener clickListener;
		private InventoryItem item; // Store item reference

		public EquipmentSlot(InventoryItem item, String placeholder, Player player, ItemType slotType, int slotIndex, DragAndDrop dragAndDrop) {
			this.item = item;
			setBackground(slotBgDrawable);

			Stack stack = new Stack();

			// Background / Placeholder
			VisTable bg = new VisTable();
			if (item == null) {
				VisLabel label = new VisLabel(placeholder);
				label.setFontScale(0.9f);
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
							hideTooltip();
							// Android tap acts as click (Unequip)
							player.equip(item, slotIndex);
							updateInventory(player);
						}
						@Override
						public boolean longPress(Actor actor, float x, float y) {
							// showAndroidTooltip(EquipmentSlot.this, item, true);
							Vector2 pos = actor.localToStageCoordinates(new Vector2(x, y));
							showContextMenu(item, true, player, pos.x, pos.y);
							return true;
						}

						@Override
						public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
							super.pan(event, x, y, deltaX, deltaY);
							hideTooltip();
						}

						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							super.touchUp(event, x, y, pointer, button);
							// hideAndroidTooltip(); // Don't hide on touchUp if it's long press menu
							// Context menu handles its own closing usually, but focus tooltip should be hidden
							hideTooltip();
						}
					};
					listener.getGestureDetector().setLongPressSeconds(0.18f);
					addListener(listener);
				} else {
                    // Mouse Hover Tooltip
                    addListener(new InputListener() {
                        @Override
                        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                            if (pointer == -1 && InputManager.getInstance().getInputMode() == InputManager.InputMode.MOUSE) {
                                showTooltip(EquipmentSlot.this, item);
                            }
                        }
                        @Override
                        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                             if (pointer == -1 && InputManager.getInstance().getInputMode() == InputManager.InputMode.MOUSE) {
                                hideTooltip();
                            }
                        }
                    });
				}

				// Click to Unequip (Standard click behavior)
				this.clickListener = new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						hideTooltip(); // Added fix
						player.equip(item, slotIndex);
						updateInventory(player);
					}
				};
				addListener(clickListener);
			}

			// Focus Border
			if (whiteDrawable != null) {
				focusBorder = new VisImage(whiteDrawable);
				focusBorder.setColor(fColor); // Semi-transparent yellow
				focusBorder.setTouchable(Touchable.disabled);
				focusBorder.setVisible(false);
				stack.addActorAt(1, focusBorder);
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

		@Override
		public void setFocused(boolean focused) {
            boolean actualFocus = focused && InputManager.getInstance().getInputMode() == InputManager.InputMode.KEYBOARD;

			if (focusBorder != null) {
				focusBorder.setVisible(actualFocus);
				if (actualFocus) focusBorder.setColor(fColor);
			}

			// Show Tooltip on Focus
			if (actualFocus && item != null) {
				showTooltip(this, item);
			} else if (actualFocus) {
				hideTooltip();
			}
		}

		@Override
		public void simulateClick() {
			if (clickListener != null) {
				InputEvent event = new InputEvent();
				event.setType(InputEvent.Type.touchDown);
				event.setButton(Input.Buttons.LEFT);
				event.setStageX(getX());
				event.setStageY(getY());
				clickListener.clicked(event, 0, 0);
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
			countLabel.setFontScale(0.9f);
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
				// showMessage("没有可用的物品");
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

			// Add Item Info Header (Android)
			if (Gdx.app.getType() == ApplicationType.Android) {
				VisTable infoTable = createItemTooltipTable(item, isEquipped);
				// Scale down slightly if needed, or keep as is. Tooltip table width is about 200-300.
				MenuItem headerItem = new MenuItem("");
				headerItem.setTouchable(Touchable.disabled); // Info only
				headerItem.add(infoTable).grow().pad(5).row();
				menu.addItem(headerItem);
				menu.addSeparator();
			}

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
					MenuItem quickSlot1 = new MenuItem("设为快捷键1 (HP)", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							if (hpQuickSlot != null) {
								hpQuickSlot.setTargetItem(item.data);
								showMessage("快捷键1 已设置为: " + item.data.name);
							}
						}
					});
					menu.addItem(quickSlot1);

					MenuItem quickSlot2 = new MenuItem("设为快捷键2 (MP)", new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							if (mpQuickSlot != null) {
								mpQuickSlot.setTargetItem(item.data);
								showMessage("快捷键2 已设置为: " + item.data.name);
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

			// Android: Ensure menu is fully visible
			if (Gdx.app.getType() == ApplicationType.Android) {
				menu.pack();
				float menuW = menu.getWidth();
				float menuH = menu.getHeight();

				float newX = x;
				float newY = y;

				if (newX + menuW > stage.getWidth()) newX = stage.getWidth() - menuW - 10;
				if (newX < 0) newX = 10;

				if (newY - menuH < 0) {
					// Show above if not enough space below (Wait, menu usually shows below cursor)
					// VisUI PopupMenu shows at (x, y) with top-left corner usually? Or aligns?
					// Standard PopupMenu behavior: tries to fit.
					// Let's manually clamp just in case.
					newY = menuH + 10;
				}

				menu.setPosition(newX, newY);
			}
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

		// === 布局重构：左右分布 ===
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
			rightCol.add(new VisLabel("回魔: +" + item.manaRegen)).left().padBottom(2).row();
		} else if (item.data == ItemData.Elixir) {
			rightCol.add(new VisLabel("回血: +" + item.heal)).left().padBottom(2).row();
			rightCol.add(new VisLabel("回魔: +" + item.manaRegen)).left().padBottom(2).row();
		} else {
			String hpRegenLabel = item.data.type == ItemType.POTION ? "回血: +" : "自然回血: +";
			String mpRegenLabel = item.data.type == ItemType.POTION ? "回魔: +" : "自然回魔: +";

			if (item.heal > 0) rightCol.add(new VisLabel(hpRegenLabel + item.heal)).left().padBottom(2).row();
			if (item.manaRegen > 0) rightCol.add(new VisLabel(mpRegenLabel + item.manaRegen)).left().padBottom(2).row();
		}

		// 6. 状态(已装备)
		if (isEquipped) {
			VisLabel status = new VisLabel("已装备");
			status.setColor(Color.YELLOW);
			rightCol.add(status).left().padTop(8).row();
		}

		// 7. 价值(最底部)
		VisLabel valueLabel = new VisLabel("价值 " + item.getValue() + " 硬币");
		valueLabel.setColor(Color.GOLD);
		// 字体大小与其他一致，不进行缩放
		rightCol.add(valueLabel).left().padTop(5).row();
		return borderTable;
	}

	private InventoryItem getEquippedItemForComparison(InventoryItem item) {
		if (currentPlayer == null || currentPlayer.equipment == null) return null;
		if (checkIsEquipped(currentPlayer, item)) return null;

		switch (item.data.type) {
			case MAIN_HAND: return currentPlayer.equipment.mainHand;
			case OFF_HAND: return currentPlayer.equipment.offHand;
			case HELMET: return currentPlayer.equipment.helmet;
			case ARMOR: return currentPlayer.equipment.armor;
			case BOOTS: return currentPlayer.equipment.boots;
			case ACCESSORY:
				if (currentPlayer.equipment.accessories != null) {
					for (InventoryItem acc : currentPlayer.equipment.accessories) {
						if (acc != null) return acc;
					}
				}
				return null;
			default: return null;
		}
	}

	public void showTooltip(Actor target, InventoryItem item) {
		hideTooltip(); // Clear previous if any
		if (item == null) return;

		InventoryItem equipped = getEquippedItemForComparison(item);
		Actor tooltipActor;

		if (equipped != null && equipped != item) {
			VisTable container = new VisTable();

			// Equipped (Left)
			VisTable equippedTable = createItemTooltipTable(equipped, true);
			// Add "(Equipped)" label to the bottom of the tooltip
			equippedTable.row();
			equippedTable.add(new VisLabel("【已装备】", Color.YELLOW)).pad(2);

			container.add(equippedTable).top().padRight(10);

			// New Item (Right)
			VisTable newTable = createItemTooltipTable(item, false);
			container.add(newTable).top();

			tooltipActor = container;
		} else {
			boolean isEquipped = checkIsEquipped(currentPlayer, item);
			tooltipActor = createItemTooltipTable(item, isEquipped);
		}

		tooltipActor.setTouchable(Touchable.disabled);
		stage.addActor(tooltipActor);

		if (tooltipActor instanceof Layout) ((Layout)tooltipActor).pack();
		currentTooltip = tooltipActor;

		float x = 0;
		float y = 0;

		if (InputManager.getInstance().getInputMode() == InputManager.InputMode.KEYBOARD) {
			// Keyboard: Fixed position next to target
			Vector2 pos = target.localToStageCoordinates(new Vector2(0, 0));
			x = pos.x + target.getWidth() + 10; // Right side
			y = pos.y + target.getHeight() - tooltipActor.getHeight(); // Align tops

			// Check Right Boundary
			if (x + tooltipActor.getWidth() > stage.getWidth()) {
				// Flip to Left side
				x = pos.x - tooltipActor.getWidth() - 10;
			}

			// Clamp Y
			if (y < 0) y = 0;
			if (y + tooltipActor.getHeight() > stage.getHeight()) y = stage.getHeight() - tooltipActor.getHeight();

			tooltipActor.setPosition(x, y);
		} else {
			// Mouse: Follow mouse
			updateTooltipPosition();
		}
	}

    private void updateTooltipPosition() {
        if (currentTooltip == null) return;

        // [Refactor] Use GScreen's built-in coordinate conversion
        float x = Gdx.input.getX();
        float y = Gdx.input.getY();

		// Fallback (Should not happen in normal game loop)
		Vector2 mousePos = stage.screenToStageCoordinates(new Vector2(x, y));
		x = mousePos.x;
		y = mousePos.y;

        x += 15;
        y -= currentTooltip.getHeight(); // Align Top to Mouse (Y is up in Stage)

        // Check Right Boundary
        if (x + currentTooltip.getWidth() > stage.getWidth()) {
            x = stage.getWidth() - currentTooltip.getWidth() - 10;
        }

        // Check Bottom Boundary (Flip to top if not enough space below)
        if (y < 10) {
            y = y + currentTooltip.getHeight() + 15; // Move above cursor
        }

        // Check Top Boundary
        if (y + currentTooltip.getHeight() > stage.getHeight()) {
             y = stage.getHeight() - currentTooltip.getHeight() - 10;
        }

        currentTooltip.setPosition(x, y);
    }

	public void hideTooltip() {
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

	private GScreen parentScreen;

	public GameHUD(GScreen parentScreen) {
		this.parentScreen = parentScreen;
		Tooltip.DEFAULT_APPEAR_DELAY_TIME = 0.2f;

		// Coin info
		coinLabel = new VisLabel("Coins: 0");

		// Use NeonBatch for SkewBar support
		this.neonBatch = new NeonBatch();
		this.stage = new Stage(parentScreen.getUIViewport(), this.neonBatch);

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
		msgLabel = new VisLabel("欢迎来到地下城", "small");
		msgLabel.setFontScale(0.8f);
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
		// Moved to VirtualKeyboard system
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
		levelBadge.setFontScale(0.95f);
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
		monsterLvLabel.setFontScale(0.78f);
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
		monsterHpLabel.setFontScale(0.9f);

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
		toolbarButtons.clear();

		inventoryBtn = new VisTextButton("背包");
		inventoryBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				toggleInventory();
				return true;
			}
		});
		container.add(inventoryBtn).pad(5);
		toolbarButtons.add(inventoryBtn);

		VisTextButton campBtn = new VisTextButton("回城");
		campBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (returnToCampListener != null) returnToCampListener.run();
				return true;
			}
		});
		container.add(campBtn).pad(5);
		toolbarButtons.add(campBtn);

		saveBtn = new VisTextButton("保存");
		saveBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (saveListener != null) saveListener.run();
				return true;
			}
		});
		container.add(saveBtn).pad(5);
		toolbarButtons.add(saveBtn);

		helpBtn = new VisTextButton("帮助");
		helpBtn.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				showHelp();
				return true;
			}
		});
		container.add(helpBtn).pad(5);
		toolbarButtons.add(helpBtn);

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
		toolbarButtons.add(pauseBtn);
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

		// 5. Circle Background (for Controller Buttons)
		Pixmap circlePm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
		circlePm.setColor(Color.LIGHT_GRAY);
		circlePm.fillCircle(16, 16, 16);
		circlePm.setColor(0.2f, 0.2f, 0.2f, 1f);
		circlePm.fillCircle(16, 16, 14); // Inner dark
		Texture circleTex = new Texture(circlePm);
		circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		circleDrawable = new TextureRegionDrawable(circleTex);
		circlePm.dispose();
	}

	private InputManager.InputMode lastInputMode = InputManager.InputMode.MOUSE;

	private void createHelpWindow() {
		// BaseDialog 内部已经调用了 addCloseButton() 和 closeOnEscape()
		// 我们不需要手动添加关闭按钮，也不需要手动检查 ESC，除非 BaseDialog 的实现有问题。
		// 为了保险起见，我们添加一个 Listener 来处理 ESC，覆盖默认行为。
		helpWindow = new BaseDialog("帮助");

		helpWindow.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
					helpWindow.hide();
					return true;
				}
				return false;
			}
		});

		helpWindow.autoPack = false;
		helpWindow.setSize(900, 600); // Increased size
		helpWindow.setCenterOnAdd(true);
		helpWindow.setMovable(true);
		helpWindow.setResizable(false);
	}

	private void updateHelpWindowContent() {
		helpWindow.getContentTable().clear();

		VisTable content = new VisTable();
		content.top().left().pad(20);

		// Left: Text Guide
		VisTable leftCol = new VisTable();
		leftCol.top().left();
		leftCol.add(new VisLabel("游戏指南")).center().padBottom(10).row();

		// Dynamic Controls Guide
		boolean isController = InputManager.getInstance().isUsingController();

		VisTable keysTable = new VisTable();
		keysTable.defaults().left().padBottom(8);

		addHelpRow(keysTable, "移动", InputAction.MOVE_UP, isController);
		addHelpRow(keysTable, "攻击", InputAction.ATTACK, isController);
		addHelpRow(keysTable, "交互", InputAction.INTERACT, isController);
		addHelpRow(keysTable, "技能", InputAction.SKILL, isController);
		addHelpRow(keysTable, "背包", InputAction.BAG, isController);
		addHelpRow(keysTable, "地图", InputAction.MAP, isController);
		addHelpRow(keysTable, "暂停", InputAction.PAUSE, isController);
		addHelpRow(keysTable, "切换区域", InputAction.TAB, isController);
		addHelpRow(keysTable, "快捷键", InputAction.QUICK_SLOT, isController);

		leftCol.add(keysTable).left().padBottom(20).row();

		VisLabel extraLabel = new VisLabel("【基本操作】\n" +
				"移动: WASD 或 方向键 (移动或 左下角摇杆)\n" +
				"攻击: 撞击怪物自动攻击\n" +
				"交互: SPACE 空格键 (移动端 交互按钮) - 下楼/上楼/进关卡\n" +
				"技能: H 键 (移动端 动作按钮) - 使用治疗药 (消耗魔法)\n" +
				"\n" +
				"【物品与装备】\n" +
				"拾取: 移动到物品上自动拾取\n" +
				"宝箱: 撞击宝箱打开战利品界面\n" +
				"背包: 按 E 键或点击背包按钮打开，点击物品进行装备/卸下/使用\n" +
				"\n" +
				"【其他】\n" +
				"存档: F5 或点击保存按钮\n" +
				"读档: F9 (仅限PC调试)\n" +
				"查看信息: 点击怪物或长按物品查看详情");
		extraLabel.setWrap(true);
		extraLabel.setFontScale(0.78f);
		extraLabel.setColor(Color.LIGHT_GRAY);
		leftCol.add(extraLabel).width(400).left();

		// Right: Item Guide
		VisTable rightCol = new VisTable();
		rightCol.top().left();
		rightCol.add(new VisLabel("物品图鉴")).center().padBottom(10).row();

		VisTable itemGrid = new VisTable();
		int itemsPerRow = 6;
		int i = 0;
		for (ItemData data : ItemData.values()) {
			boolean discovered = currentPlayer != null && currentPlayer.discoveredItems.contains(data.name());

			// Create Icon
			InventoryItem dummy = new InventoryItem(data);
			dummy.quality = ItemQuality.COMMON;

			VisTable icon = ItemRenderer.createItemIcon(dummy, 48);

			VisTable slot = new VisTable();
			slot.setBackground(slotBgDrawable);

			if (discovered) {
				slot.add(icon).size(48, 48);
				// Tooltip
				if (Gdx.app.getType() != ApplicationType.Android) {
					VisTable tooltip = createItemTooltipTable(dummy, false);
					new Tooltip.Builder(tooltip).target(slot).build();
				}
			} else {
				icon.setColor(0.2f, 0.2f, 0.2f, 1f); // Dark
				slot.add(icon).size(48, 48);
			}

			itemGrid.add(slot).size(56, 56).pad(4);

			i++;
			if (i % itemsPerRow == 0) itemGrid.row();
		}
		rightCol.add(itemGrid).top().left();

		// Layout
		content.add(leftCol).top().left().width(450).padRight(20);
		content.add(new Separator("vertical")).growY().pad(10);
		content.add(rightCol).top().left().growX();

		VisScrollPane scroll = new VisScrollPane(content);
		scroll.setScrollingDisabled(true, false);
		scroll.setFadeScrollBars(false);

		helpWindow.getContentTable().add(scroll).grow();
	}

	private void addHelpRow(VisTable table, String actionName, InputAction action, boolean isController) {
		table.add(new VisLabel(actionName)).left().width(80);

		if (action == InputAction.MOVE_UP) {
			if (isController) {
				table.add(createKeyIcon("LS", true)).padRight(5);
				table.add(new VisLabel("/")).padRight(5);
				table.add(createKeyIcon("DPad", true)).left();
			} else {
				table.add(createKeyIcon("W", false)).padRight(2);
				table.add(createKeyIcon("A", false)).padRight(2);
				table.add(createKeyIcon("S", false)).padRight(2);
				table.add(createKeyIcon("D", false)).padRight(5);
				table.add(new VisLabel("或")).padRight(5);
				table.add(createKeyIcon("↑↓←→", false)).left();
			}
		} else {
			InputManager input = InputManager.getInstance();
			if (isController) {
				int btn = input.getBoundButton(action);
				if (btn != -1) {
					table.add(createKeyIcon(getButtonName(btn), true)).left();
				} else {
					table.add(new VisLabel("-")).left();
				}
			} else {
				int key = input.getBoundKey(action);
				if (key != -1) {
					String keyName = Input.Keys.toString(key);
					table.add(createKeyIcon(keyName, false)).left();
				} else {
					table.add(new VisLabel("-")).left();
				}
			}
		}
		table.row();
	}

	private Actor createKeyIcon(String text, boolean isController) {
		VisTable t = new VisTable();
		// Use circle for controller, box for keyboard
		t.setBackground(isController ? circleDrawable : slotBgDrawable);

		VisLabel l = new VisLabel(text);
		l.setAlignment(Align.center);

		// Adaptive font scale
		if (text.length() > 2) l.setFontScale(0.78f);
		else l.setFontScale(0.9f);

		// Increase size for better visibility
		float minSize = 50;

		if (isController) {
			// Controller icons (Circle/Pill)
			if (text.length() > 2) {
				t.add(l).pad(10, 15, 10, 15).minSize(minSize, minSize);
			} else {
				t.add(l).center().size(minSize, minSize);
			}
		} else {
			// Keyboard icons (Box)
			if (text.length() > 1) {
				t.add(l).pad(10, 15, 10, 15).minSize(minSize, minSize);
			} else {
				t.add(l).center().size(minSize, minSize);
			}
		}

		return t;
	}

	private String getButtonName(int code) {
		// Standard LibGDX Controller Mappings (Xbox-like)
		switch(code) {
			case 0: return "A";
			case 1: return "B";
			case 2: return "X";
			case 3: return "Y";
			case 4: return "LB";
			case 5: return "RB";
			case 6: return "Back";
			case 7: return "Start";
			case 8: return "L3";
			case 9: return "R3";
			case 10: return "Up";
			case 11: return "Down";
			case 12: return "Left";
			case 13: return "Right";
			default: return String.valueOf(code);
		}
	}

	private void showHelp() {
		updateHelpWindowContent();
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

	public void showChestDialog(Chest chest, Player player) {
		if (chestDialog != null) chestDialog.remove();
		chestDialog = new ChestDialog(chest, player);
		chestDialog.show(stage);
	}

	public void updateInventoryDialog(Player player) {
		updateInventory(player);
	}

	// Deprecated Android Control Methods (Keep empty to avoid break changes if called externally)
	public Vector2 getMovementDirection() {
		return new Vector2(0,0);
	}

	public boolean isAttackPressed() {
		return false;
	}

	public boolean isInteractPressed() {
		return false;
	}

	public boolean hasModalUI() {
		return stage.getActors().contains(inventoryDialog, true) ||
			   stage.getActors().contains(chestDialog, true) ||
			   stage.getActors().contains(helpWindow, true);
	}

	public boolean handleBackKey() {
		// 1. 优先检查并关闭顶层窗口 (倒序遍历)
		for (int i = stage.getActors().size - 1; i >= 0; i--) {
			Actor actor = stage.getActors().get(i);
			if (!actor.isVisible()) continue;

			// 忽略 GameOver 窗口 (必须通过按钮操作)
			if (actor instanceof GameOverWindow) {
				return true; // 拦截 Escape，防止退出屏幕
			}

			// 关闭 PopupMenu
			if (actor instanceof PopupMenu) {
				((PopupMenu) actor).remove();
				return true;
			}

			// 关闭通用窗口 (Inventory, Chest, Help, Settings, LevelSelection 等)
			if (actor instanceof VisWindow) {
				((VisWindow) actor).fadeOut();
				return true;
			}
		}

		// 2. 如果没有窗口可关闭，尝试打开暂停菜单
		// 如果暂停菜单尚未实现，至少拦截 Escape 防止意外退出 (或者让它退出，取决于设计)
		// 根据用户反馈 "习惯性 Escape... 直接返回了上一屏幕"，这里应该拦截
		// 我们可以显示一个暂停菜单，或者 Toast 提示 "再按一次退出" (需额外状态)

		// 暂时先显示暂停菜单 (如果存在) 或拦截
		if (pauseWindow == null) {
			createPauseWindow();
		}
		if (!pauseWindow.hasParent()) {
			pauseWindow.show(stage);
			return true;
		}

		return false; // 只有当暂停菜单已经在显示时 (被上面循环捕获关闭)，才返回 false?
		// 不，如果暂停菜单在显示，上面的循环会捕获并关闭它 (VisWindow)，返回 true。
		// 所以这里只有当没有窗口时才会执行。
		// 意味着：按 Esc -> 打开暂停菜单。再按 Esc -> 关闭暂停菜单。
	}

	private BaseDialog pauseWindow;

	private void createPauseWindow() {
		pauseWindow = new BaseDialog("暂停");
		pauseWindow.setModal(true);

		VisTable content = new VisTable();
		content.pad(20);

		VisTextButton btnResume = new VisTextButton("继续游戏");
		btnResume.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				pauseWindow.fadeOut();
			}
		});

		VisTextButton btnSettings = new VisTextButton("设置");
		btnSettings.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				new com.goldsprite.magicdungeon.core.ui.SettingsDialog().show(stage);
				// pauseWindow.fadeOut(); // 可选：关闭暂停菜单，或者保持在 Settings 下面
			}
		});

		VisTextButton btnExit = new VisTextButton("退出至标题");
		btnExit.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// Fallback
				ScreenManager.getInstance().playTransition(() -> ScreenManager.getInstance().popLastScreen());
			}
		});

		content.add(btnResume).width(150).padBottom(10).row();
		content.add(btnSettings).width(150).padBottom(10).row();
		content.add(btnExit).width(150).row();

		pauseWindow.add(content);
		pauseWindow.pack();
		pauseWindow.centerWindow();
	}


	public void setSaveListener(Runnable saveListener) {
		this.saveListener = saveListener;
	}

	public void setReturnToCampListener(Runnable listener) {
		this.returnToCampListener = listener;
	}

	public void showLevelSelection(int maxDepth, Consumer<Integer> onSelect) {
		BaseDialog dialog = new BaseDialog("选择层数");
		dialog.autoPack = true; // Ensure dialog is packed to fit content

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
		hideTooltip(); // Added fix for orphan tooltips
		if (player == null) return;

		if (inventoryDialog != null) {
			inventoryDialog.inventorySlots.clear();
			inventoryDialog.equipmentSlots.clear();
		}

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
			EquipmentSlot helmSlot = new EquipmentSlot(player.equipment.helmet, "头盔", player, ItemType.HELMET, -1, dragAndDrop);
			equipmentTable.add(helmSlot).colspan(3).padBottom(5).row();
			if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(helmSlot);

			// Accessories (Moved to below Helmet)
			VisTable accTable = new VisTable();
			for(int i=0; i<3; i++) {
				InventoryItem item = (player.equipment.accessories != null && i < player.equipment.accessories.length) ? player.equipment.accessories[i] : null;
				EquipmentSlot accSlot = new EquipmentSlot(item, "饰品", player, ItemType.ACCESSORY, i, dragAndDrop);
				accTable.add(accSlot).pad(2);
				if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(accSlot);
			}
			equipmentTable.add(accTable).colspan(3).padBottom(5).row();

			// Middle: Main, Avatar, Off
			EquipmentSlot mainSlot = new EquipmentSlot(player.equipment.mainHand, "主手", player, ItemType.MAIN_HAND, -1, dragAndDrop);
			equipmentTable.add(mainSlot).padRight(10);
			if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(mainSlot);

			if (avatarWidget != null) {
				equipmentTable.add(avatarWidget).size(128, 128); // Avatar
			} else {
				equipmentTable.add().size(128, 128);
			}

			EquipmentSlot offSlot = new EquipmentSlot(player.equipment.offHand, "副手", player, ItemType.OFF_HAND, -1, dragAndDrop);
			equipmentTable.add(offSlot).padLeft(10).row();
			if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(offSlot);

			// Bottom: Armor
			EquipmentSlot armorSlot = new EquipmentSlot(player.equipment.armor, "铠甲", player, ItemType.ARMOR, -1, dragAndDrop);
			equipmentTable.add(armorSlot).colspan(3).padTop(5).row();
			if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(armorSlot);

			// Feet: Boots
			EquipmentSlot bootsSlot = new EquipmentSlot(player.equipment.boots, "鞋子", player, ItemType.BOOTS, -1, dragAndDrop);
			equipmentTable.add(bootsSlot).colspan(3).padTop(5).row();
			if (inventoryDialog != null) inventoryDialog.equipmentSlots.add(bootsSlot);
		}

		// 2. Update Stats
		if (statsTable != null) {
			statsTable.clear();
			statsTable.setBackground(slotBorderDrawable); // Add border
			statsTable.pad(10); // Add padding inside border

			// Helper for small text
			float fontScale = 0.7f;

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
		// if (inventoryDialog != null) inventoryDialog.slotActors.clear();

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
			if (inventoryDialog != null) inventoryDialog.inventorySlots.add(slot);
			if ((i + 1) % itemsPerRow == 0) inventoryList.row();
		}

		if (inventoryDialog != null) {
			// Only update focus if dialog is actually visible
			if (inventoryDialog.getParent() != null) {
				inventoryDialog.updateFocus();
			} else {
				// If not visible, just ensure no residual tooltips from previous states
				// Although setFocused shouldn't be called if not in updateFocus...
				// But let's be safe.
			}
		}
	}

	private void handleChestTransaction(InventoryItem item, boolean isChestItem, ChestDialog dialog, Chest chest, Player player) {
		if (item == null) return;

		if (isChestItem) {
			// Loot: Chest -> Player
			if (player.addItem(item)) {
				chest.items.remove(item);
				showMessage("获得了 " + item.data.name);
				dialog.updateContent();
				updateInventory(player);
			} else {
				showMessage("背包已满!");
			}
		} else {
			// Store: Player -> Chest
			chest.addItem(item);
			unequipItem(player, item);
			player.inventory.remove(item);
			dialog.updateContent();
			updateInventory(player);
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

	private void handleToolbarInput() {
		if (hasModalUI()) return;

		InputManager input = InputManager.getInstance();

		// Toggle focus with TAB
		if (input.isJustPressed(InputAction.TAB)) {
			isToolbarFocused = !isToolbarFocused;
			if (isToolbarFocused) {
				if (toolbarFocusIndex == -1) toolbarFocusIndex = 0;
			} else {
				toolbarFocusIndex = -1;
			}
			updateToolbarVisuals();
		}

		if (isToolbarFocused) {
			if (input.isJustPressed(InputAction.UI_RIGHT)) {
				toolbarFocusIndex++;
				if (toolbarFocusIndex >= toolbarButtons.size) toolbarFocusIndex = 0;
				updateToolbarVisuals();
			} else if (input.isJustPressed(InputAction.UI_LEFT)) {
				toolbarFocusIndex--;
				if (toolbarFocusIndex < 0) toolbarFocusIndex = toolbarButtons.size - 1;
				updateToolbarVisuals();
			} else if (input.isJustPressed(InputAction.UI_CONFIRM)) {
				if (toolbarFocusIndex >= 0 && toolbarFocusIndex < toolbarButtons.size) {
					VisTextButton btn = toolbarButtons.get(toolbarFocusIndex);
					InputEvent event = new InputEvent();
					event.setType(InputEvent.Type.touchDown);
					btn.fire(event);
					event.setType(InputEvent.Type.touchUp);
					btn.fire(event);
				}
			} else if (input.isJustPressed(InputAction.UI_CANCEL)) {
				isToolbarFocused = false;
				toolbarFocusIndex = -1;
				updateToolbarVisuals();
			}
		}
	}

	private void updateToolbarVisuals() {
		for(int i=0; i<toolbarButtons.size; i++) {
			VisTextButton btn = toolbarButtons.get(i);
			if (i == toolbarFocusIndex && isToolbarFocused) {
				btn.setColor(Color.YELLOW);
			} else {
				btn.setColor(Color.WHITE);
			}
		}
	}

	public void render() {
		// Sync with Global InputMode
		InputManager.InputMode currentMode = InputManager.getInstance().getInputMode();
		if (currentMode != lastInputMode) {
			lastInputMode = currentMode;
			onInputModeChanged(currentMode);
		}

        // Tooltip follow mouse
        if (currentTooltip != null && currentMode == InputManager.InputMode.MOUSE) {
            updateTooltipPosition();
        }

		handleToolbarInput();

		stage.draw();
	}

	private void onInputModeChanged(InputManager.InputMode newMode) {
		if (newMode == InputManager.InputMode.KEYBOARD) {
			if (inventoryDialog != null && inventoryDialog.getParent() != null) {
				inventoryDialog.updateFocus();

				// Ensure focus if none (Fix for controller stuck without focus)
				if (inventoryDialog.currentArea == InventoryDialog.FocusArea.INVENTORY && inventoryDialog.currentInvIndex < 0) {
					 inventoryDialog.currentInvIndex = 0;
					 inventoryDialog.updateFocus();
				}
			}
			if (chestDialog != null && chestDialog.getParent() != null) {
				chestDialog.updateFocus();
			}
		} else {
			if (inventoryDialog != null) {
				inventoryDialog.clearFocus();
			}
			if (chestDialog != null) {
				chestDialog.clearFocus();
			}
			hideTooltip();
		}

		// Refresh Help Window content if needed
		if (helpWindow != null) {
			updateHelpWindowContent();
			if (helpWindow.isVisible()) {
				helpWindow.centerWindow();
			}
		}

		// Center other windows
		if (inventoryDialog != null && inventoryDialog.isVisible()) {
			inventoryDialog.centerWindow();
		}
		if (chestDialog != null && chestDialog.isVisible()) {
			chestDialog.centerWindow();
		}
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		// Recenter windows if they are open
		if (helpWindow != null && helpWindow.getParent() != null) helpWindow.centerWindow();
		if (inventoryDialog != null && inventoryDialog.getParent() != null) inventoryDialog.centerWindow();
		if (chestDialog != null && chestDialog.getParent() != null) chestDialog.centerWindow();
		// Android controls handled by VirtualKeyboard now
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

	private class ChestDialog extends BaseDialog {
		private Chest chest;
		private Player player;
		private VisTable chestItemsTable;
		private VisTable playerItemsTable;

		// Navigation
		enum FocusArea { CHEST, INVENTORY }
		FocusArea currentArea = FocusArea.CHEST;
		List<InventorySlot> chestSlots = new ArrayList<>();
		List<InventorySlot> inventorySlots = new ArrayList<>();
		int currentChestIndex = 0;
		int currentInvIndex = 0;
		final int itemsPerRow = 8;

		public ChestDialog(Chest chest, Player player) {
			super("宝箱");
			this.chest = chest;
			this.player = player;

			float width = Math.max(900, stage.getWidth() * 0.8f);
			float height = stage.getHeight() * 0.8f;
			setSize(width, height);
			setCenterOnAdd(true);
			autoPack = false;

			VisTable mainTable = new VisTable();
			mainTable.setFillParent(true);
			mainTable.pad(20);

			// Left: Chest Items
			VisTable leftCol = new VisTable();
			leftCol.setBackground(slotBorderDrawable);
			leftCol.add(new VisLabel("宝箱内容")).pad(10).top().row();

			chestItemsTable = new VisTable();
			VisScrollPane chestScroll = new VisScrollPane(chestItemsTable);
			chestScroll.setScrollingDisabled(true, false);
			chestScroll.setFadeScrollBars(false);
			leftCol.add(chestScroll).grow();

			// Right: Player Inventory
			VisTable rightCol = new VisTable();
			rightCol.setBackground(slotBorderDrawable);
			rightCol.add(new VisLabel("你的背包")).pad(10).top().row();

			playerItemsTable = new VisTable();
			VisScrollPane playerScroll = new VisScrollPane(playerItemsTable);
			playerScroll.setScrollingDisabled(true, false);
			playerScroll.setFadeScrollBars(false);
			rightCol.add(playerScroll).grow();

			// Layout 50:50
			float availableWidth = width - 50; // 20*2 padding + 10 gap
			mainTable.add(leftCol).width(availableWidth * 0.5f).growY().padRight(10);
			mainTable.add(rightCol).width(availableWidth * 0.5f).growY();

			getContentTable().add(mainTable).grow();

			updateContent();
		}

		@Override
		public boolean remove() {
			GameHUD.this.hideTooltip();
			return super.remove();
		}

		public void updateContent() {
			chestSlots.clear();
			inventorySlots.clear();

			// Chest Items
			chestItemsTable.clear();
			int maxChestSlots = 16; // 16格两排容量
			for (int i = 0; i < maxChestSlots; i++) {
				InventoryItem item = (i < chest.items.size()) ? chest.items.get(i) : null;
				InventorySlot slot = new InventorySlot(item, player, dragAndDrop, chest, this, true);
				chestItemsTable.add(slot).size(64, 64).pad(5);
				chestSlots.add(slot);
				if ((i + 1) % itemsPerRow == 0) chestItemsTable.row();
			}

			// Player Inventory
			playerItemsTable.clear();
			int maxInvSlots = Constants.MAX_INVENTORY_SLOTS;
			List<InventoryItem> invItems = player.inventory;
			for (int i = 0; i < maxInvSlots; i++) {
				InventoryItem item = (i < invItems.size()) ? invItems.get(i) : null;
				InventorySlot slot = new InventorySlot(item, player, dragAndDrop, chest, this, false);
				playerItemsTable.add(slot).size(64, 64).pad(5);
				inventorySlots.add(slot);
				if ((i + 1) % itemsPerRow == 0) playerItemsTable.row();
			}

			updateFocus();
		}

		private void navigate(int dx, int dy) {
			if (currentArea == FocusArea.CHEST) {
				if (dx == 1) currentChestIndex++;
				if (dx == -1) currentChestIndex--;
				if (dy == 1) currentChestIndex -= itemsPerRow;
				if (dy == -1) currentChestIndex += itemsPerRow;

				// Clamp
				if (currentChestIndex < 0) currentChestIndex = 0;
				if (chestSlots.size() > 0) {
					if (currentChestIndex >= chestSlots.size()) currentChestIndex = chestSlots.size() - 1;
				} else {
					currentChestIndex = 0;
				}

				// Switch to Inventory if moving Right from right edge? Or Tab?
				// Let's use Tab to switch context usually, but maybe simple right/left
				// If on right edge of Chest, move to Inventory?
				// Simple: Tab switches area
			} else {
				if (dx == 1) currentInvIndex++;
				if (dx == -1) currentInvIndex--;
				if (dy == 1) currentInvIndex -= itemsPerRow;
				if (dy == -1) currentInvIndex += itemsPerRow;

				if (currentInvIndex < 0) currentInvIndex = 0;
				if (inventorySlots.size() > 0) {
					if (currentInvIndex >= inventorySlots.size()) currentInvIndex = inventorySlots.size() - 1;
				} else {
					currentInvIndex = 0;
				}
			}
			updateFocus();
		}

		public void updateFocus() {
			for (int i = 0; i < chestSlots.size(); i++) {
				chestSlots.get(i).setFocused(currentArea == FocusArea.CHEST && i == currentChestIndex);
			}
			for (int i = 0; i < inventorySlots.size(); i++) {
				inventorySlots.get(i).setFocused(currentArea == FocusArea.INVENTORY && i == currentInvIndex);
			}
		}

		public void clearFocus() {
			for (InventorySlot slot : chestSlots) slot.setFocused(false);
			for (InventorySlot slot : inventorySlots) slot.setFocused(false);
		}

		@Override
		public void act(float delta) {
			super.act(delta);

			InputManager input = InputManager.getInstance();

			if (input.isJustPressed(InputAction.UI_RIGHT)) navigate(1, 0);
			if (input.isJustPressed(InputAction.UI_LEFT)) navigate(-1, 0);
			if (input.isJustPressed(InputAction.UI_UP)) navigate(0, 1);
			if (input.isJustPressed(InputAction.UI_DOWN)) navigate(0, -1);

			if (input.isJustPressed(InputAction.TAB)) {
				if (currentArea == FocusArea.CHEST) currentArea = FocusArea.INVENTORY;
				else currentArea = FocusArea.CHEST;
				updateFocus();
			}

			if (input.isJustPressed(InputAction.UI_CONFIRM)) {
				if (currentArea == FocusArea.CHEST) {
					if (currentChestIndex < chestSlots.size()) chestSlots.get(currentChestIndex).simulateClick();
				} else {
					if (currentInvIndex < inventorySlots.size()) inventorySlots.get(currentInvIndex).simulateClick();
				}
			}

			if (input.isJustPressed(InputAction.UI_CANCEL)) {
				hide();
			}
		}

		@Override
		public VisDialog show(Stage stage) {
			super.show(stage);
			currentArea = FocusArea.CHEST;
			currentChestIndex = 0;
			currentInvIndex = 0;
			Gdx.app.postRunnable(this::updateFocus);
			return this;
		}
	}

	private void unequipItem(Player player, InventoryItem item) {
		// Helper to unequip if item is equipped
		if (checkIsEquipped(player, item)) {
			// Find which slot
			if (player.equipment.mainHand != null && player.equipment.mainHand.id.equals(item.id)) player.equipment.mainHand = null;
			else if (player.equipment.offHand != null && player.equipment.offHand.id.equals(item.id)) player.equipment.offHand = null;
			else if (player.equipment.helmet != null && player.equipment.helmet.id.equals(item.id)) player.equipment.helmet = null;
			else if (player.equipment.armor != null && player.equipment.armor.id.equals(item.id)) player.equipment.armor = null;
			else if (player.equipment.boots != null && player.equipment.boots.id.equals(item.id)) player.equipment.boots = null;
			else if (player.equipment.accessories != null) {
				for (int i = 0; i < player.equipment.accessories.length; i++) {
					if (player.equipment.accessories[i] != null && player.equipment.accessories[i].id.equals(item.id)) {
						player.equipment.accessories[i] = null;
					}
				}
			}
		}
	}
}
