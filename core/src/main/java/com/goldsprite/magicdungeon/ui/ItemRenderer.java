package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisTable;

public class ItemRenderer {

	/**
	 * 在 Scene2D UI 中渲染物品图标 (包含品质星级)
	 * @param item 物品数据
	 * @param size 图标大小
	 * @return 包含图标和星级的 Table
	 */
	public static VisTable createItemIcon(InventoryItem item, float size) {
		VisTable container = new VisTable();
		
		// 1. Base Icon
		TextureRegion tex = TextureManager.getInstance().getItem(item.data.name());
		if (tex != null) {
			VisImage icon = new VisImage(new TextureRegionDrawable(tex));
			// 移除颜色染色，保持原图色彩
			icon.setColor(Color.WHITE);
			
			// 将图标添加到容器，并设置为指定大小
			container.add(icon).size(size, size);
			
			// 2. Quality Star (Overlay)
			TextureRegion starTex = TextureManager.getInstance().getQualityStar();
			if (starTex != null) {
				VisImage star = new VisImage(new TextureRegionDrawable(starTex));
				star.setColor(item.quality.color);
				
				// 计算星级大小 (相对于图标大小的比例)
				float starSize = size * 0.5f; // 50% of icon size
				
				// 使用 row() 和负 padding 或者 Stack 来实现叠加
				// 但这里 container 已经添加了 icon，最简单的是将 container 设为 Stack
				// 或者重新构建结构
			}
		}
		
		// 重新构建为 Stack 结构以支持叠加
		return buildStackIcon(item, size);
	}
	
	private static VisTable buildStackIcon(InventoryItem item, float size) {
		VisTable stackTable = new VisTable();
		com.badlogic.gdx.scenes.scene2d.ui.Stack stack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
		
		// Layer 1: Item Icon
		TextureRegion tex = TextureManager.getInstance().getItem(item.data.name());
		if (tex != null) {
			VisImage icon = new VisImage(new TextureRegionDrawable(tex));
			icon.setColor(Color.WHITE);
			VisTable iconLayer = new VisTable();
			iconLayer.add(icon).size(size, size);
			stack.add(iconLayer);
		}
		
		// Layer 2: Quality Star (Top Left)
		TextureRegion starTex = TextureManager.getInstance().getQualityStar();
		if (starTex != null) {
			VisImage star = new VisImage(new TextureRegionDrawable(starTex));
			star.setColor(item.quality.color);
			
			VisTable starLayer = new VisTable();
			starLayer.top().left();
			float starSize = size * 0.4f; // 40% size
			starLayer.add(star).size(starSize, starSize).pad(2);
			stack.add(starLayer);
		}
		
		stackTable.add(stack).size(size, size);
		return stackTable;
	}

	/**
	 * 在 SpriteBatch 中直接渲染物品 (用于地图渲染)
	 * @param batch SpriteBatch
	 * @param item 物品数据
	 * @param x 绘制X坐标
	 * @param y 绘制Y坐标
	 * @param size 绘制大小
	 */
	public static void drawItem(Batch batch, InventoryItem item, float x, float y, float size) {
		TextureRegion itemTex = TextureManager.getInstance().get(item.data.name());
		if (itemTex != null) {
			// Draw Item
			batch.setColor(Color.WHITE);
			batch.draw(itemTex, x, y, size, size);

			// Draw Quality Star
			TextureRegion starTex = TextureManager.getInstance().getQualityStar();
			if (starTex != null) {
				batch.setColor(item.quality.color);
				float starSize = size * 0.5f; // 50% size
				// Top-Left corner relative to item
				batch.draw(starTex, x - 2, y + size - starSize + 2, starSize, starSize);
				batch.setColor(Color.WHITE);
			}
		}
	}
}
