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

    /**
     * 将本变量包装到 ByteBuffer 中发送
     */
    public void serialize(NetBuffer buffer) {
        if (value instanceof Float) {
            buffer.writeFloat((Float) value);
        } else if (value instanceof Integer) {
            buffer.writeInt((Integer) value);
        } else if (value instanceof Boolean) {
            buffer.writeBoolean((Boolean) value);
        } else if (value instanceof String) {
            buffer.writeString((String) value);
        } else if (value instanceof com.badlogic.gdx.graphics.Color) {
            buffer.writeInt(com.badlogic.gdx.graphics.Color.rgba8888((com.badlogic.gdx.graphics.Color) value));
        } else {
            throw new IllegalArgumentException("NetworkVariable 暂不支持此类型序列化: " + (value != null ? value.getClass() : "null"));
        }
    }

    /**
     * 从 ByteBuffer 按当前类型指引解析数据并覆盖，且不触发脏标记
     */
    @SuppressWarnings("unchecked")
    public void deserialize(NetBuffer buffer) {
        Object newVal = null;
        if (value instanceof Float) {
            newVal = buffer.readFloat();
        } else if (value instanceof Integer) {
            newVal = buffer.readInt();
        } else if (value instanceof Boolean) {
            newVal = buffer.readBoolean();
        } else if (value instanceof String) {
            newVal = buffer.readString();
        } else if (value instanceof com.badlogic.gdx.graphics.Color) {
            newVal = new com.badlogic.gdx.graphics.Color(buffer.readInt());
        } else {
            throw new IllegalArgumentException("NetworkVariable 暂不支持此类型反序列化: " + (value != null ? value.getClass() : "null"));
        }
        
        // 我们在接收自远端覆盖时，不能激活脏标记（防止反射再广播回去）
        this.value = (T) newVal;
    }
}
