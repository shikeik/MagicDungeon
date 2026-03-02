package com.goldsprite.gdengine.netcode;

import java.util.Objects;

/**
 * 自动状态同步变量 (类似 Unity NGO 的 NetworkVariable)。
 * 负责追踪数据的变化 (Dirty Flag)，只在发生改变时被动打成网络包。
 * <p>
 * 支持 <b>平滑调和模式</b>（Server Reconciliation）：
 * 当客户端使用 {@link #setLocal(Object)} 进行本地预测时，
 * 服务端状态到达后不再硬覆盖，而是通过插值平滑过渡到权威值，
 * 从根本上消除"被拉回/扯着走"的手感问题。
 * @param <T> 内部持有的数据类型
 */
public class NetworkVariable<T> {
    private T value;
    private boolean isDirty = true; // 默认初始化时必须为 dirty，这样才能进行初始同步

    // ── 平滑调和（Server Reconciliation）相关 ──
    /** 是否启用平滑调和模式（仅对 Float 类型变量生效） */
    private boolean smoothReconciliation = false;
    /** 服务端权威值（调和模式下，deserialize 写入此处而非直接覆盖 value） */
    private T serverAuthoritativeValue;
    /** 调和插值速度（每秒向权威值靠近的比例，0~1 之间；越大越快收敛，越小越平滑） */
    private float reconcileSpeed = 10f;
    /** 硬拉回阈值（当本地值与服务端差值超过此值时直接 snap，防止明显偏离） */
    private float snapThreshold = 50f;
    /** 调和容差（当差值小于此值时直接吸附，避免无限小幅抖动） */
    private float reconcileTolerance = 0.5f;
    /** 标记: 当前是否有未消化的服务端权威值（避免在无新数据时也做插值） */
    private boolean hasPendingAuthority = false;

    public NetworkVariable(T initialValue) {
        this.value = initialValue;
        this.serverAuthoritativeValue = initialValue;
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

    /**
     * 仅设置本地值，不触发脏标记（不会被网络同步广播）。
     * 典型用途: 客户端预测——本地立即更新，等待 Server 权威值通过调和机制平滑修正。
     */
    public void setLocal(T newValue) {
        this.value = newValue;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        this.isDirty = false;
    }

    // ── 平滑调和配置 API ──

    /**
     * 启用平滑调和模式（仅对 Float 类型变量有效，如坐标 x/y）。
     * 启用后，服务端状态到达时不再硬覆盖本地值，而是通过插值平滑过渡。
     * @param speed       调和速度（每秒收敛比例，推荐 8~15，越大越快但越生硬）
     * @param snapDist    硬拉回阈值（差值超过此值则直接 snap，推荐 30~100）
     * @param tolerance   容差（差值小于此值直接吸附，推荐 0.1~1.0）
     * @return this（支持链式调用）
     */
    public NetworkVariable<T> enableSmooth(float speed, float snapDist, float tolerance) {
        this.smoothReconciliation = true;
        this.reconcileSpeed = speed;
        this.snapThreshold = snapDist;
        this.reconcileTolerance = tolerance;
        return this;
    }

    /** 使用默认参数启用平滑调和 */
    public NetworkVariable<T> enableSmooth() {
        return enableSmooth(10f, 50f, 0.5f);
    }

    /** 禁用平滑调和（恢复硬覆盖行为） */
    public NetworkVariable<T> disableSmooth() {
        this.smoothReconciliation = false;
        return this;
    }

    /** 是否启用了平滑调和 */
    public boolean isSmoothEnabled() { return smoothReconciliation; }

    /** 获取服务端权威值（调和模式下可用于调试显示） */
    public T getServerValue() { return serverAuthoritativeValue; }

    /**
     * 每帧调用一次，驱动平滑调和插值。
     * 仅对启用了调和模式的 Float 类型变量生效。
     * @param delta 帧间隔（秒）
     */
    public void reconcileTick(float delta) {
        if (!smoothReconciliation || !hasPendingAuthority) return;
        if (!(value instanceof Float)) return;

        float current = (Float) value;
        float target = (Float) serverAuthoritativeValue;
        float diff = Math.abs(current - target);

        if (diff <= reconcileTolerance) {
            // 差距极小，直接吸附
            this.value = serverAuthoritativeValue;
            hasPendingAuthority = false;
            return;
        }

        if (diff > snapThreshold) {
            // 差距过大（可能是传送/复活），直接 snap
            this.value = serverAuthoritativeValue;
            hasPendingAuthority = false;
            return;
        }

        // 指数衰减插值: 每帧 current 向 target 靠近 (1 - e^(-speed*delta)) 的比例
        float alpha = 1f - (float) Math.exp(-reconcileSpeed * delta);
        float lerped = current + (target - current) * alpha;
        @SuppressWarnings("unchecked")
        T result = (T) Float.valueOf(lerped);
        this.value = result;
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
     * 从 ByteBuffer 按当前类型指引解析数据并覆盖。
     * <p>
     * 若启用了平滑调和模式（仅 Float），新值写入 {@code serverAuthoritativeValue}，
     * 由 {@link #reconcileTick(float)} 渐进地将 value 拉向权威值。
     * 否则直接覆盖 value（旧行为）。
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
        
        // 平滑调和模式：服务端值写入权威缓冲区，由 reconcileTick 驱动插值
        if (smoothReconciliation && value instanceof Float) {
            this.serverAuthoritativeValue = (T) newVal;
            this.hasPendingAuthority = true;
            return;
        }

        // 非调和模式：直接覆盖（旧行为，不激活脏标记，防止反射再广播回去）
        this.value = (T) newVal;
    }
}
