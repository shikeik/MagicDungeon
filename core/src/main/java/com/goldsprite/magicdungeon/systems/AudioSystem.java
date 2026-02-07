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

	/** 移动音效: 柔和的低频脉冲 */
	public void playMove() {
		// 提高一点音量和频率，确保可听见但不过分
		// 80Hz -> 100Hz, 0.05s -> 0.08s, vol 0.2 -> 0.4
		playTone(100, WaveType.SINE, 0.08f, 0.4f, 50);
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

		public float getSample() {
			if (currentSample >= duration) return 0;
			
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
			currentSample++;
			return sampleValue * currVol;
		}

		// Legacy block process removed in favor of per-sample mixing for precision
		public boolean process(float[] mixBuffer, int len) {
			return false; 
		}
	}

	private class AudioThread extends Thread {
		volatile boolean running = true;
		float[] mixBuffer = new float[BUFFER_SIZE];
		short[] outBuffer = new short[BUFFER_SIZE];

		// BGM 状态机
		int bpm = 110; // 提高BGM速度，增强动感 (Cyberpunk/Synthwave 常用速度)
		
		// 采样级计数器需要更精确
		// SAMPLE_RATE = 44100
		// Beat Duration = 60 / BPM
		// 16th Note Duration = Beat Duration / 4 = 15 / BPM
		// Samples Per 16th = SAMPLE_RATE * 15 / BPM
		// 对于 110 BPM: 44100 * 15 / 110 ≈ 6013.63
		// 使用 float 累加器以保持长期精度，避免整数截断误差累积
		float samplesPerStep = (SAMPLE_RATE * 15f) / bpm;
		float sampleCounter = 0;
		int step = 0; // 0-15

		// C Minor Scale (C小调): C3, Eb3, F3, G3, Bb3 (低八度，减少刺耳感)
		float[] melodyScale = { 130.81f, 155.56f, 174.61f, 196.00f, 233.08f }; 
		// Bass Scale: C2, Eb2, F2, G2
		float[] bassScale = { 65.41f, 77.78f, 87.31f, 98.00f };

		@Override
		public void run() {
			// 设置线程优先级为最高，以减少系统调度带来的延迟抖动
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			
			while (running) {
				// --- 1. BGM Sequencer Logic (Sample Precise) ---
				// 移动到混音循环内部以获得采样级精度
				
				for (int i = 0; i < BUFFER_SIZE; i++) {
					mixBuffer[i] = 0;
					
					// BGM Trigger Check per Sample
					if (bgmEnabled) {
						sampleCounter++;
						if (sampleCounter >= samplesPerStep) {
							sampleCounter -= samplesPerStep;
							triggerBeat(step);
							step = (step + 1) % 16;
						}
					}
					
					// Mixing Voices per Sample
					Iterator<Voice> it = activeVoices.iterator();
					while (it.hasNext()) {
						Voice v = it.next();
						if (v.currentSample < v.duration) {
							float val = v.getSample(); // 获取单采样
							mixBuffer[i] += val;
						} else {
							it.remove();
						}
					}
				}

				// --- 3. Output ---
				// 简单的限幅处理
				boolean hasSound = false;
				for (int i = 0; i < BUFFER_SIZE; i++) {
					float val = mixBuffer[i];
					if (Math.abs(val) > 0.001f) hasSound = true;
					if (val > 1f) val = 1f; if (val < -1f) val = -1f;
					outBuffer[i] = (short) (val * 32767);
				}
				
				// 关键修复：始终写入音频数据，无论是否有声音
				// 这确保了音频设备的缓冲区始终被填满，维持稳定的时钟
				// 避免在静音时暂停写入导致的后续写入延迟
				device.writeSamples(outBuffer, 0, BUFFER_SIZE);
			}
		}

		private void triggerBeat(int s) {
			// 1. Kick Drum (底鼓): 更有力的低频冲击
			// 4/4拍强拍: 0, 4, 8, 12
			if (s % 4 == 0) {
				// 快速下潜的正弦波，模拟底鼓
				activeVoices.add(new Voice(150, WaveType.SINE, 0.15f, 0.7f, 10));
			}

			// 2. Hi-Hat (踩镲): 闭镲，增加律动
			// 反拍: 2, 6, 10, 14
			if (s % 4 == 2) {
				// 极短的噪音，音量降低防止刺耳
				activeVoices.add(new Voice(0, WaveType.NOISE, 0.03f, 0.15f, 0));
			}
			// 16分音符弱拍点缀: 奇数位
			if (s % 2 == 1 && MathUtils.randomBoolean(0.5f)) {
				activeVoices.add(new Voice(0, WaveType.NOISE, 0.01f, 0.1f, 0));
			}

			// 3. Bassline (贝斯): 16分音符驱动，锯齿波低通滤波效果(模拟)
			// 逻辑: 每一拍变换一个音，或者持续律动
			if (s % 2 == 0) { // 8分音符
				float note = bassScale[MathUtils.random(bassScale.length - 1)];
				// 使用三角波代替锯齿波，声音更圆润厚实，不刺耳
				activeVoices.add(new Voice(note, WaveType.TRIANGLE, 0.15f, 0.4f, 0));
				// 叠加一个低八度正弦波增强厚度
				activeVoices.add(new Voice(note / 2, WaveType.SINE, 0.15f, 0.5f, 0));
			}

			// 4. Lead Melody (主旋律): 减少随机性，增加回声感
			// 使用正弦波代替方波，更加空灵
			if (s % 8 == 0 || (s % 8 == 6 && MathUtils.randomBoolean(0.4f))) {
				float note = melodyScale[MathUtils.random(melodyScale.length - 1)];
				// 正弦波 + 慢速Attack (模拟Pad音色)
				activeVoices.add(new Voice(note, WaveType.SINE, 0.4f, 0.3f, 0));
				
				// Delay/Echo
				new Thread(() -> {
					try { Thread.sleep(250); } catch (Exception e) {}
					activeVoices.add(new Voice(note, WaveType.SINE, 0.4f, 0.15f, 0));
				}).start();
			}

			// 5. Arp (琶音点缀): 极高频，极低音量，增加空间感
			if (MathUtils.randomBoolean(0.1f)) {
				float note = melodyScale[MathUtils.random(melodyScale.length - 1)] * 2;
				activeVoices.add(new Voice(note, WaveType.SINE, 0.1f, 0.1f, 0));
			}
		}
	}
}
