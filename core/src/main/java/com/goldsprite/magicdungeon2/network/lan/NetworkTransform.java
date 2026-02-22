package com.goldsprite.magicdungeon2.network.lan;

/**
 * 网络变换组件，封装了位置插值平滑逻辑。
 * 客户端收到服务器发来的位置快照时，不直接渲染最新位置，
 * 而是将渲染时间“倒退”一段延迟（比如 80ms），在过去和未来的快照之间进行线性插值（Lerp），
 * 从而得到平滑的移动轨迹。
 */
public class NetworkTransform {
    private LanRoomPlayer previous;
    private LanRoomPlayer current;
    
    private boolean interpolationEnabled = true;
    private long interpolationDelayMs = 80L;

    public NetworkTransform() {
    }

    public void pushSnapshot(LanRoomPlayer next) {
        if (this.current == null) {
            this.previous = next;
            this.current = next;
        } else if (next.getTimestamp() > this.current.getTimestamp()) {
            this.previous = this.current;
            this.current = next;
        }
    }

    public long getCurrentTimestamp() {
        return current == null ? 0L : current.getTimestamp();
    }

    /**
     * 获取插值后的位置
     * @param nowMs 当前系统时间（毫秒）
     * @return [x, y] 数组
     */
    public float[] getInterpolatedPosition(long nowMs) {
        if (current == null) return new float[]{0f, 0f};
        
        if (!interpolationEnabled) {
            return new float[]{current.getX(), current.getY()};
        }

        if (previous == null) return new float[]{current.getX(), current.getY()};

        long renderTime = nowMs - interpolationDelayMs;
        long prevTime = previous.getTimestamp();
        long currTime = current.getTimestamp();

        if (currTime <= prevTime) {
            return new float[]{current.getX(), current.getY()};
        }

        float t = (renderTime - prevTime) / (float) (currTime - prevTime);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        float x = previous.getX() + (current.getX() - previous.getX()) * t;
        float y = previous.getY() + (current.getY() - previous.getY()) * t;
        return new float[]{x, y};
    }

    /**
     * 获取最新收到的原始位置（无平滑）
     */
    public float[] getRawPosition() {
        if (current == null) return new float[]{0f, 0f};
        return new float[]{current.getX(), current.getY()};
    }

    public boolean isInterpolationEnabled() {
        return interpolationEnabled;
    }

    public void setInterpolationEnabled(boolean interpolationEnabled) {
        this.interpolationEnabled = interpolationEnabled;
    }

    public long getInterpolationDelayMs() {
        return interpolationDelayMs;
    }

    public void setInterpolationDelayMs(long interpolationDelayMs) {
        this.interpolationDelayMs = interpolationDelayMs;
    }
}
