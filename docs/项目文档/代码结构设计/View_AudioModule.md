# 视觉与音频模块: View_AudioModule (Sound & Music)

## 1. 模块职责
负责游戏内所有声音资源的加载、播放、音量控制以及音效管理 (SFX & BGM)。

## 2. 核心功能

### 2.1 音效 (SFX)
*   **战斗音效**:
    *   `WeaponSwing`: 挥剑声。
    *   `HitFlesh`: 击中肉体声。
    *   `HitMetal`: 击中盔甲声 (Miss/Block)。
    *   `CastMagic`: 施法声。
*   **环境音效**:
    *   `Footstep`: 脚步声 (根据地形材质变化)。
    *   `DoorOpen`: 开门声。
    *   `ChestOpen`: 开箱声。
*   **UI 音效**:
    *   `Click`: 按钮点击。
    *   `Equip`: 穿戴装备。
    *   `Error`: 操作无效 (如MP不足)。

### 2.2 背景音乐 (BGM)
*   **循环播放**: 根据当前层级或环境切换。
    *   `Theme_Title`: 主菜单。
    *   `Theme_Dungeon_Low`: 低层 (1-10层) 轻松诡异。
    *   `Theme_Dungeon_High`: 高层 (11-20层) 紧张激烈。
    *   `Theme_Boss`: Boss战激昂。
*   **过渡**: 音乐淡入淡出 (Crossfade) 避免突兀切换。

## 3. 音频管理 (Manager)
*   **资源池**: 预加载常用音效，流式加载长音乐。
*   **音量控制**: 
    *   `MasterVolume`: 主音量。
    *   `MusicVolume`: 音乐音量。
    *   `SoundVolume`: 音效音量。
*   **空间音效 (Optional)**: 简单的左右声道偏移 (根据声音源相对于屏幕中心的位置)。

## 4. 接口设计 (API Draft)

```java
interface IAudioManager {
    // 播放控制
    void playSound(String soundName);
    void playSound(String soundName, float pitch); // 音调随机化，增加丰富度
    void playMusic(String musicName, boolean loop);
    void stopMusic();
    
    // 设置
    void setMasterVolume(float vol);
    void setMusicVolume(float vol);
    void setSoundVolume(float vol);
    
    // 资源管理
    void loadAudioResources();
    void dispose();
}
```
