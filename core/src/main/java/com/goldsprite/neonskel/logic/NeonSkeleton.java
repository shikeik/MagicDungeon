package com.goldsprite.neonskel.logic;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.neonskel.data.NeonBone;
import com.goldsprite.neonskel.data.NeonIKConstraint;
import com.goldsprite.neonskel.data.NeonSlot;
import com.goldsprite.neonskel.render.BoneSkin;

import java.util.HashMap;
import java.util.Map;

public class NeonSkeleton {
	public final NeonBone rootBone;
	private final Map<String, NeonBone> boneMap = new HashMap<>();
	private final Array<NeonSlot> drawOrder = new Array<>();
	
	// IK 约束列表
	private final Array<NeonIKConstraint> ikConstraints = new Array<>();

	public float x, y, rotation, scaleX = 1f, scaleY = 1f;
	
	// 临时向量，避免 GC
	private final Vector2 tempV1 = new Vector2();
	private final Vector2 tempV2 = new Vector2();
	private final Vector2 tempV3 = new Vector2();

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
	
	// 便捷 API: 查找骨骼 (别名)
	public NeonBone findBone(String name) {
		return boneMap.get(name);
	}

	public NeonSlot getSlot(String name) {
		for(NeonSlot slot : drawOrder) {
			if(slot.name.equals(name)) return slot;
		}
		return null;
	}
	
	public void addIKConstraint(NeonIKConstraint ik) {
		ikConstraints.add(ik);
	}

	public void updateWorldTransform() {
		// 1. Apply skeleton transform to root bone
		rootBone.x = this.x;
		rootBone.y = this.y;
		rootBone.rotation = this.rotation;
		rootBone.scaleX = this.scaleX;
		rootBone.scaleY = this.scaleY;

		// 2. Initial FK update
		rootBone.updateWorldTransform();
		
		// 3. Apply IK
		if (ikConstraints.size > 0) {
			applyIK();
			// 4. Re-update FK after IK modification
			rootBone.updateWorldTransform();
		}
	}
	
	private void applyIK() {
		for (NeonIKConstraint ik : ikConstraints) {
			if (ik.mix <= 0 || ik.target == null || ik.bones.size == 0) continue;
			
			// 1-Bone IK (LookAt)
			if (ik.bones.size == 1) {
				applyOneBoneIK(ik);
			}
			// 2-Bone IK (Analytical)
			else if (ik.bones.size == 2) {
				applyTwoBoneIK(ik);
			}
			// CCD Solver (Generic, > 2 bones)
			else {
				// 暂时只支持 1 和 2 骨骼 IK，因为最常用
				// CCD 可以作为后续扩展
			}
		}
	}
	
	private void applyOneBoneIK(NeonIKConstraint ik) {
		NeonBone bone = ik.bones.first();
		NeonBone target = ik.target;
		
		// 1. 获取 Target 的世界坐标 (相对于骨架原点更安全，但这里 worldTransform 是全局的)
		float tx = target.worldTransform.m02;
		float ty = target.worldTransform.m12;
		
		// 2. 获取 Bone 的父级世界变换 (以此将 Target 转到局部空间)
		// 如果没有父级，就用骨架原点
		float px = 0, py = 0, prot = 0;
		float pSx = 1, pSy = 1; // Parent Scale
		
		if (bone.parent != null) {
			px = bone.parent.worldTransform.m02;
			py = bone.parent.worldTransform.m12;
			// 提取旋转和缩放比较复杂，这里简化假设：
			// 我们直接计算向量角度，然后减去父级世界旋转得到局部旋转
			
			// 从矩阵提取父级世界旋转 (假设无剪切)
			prot = MathUtils.atan2(bone.parent.worldTransform.m10, bone.parent.worldTransform.m00) * MathUtils.radDeg;
			
			// 提取父级世界缩放 (近似长度)
			pSx = (float)Math.sqrt(bone.parent.worldTransform.m00 * bone.parent.worldTransform.m00 + bone.parent.worldTransform.m10 * bone.parent.worldTransform.m10);
			pSy = (float)Math.sqrt(bone.parent.worldTransform.m01 * bone.parent.worldTransform.m01 + bone.parent.worldTransform.m11 * bone.parent.worldTransform.m11);
		} else {
			// 如果是根骨骼，父级就是骨架自身变换 (已经在 rootBone.x/y/rot 体现了)
			// 但这里我们已经在 updateWorldTransform 里把骨架变换赋给 rootBone 了
			// 所以 rootBone 实际上没有逻辑上的 "parent"
		}
		
		// 3. 计算 Target 相对于 Bone 父级的角度
		// Vector Parent->Target
		float dx = tx - px;
		float dy = ty - py;
		
		// 考虑父级缩放对角度的影响 (如果是非等比缩放，atan2 结果需要修正)
		// 简单起见，假设 uniform scale 或者忽略 scale 对方向的影响
		
		float targetRotation = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
		
		// 4. 转为局部旋转
		// local = targetWorld - parentWorld
		float localRotation = targetRotation - prot;
		
		// 5. 考虑父级缩放反向 (如果 scaleX/Y 为负)
		if (pSx * pSy < 0) { // 简单判断翻转
			localRotation = -localRotation; 
			// 180度修正? 视坐标系定义而定
			localRotation += 180;
		}
		
		// 6. 归一化角度
		while (localRotation <= -180) localRotation += 360;
		while (localRotation > 180) localRotation -= 360;
		
		// 7. 应用混合
		if (ik.mix < 1.0f) {
			bone.rotation = MathUtils.lerp(bone.rotation, localRotation, ik.mix);
		} else {
			bone.rotation = localRotation;
		}
	}
	
