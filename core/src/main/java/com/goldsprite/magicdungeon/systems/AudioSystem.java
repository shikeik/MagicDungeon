package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon.assets.AudioAssets;
import com.goldsprite.magicdungeon.core.SettingsManager;

public class AudioSystem {

	private AssetManager assetManager;
	private Music currentMusic;
	private boolean isInitialized = false;
	
	private static AudioSystem instance;

	public static AudioSystem getInstance() {
		if (instance == null) {
			instance = new AudioSystem();
		}
		return instance;
	}

	private AudioSystem() {
		init();
	}

	public void init() {
		if (isInitialized) return;
		try {
			assetManager = new AssetManager();
			AudioAssets.loadAll(assetManager);
			assetManager.finishLoading();
			DLog.logT("AudioSystem", "Audio assets loaded.");
			isInitialized = true;
		} catch (Exception e) {
			Gdx.app.error("AudioSystem", "Init failed", e);
		}
	}

	public void dispose() {
		if (!isInitialized) return;
		stopBGM();
		if (assetManager != null) {
			assetManager.dispose();
		}
		isInitialized = false;
	}

	// --- Core Playback ---

	public void playSound(String path) {
		if (!isInitialized) return;
		if (assetManager.isLoaded(path)) {
			Sound sound = assetManager.get(path, Sound.class);
			float vol = SettingsManager.getInstance().getSfxVolume();
			// Add slight pitch variation for variety
			float pitch = 0.95f + MathUtils.random(0.1f);
			sound.play(vol, pitch, 0);
		}
	}

	public void playMusic(String path) {
		playMusic(path, true);
	}

	public void playMusic(String path, boolean loop) {
		if (!isInitialized) return;
		if (assetManager.isLoaded(path)) {
			if (currentMusic != null) {
				currentMusic.stop();
			}
			currentMusic = assetManager.get(path, Music.class);
			currentMusic.setLooping(loop);
			updateMusicVolume();
			currentMusic.play();
		}
	}

	public void stopBGM() {
		if (currentMusic != null) {
			currentMusic.stop();
		}
	}

	public void updateMusicVolume() {
		if (currentMusic != null) {
			currentMusic.setVolume(SettingsManager.getInstance().getMusicVolume());
		}
	}

	// --- Legacy Methods Mapped to New Assets ---

	/** 移动音效 */
	public void playMove() {
		// 移动太频繁，可以不播放声音，或者播放非常轻微的脚步声
		// playSound(AudioAssets.SFX_MENU_SELECT); 
	}

	/** 攻击音效 */
	public void playAttack() {
		playSound(AudioAssets.SFX_PUNCH);
	}

	/** 受击音效 */
	public void playHit() {
		playSound(AudioAssets.SFX_DEFEND);
	}

	/** 拾取音效 */
	public void playItem() {
		playSound(AudioAssets.SFX_DROP);
	}

	/** 升级/降层音效 */
	public void playLevelUp() {
		playSound(AudioAssets.SFX_POWER_UP);
	}

	/** 技能音效 */
	public void playSkill() {
		playSound(AudioAssets.SFX_PROTECT_CAST);
	}

	/** 游戏结束音效 */
	public void playGameOver() {
		stopBGM();
		playMusic(AudioAssets.MUSIC_DEFEAT, false);
	}
	
	// --- New Methods ---
	
	public void playMenuSelect() {
		playSound(AudioAssets.SFX_MENU_SELECT);
	}
	
	public void playMenuBack() {
		playSound(AudioAssets.SFX_MENU_BACK);
	}
	
	public void playChestOpen() {
		playSound(AudioAssets.SFX_CHEST_OPEN);
	}

}
