package com.goldsprite.gdengine.netcode;

import java.lang.reflect.Field;

/**
 * 所有联机业务逻辑的基类 (类似 Unity 的 NetworkBehaviour)。
 * 当它被挂载到 NetworkObject 后，会自动通过反射将其带有的 NetworkVariable 字段注册给 NetworkObject 进行管理。
 */
public abstract class NetworkBehaviour {

    private NetworkObject networkObject;

    public void internalAttach(NetworkObject parentObject) {
        this.networkObject = parentObject;
        autoRegisterNetworkVariables();
    }

    public NetworkObject getNetworkObject() {
        return networkObject;
    }

    public boolean isServer() {
        return networkObject != null && networkObject.isServer;
    }

    public boolean isClient() {
        return networkObject != null && networkObject.isClient;
    }

    public boolean isLocalPlayer() {
        return networkObject != null && networkObject.isLocalPlayer;
    }

    /**
     * 利用反射自动收集当前业务逻辑子类中定义的所有 NetworkVariable，
     * 并将其托管给上级的 NetworkObject。
     */
    private void autoRegisterNetworkVariables() {
        Class<?> clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            if (NetworkVariable.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                try {
                    NetworkVariable<?> var = (NetworkVariable<?>) f.get(this);
                    if (var != null) {
                        networkObject.registerVariable(var);
                    }
                } catch (IllegalAccessException e) {
                    System.err.println("[NetworkBehaviour] 反射访问 NetworkVariable 失败: " + f.getName());
                }
            }
        }
    }
}