	private void applyTwoBoneIK(NeonIKConstraint ik) {
		NeonBone child = ik.bones.get(0); // 末端 (如小腿/小臂)
		NeonBone parent = ik.bones.get(1); // 上级 (如大腿/大臂)
		NeonBone target = ik.target;
		
		// --- 1. 获取 Target 世界坐标 ---
		float tx = target.worldTransform.m02;
		float ty = target.worldTransform.m12;
		
		// --- 2. 获取 Parent 的 Parent (GrandParent) 信息 ---
		// 用于将 Target 转到 Parent 的局部空间
		float gpx = 0, gpy = 0, gprot = 0;
		float gpSx = 1, gpSy = 1;
		
		if (parent.parent != null) {
			gpx = parent.parent.worldTransform.m02;
			gpy = parent.parent.worldTransform.m12;
			gprot = MathUtils.atan2(parent.parent.worldTransform.m10, parent.parent.worldTransform.m00) * MathUtils.radDeg;
			gpSx = (float)Math.sqrt(parent.parent.worldTransform.m00 * parent.parent.worldTransform.m00 + parent.parent.worldTransform.m10 * parent.parent.worldTransform.m10);
			gpSy = (float)Math.sqrt(parent.parent.worldTransform.m01 * parent.parent.worldTransform.m01 + parent.parent.worldTransform.m11 * parent.parent.worldTransform.m11);
		}
		
		// --- 3. 余弦定理求解 ---
		// L1: Parent 长度
		// L2: Child 长度
		// LT: Parent 到 Target 的距离
		
		float l1 = parent.length;
		float l2 = child.length;
		
		// 目标点相对于 GrandParent 的坐标
		float dx = tx - gpx;
		float dy = ty - gpy;
		
		// 考虑 GrandParent 缩放修正距离 (简化: 假设 uniform scale)
		// 实际上应该做逆变换
		float lenScale = (Math.abs(gpSx) + Math.abs(gpSy)) / 2f;
		if (lenScale == 0) lenScale = 1;
		
		// 目标点距离 Parent (在 Parent 局部空间原点) 的距离
		// 此时 dx, dy 是世界坐标差，需要长度除以缩放
		float distSq = (dx * dx + dy * dy) / (lenScale * lenScale);
		float dist = (float)Math.sqrt(distSq);
		
		// 限制距离 (不能超过两骨骼之和，也不能小于差)
		// 但数学上 atan2 和 acos 会处理
		
		// Cosine Law: c^2 = a^2 + b^2 - 2ab cos(C)
		// angleA (Parent 旋转偏移)
		// angleB (Child 旋转偏移)
		
		// Parent 需要旋转的角度 alpha
		// alpha 由两部分组成: 
		// 1. GrandParent->Target 的基础角度 (baseAngle)
		// 2. 余弦定理求出的内角 A (angleA)
		
		// baseAngle = atan2(y, x)
		// 注意: dx, dy 还是世界坐标差，需要旋转回 GrandParent 的局部空间才能算纯角度
		// 或者直接用世界角度减去 gprot
		float baseAngleWorld = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
		float baseAngleLocal = baseAngleWorld - gprot;
		
		// 计算内角
		// dist^2 = l1^2 + l2^2 - 2*l1*l2*cos(B_outer) -> 求 Child 弯曲角
		// l2^2 = l1^2 + dist^2 - 2*l1*dist*cos(A) -> 求 Parent 偏移角
		
		// 限制 dist 使得三角形成立
		float maxDist = l1 + l2;
		float minDist = Math.abs(l1 - l2);
		float validDist = MathUtils.clamp(dist, minDist, maxDist);
		
		// cos(A)
		float cosA = (l1 * l1 + validDist * validDist - l2 * l2) / (2 * l1 * validDist);
		cosA = MathUtils.clamp(cosA, -1, 1);
		float angleA = (float)Math.acos(cosA) * MathUtils.radDeg;
		
		// cos(B) - Child 的外角 (180 - 内角)
		// l2^2 + l1^2 - 2*l1*l2*cos(180-B) = dist^2
		// cos(180-B) = (l1^2 + l2^2 - dist^2) / (2 * l1 * l2)
		// angleB = 180 - acos(...)
		// 或者直接算内角 C: dist^2 = l1^2 + l2^2 - 2*l1*l2*cos(C)
		float cosC = (l1 * l1 + l2 * l2 - validDist * validDist) / (2 * l1 * l2);
		cosC = MathUtils.clamp(cosC, -1, 1);
		float angleC = (float)Math.acos(cosC) * MathUtils.radDeg;
		// Child 旋转通常是相对于 Parent 的直线的偏转，所以是 180 - C (如果 C 是内角)
		// 或者如果是顺时针弯曲...
		
		// 根据 bendPositive 决定方向
		float pRot, cRot;
		if (ik.bendPositive) {
			pRot = baseAngleLocal - angleA;
			cRot = 180 - angleC; // Child 偏转
		} else {
			pRot = baseAngleLocal + angleA;
			cRot = angleC - 180;
		}
		
		// 4. 应用旋转
		// Parent
		while (pRot <= -180) pRot += 360;
		while (pRot > 180) pRot -= 360;
		
		// Child
		while (cRot <= -180) cRot += 360;
		while (cRot > 180) cRot -= 360;
		
		// 5. 混合
		if (ik.mix < 1.0f) {
			parent.rotation = MathUtils.lerp(parent.rotation, pRot, ik.mix);
			child.rotation = MathUtils.lerp(child.rotation, cRot, ik.mix);
		} else {
			parent.rotation = pRot;
			child.rotation = cRot;
		}
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Array<NeonSlot> getDrawOrder() {
		return drawOrder;
	}
}
