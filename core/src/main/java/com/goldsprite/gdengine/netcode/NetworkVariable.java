package com.goldsprite.gdengine.netcode;

import java.util.Objects;

/**
 * 自动状态同步变量 (类似 Unity NGO 的 NetworkVariable)。
 * 负责追踪数据的变化 (Dirty Flag)，只在发生改变时被动打成网络包。
 * @param <T> 内部持有的数据类型
 */
public class NetworkVariable<T> {
    private T value;
    private boolean isDirty = true; // 默认初始化时必须为 dirty，这样才能进行初始同步

    public NetworkVariable(T initialValue) {
        this.value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        // 如果值没有实质变化，不要触发网络同步
        if (Objects.equals(this.value, newValue)) {
            return;
        }
        this.value = newValue;
        this.isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        this.isDirty = false;
    }
}
