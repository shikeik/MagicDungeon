package com.goldsprite.gdengine.netcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 挂载在网络实体上的核心枢纽。
 * 负责收集实体下属的 NetworkVariable 的脏状态，并作为 RPC 拦截的分发站。
 */
public class NetworkObject {

    private long networkId;
    
    // 存储当前实体上所有的状态变量
    private List<NetworkVariable<?>> networkVariables = new ArrayList<>();
    
    // 权限标记，后续将交由 NetworkManager 管理填充
    public boolean isServer = false;
    public boolean isClient = false;
    public boolean isLocalPlayer = false;

    public NetworkObject(long networkId) {
        this.networkId = networkId;
    }

    public long getNetworkId() {
        return networkId;
    }

    /**
     * 业务层通过此方法将自动同步变量交给 NetworkObject 托管。
     */
    public void registerVariable(NetworkVariable<?> variable) {
        if (variable != null && !networkVariables.contains(variable)) {
            networkVariables.add(variable);
        }
    }

    /**
     * 引擎核心将每帧执行（类似于 LateUpdate）。
     * 清点旗下所有的包裹，如果有人触发异动（Dirty），则表明它必须向上级提交同步打包任务。
     * @return 当前对象上发生更改了的脏变量数量
     */
    public int countDirtyVariables() {
        int dirtyCount = 0;
        for (NetworkVariable<?> var : networkVariables) {
            if (var.isDirty()) {
                dirtyCount++;
            }
        }
        return dirtyCount;
    }

    /**
     * 打包完毕后的状态重置清场。
     */
    public void clearAllDirtyVariables() {
        for (NetworkVariable<?> var : networkVariables) {
            var.clearDirty();
        }
    }
}
