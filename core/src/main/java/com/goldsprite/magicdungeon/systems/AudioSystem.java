package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioSystem {

	public enum WaveType { SINE, SQUARE, SAWTOOTH, TRIANGLE, NOISE }

	private static final int SAMPLE_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;

	private AudioDevice device;
	private AudioThread audioThread;
	private boolean isInitialized = false;

	private final Queue<Voice> activeVoices = new ConcurrentLinkedQueue<>();

	public AudioSystem() {
		init();
	}

	public void init() {
		if (isInitialized) return;
		try {
			device = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
			audioThread = new AudioThread();
			audioThread.start();
			isInitialized = true;
		} catch (Exception e) {
			Gdx.app.error("AudioSystem", "Init failed", e);
		}
	}

	public void dispose() {
		if (!isInitialized) return;
		audioThread.running = false;
		try { audioThread.join(500); } catch (InterruptedException ignored) {}
		if(device != null) device.dispose();
		isInitialized = false;
	}

	public void playTone(float freq, WaveType type, float duration, float vol) {
		playTone(freq, type, duration, vol, 0);
	}

	public void playTone(float freq, WaveType type, float duration, float vol, float slideFreq) {
		if (!isInitialized) return;
		activeVoices.add(new Voice(freq, type, duration, vol, slideFreq));
	}

	// BGM 控制开关
	private volatile boolean bgmEnabled = false;

	public void playBGM() { bgmEnabled = true; }
	public void stopBGM() { bgmEnabled = false; }

	// --- Convenience Methods (Legacy Support) ---

	/** 移动音效: 极短促的脚步声 (白噪) */
	public void playMove() {
		// 0.02s 线性衰减噪音
		playTone(0, WaveType.NOISE, 0.02f, 0.3f);
	}

	/** 攻击音效: 快速下行的锯齿波 */
	public void playAttack() {
		// 150Hz -> 50Hz, 0.1s
		playTone(150, WaveType.SAWTOOTH, 0.1f, 0.5f, 50);
	}

	/** 受击音效: 方波与噪音混合 */
	public void playHit() {
		// 强烈的频率抖动，模拟受击反馈
		playTone(100, WaveType.SQUARE, 0.15f, 0.6f, 50);
		playTone(0, WaveType.NOISE, 0.1f, 0.4f);
	}

	/** 拾取音效: 快速上升的音阶 */
	public void playItem() {
		// 400Hz -> 800Hz 正弦波，清脆明快
		playTone(400, WaveType.SINE, 0.15f, 0.4f, 800);
	}

	/** 升级/降层音效: 辉煌的二部和弦 (纯五度) */
	public void playLevelUp() {
		playTone(440, WaveType.SINE, 1.5f, 0.4f); // A4
		playTone(660, WaveType.SINE, 1.5f, 0.4f); // E5 (纯五度)
	}

	/** 技能音效: 带有扫频效果的长音 */
	public void playSkill() {
		// 正弦波频率从低到高再回到中音 (简化为向上扫频然后手动添加第二段?)
		// 这里简化为一段明显的长扫频
		playTone(200, WaveType.SINE, 0.5f, 0.5f, 600);
	}

	// --- Inner Classes from SynthAudio ---

	private static class Voice {
		float freqStart, freqEnd;
		WaveType type;
		float duration;
		float volume;
		float currentSample = 0;
		float phase = 0;

		public Voice(float freq, WaveType type, float durationSec, float vol, float slideFreq) {
			this.freqStart = freq;
			this.freqEnd = (slideFreq <= 0) ? freq : slideFreq;
			this.type = type;
			this.duration = durationSec * SAMPLE_RATE;
			this.volume = vol;
		}

		public boolean process(float[] mixBuffer, int len) {
			for (int i = 0; i < len; i++) {
				if (currentSample >= duration) return false;
				float t = currentSample / duration;
				float currFreq = freqStart + (freqEnd - freqStart) * t;
				// Linear Decay Envelope
				float currVol = volume * (1f - t);

				float phaseIncrement = (float) (Math.PI * 2 * currFreq / SAMPLE_RATE);
				phase += phaseIncrement;
				if (phase > Math.PI * 2) phase -= Math.PI * 2;

				float sampleValue = 0;
				switch (type) {
					case SINE: sampleValue = (float) Math.sin(phase); break;
					case SQUARE: sampleValue = (phase < Math.PI) ? 1f : -1f; break;
					case SAWTOOTH: sampleValue = (float) (phase / Math.PI - 1f); break;
					case TRIANGLE:
						float raw = (float) (phase / Math.PI - 1f);
						sampleValue = 2f * (0.5f - Math.abs(raw));
						break;
					case NOISE: sampleValue = MathUtils.random(-1f, 1f); break;
				}
				mixBuffer[i] += sampleValue * currVol;
				currentSample++;
			}
			return true;
		}
	}

	private class AudioThread extends Thread {
		volatile boolean running = true;
		float[] mixBuffer = new float[BUFFER_SIZE];
		short[] outBuffer = new short[BUFFER_SIZE];

		// BGM 状态机
		int bpm = 80; // 较慢的速度，营造压抑感
		int samplesPerBeat = (SAMPLE_RATE * 60) / (bpm * 4); // 16分音符
		long sampleCounter = 0;
		int step = 0; // 0-15

		// 小调五声 (C Minor Pentatonic): C, Eb, F, G, Bb
		float[] melodyScale = { 261.63f, 311.13f, 349.23f, 392.00f, 466.16f }; 

		@Override
		public void run() {
			while (running) {
				for (int i = 0; i < BUFFER_SIZE; i++) mixBuffer[i] = 0;

				// --- 1. BGM Sequencer Logic ---
				if (bgmEnabled) {
					sampleCounter += BUFFER_SIZE;
					if (sampleCounter >= samplesPerBeat) {
						sampleCounter -= samplesPerBeat;
						triggerBeat(step);
						step = (step + 1) % 16;
					}
				}

				// --- 2. Mixing Voices ---
				Iterator<Voice> it = activeVoices.iterator();
				boolean hasSound = false;
				while (it.hasNext()) {
					Voice v = it.next();
					boolean alive = v.process(mixBuffer, BUFFER_SIZE);
					if (!alive) it.remove();
					else hasSound = true;
				}

				// --- 3. Output ---
				if (hasSound) {
					for (int i = 0; i < BUFFER_SIZE; i++) {
						float val = mixBuffer[i];
						if (val > 1f) val = 1f; if (val < -1f) val = -1f;
						outBuffer[i] = (short) (val * 32767);
					}
					device.writeSamples(outBuffer, 0, BUFFER_SIZE);
				} else {
					try { Thread.sleep(10); } catch (InterruptedException e) {}
				}
			}
		}

		private void triggerBeat(int s) {
			// 1. Pulse (心跳律动): 每拍一次
			if (s % 4 == 0) {
				// 极低频正弦波
				activeVoices.add(new Voice(60, WaveType.SINE, 0.15f, 0.4f, 40));
			}

			// 2. Drone (环境底噪): 长音持续
			// 每小节触发一次 (16步)
			if (s == 0) {
				// 两个接近频率产生拍频，低音量
				activeVoices.add(new Voice(110, WaveType.TRIANGLE, 4.0f, 0.15f, 0));
				activeVoices.add(new Voice(112, WaveType.TRIANGLE, 4.0f, 0.15f, 0));
			}

			// 3. Eerie Melody (随机旋律): 随机触发
			if (s % 2 == 0 && MathUtils.randomBoolean(0.25f)) {
				float note = melodyScale[MathUtils.random(melodyScale.length - 1)];
				// 方波，带一点滑音效果
				activeVoices.add(new Voice(note, WaveType.SQUARE, 0.3f, 0.15f, note - 5));
				
				// 简单的回声 (Delay)
				new Thread(() -> {
					try { Thread.sleep(300); } catch (Exception e) {}
					activeVoices.add(new Voice(note, WaveType.SQUARE, 0.3f, 0.08f, note - 5));
				}).start();
			}
		}
	}
}
