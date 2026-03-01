package com.goldsprite.gdengine.netcode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 联机引擎核心序列化缓冲器。
 * 用于将各种网络变量、实体ID压缩成高度紧凑的字节数组 (byte[])，
 * 以便 Transport 层可以通过 UDP/TCP 发送。同时也负责解析收到的封包。
 */
public class NetBuffer {
    private ByteBuffer buffer;

    /**
     * 作为发送端：开辟写入缓冲区
     */
    public NetBuffer() {
        // 默认分配1024字节（1KB），实际商业引擎会用对象池复用，沙盒暂且直接分配
        buffer = ByteBuffer.allocate(1024);
    }

    /**
     * 作为接收端：将收到的字节流包装进来准备读取
     */
    public NetBuffer(byte[] data) {
        buffer = ByteBuffer.wrap(data);
    }

    public void writeInt(int value) {
        buffer.putInt(value);
    }

    public int readInt() {
        return buffer.getInt();
    }

    public void writeFloat(float value) {
        buffer.putFloat(value);
    }

    public float readFloat() {
        return buffer.getFloat();
    }

    public void writeBoolean(boolean value) {
        buffer.put((byte) (value ? 1 : 0));
    }

    public boolean readBoolean() {
        return buffer.get() == 1;
    }

    public void writeString(String value) {
        if (value == null) {
            value = "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    public String readString() {
        int len = buffer.getInt();
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 将装配好的数据翻转并导出为紧凑的 byte 数组，以便底层网络层发送
     */
    public byte[] toByteArray() {
        int positionResult = buffer.position();
        byte[] result = new byte[positionResult];
        
        // 临时备份状态以便读取，读出刚刚装入的字节
        ByteBuffer readCopy = buffer.duplicate();
        readCopy.position(0);
        readCopy.get(result, 0, positionResult);
        
        return result;
    }
}
