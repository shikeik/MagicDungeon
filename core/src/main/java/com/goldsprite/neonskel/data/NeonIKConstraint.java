package com.goldsprite.neonskel.data;

import com.badlogic.gdx.utils.Array;

/**
 * IK 约束 (Inverse Kinematics Constraint)
 * 用于控制骨骼链末端追踪目标点
 */
public class NeonIKConstraint {
    public String name;
    
    // 目标骨骼 (Target)
    public NeonBone target;
    
    // 受影响的骨骼链 (从末端往回溯)
    // bones[0] 是末端骨骼 (Effector, 如手/脚)
    // bones[1] 是上一级 (如小臂/小腿)
    // ...
    public final Array<NeonBone> bones = new Array<>();
    
    // 弯曲方向 (用于 2-Bone IK 解析解，或者 CCD 的初始偏向)
    public boolean bendPositive = true;
    
    // 混合权重 (0=完全FK, 1=完全IK)
    public float mix = 1.0f;
    
    // 迭代次数 (仅限 CCD)
    public int iterations = 10;
    
    // 容差距离 (仅限 CCD)
    public float tolerance = 1.0f;

    public NeonIKConstraint(String name) {
        this.name = name;
    }
    
    public void setTarget(NeonBone target) {
        this.target = target;
    }
    
    public void addBone(NeonBone bone) {
        bones.add(bone);
    }
}
