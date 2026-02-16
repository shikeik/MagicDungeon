package com.goldsprite.neonskel.logic;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.neonskel.data.NeonBone;
import com.goldsprite.neonskel.data.NeonSlot;
import com.goldsprite.neonskel.render.BoneSkin;

import java.util.HashMap;
import java.util.Map;

public class NeonSkeleton {
	public final NeonBone rootBone;
	private final Map<String, NeonBone> boneMap = new HashMap<>();
	private final Array<NeonSlot> drawOrder = new Array<>();

	public float x, y, rotation, scaleX = 1f, scaleY = 1f;

	public NeonSkeleton() {
		this.rootBone = new NeonBone("root", 0);
		boneMap.put("root", rootBone);
	}

	public NeonBone createBone(String name, String parentName, float length, BoneSkin defaultSkin) {
		NeonBone bone = new NeonBone(name, length);

		NeonBone parent = boneMap.get(parentName);
		if (parent != null) {
			parent.addChild(bone);
		} else if (!name.equals("root")) {
			rootBone.addChild(bone);
		}
		boneMap.put(name, bone);

		NeonSlot slot = new NeonSlot(name, bone, defaultSkin);
		drawOrder.add(slot);

		return bone;
	}

	public NeonBone getBone(String name) {
		return boneMap.get(name);
	}

	public NeonSlot getSlot(String name) {
		for(NeonSlot slot : drawOrder) {
			if(slot.name.equals(name)) return slot;
		}
		return null;
	}

	public void updateWorldTransform() {
		// Apply skeleton transform to root bone
		rootBone.x = this.x;
		rootBone.y = this.y;
		rootBone.rotation = this.rotation;
		rootBone.scaleX = this.scaleX;
		rootBone.scaleY = this.scaleY;

		rootBone.updateWorldTransform();
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Array<NeonSlot> getDrawOrder() {
		return drawOrder;
	}
}
