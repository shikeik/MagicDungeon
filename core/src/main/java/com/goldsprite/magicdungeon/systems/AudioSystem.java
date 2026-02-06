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

	// --- Convenience Methods (Legacy Support) ---

	public void playAttack() {
		playTone(150, WaveType.SAWTOOTH, 0.1f, 0.5f);
	}

	public void playHit() {
		playTone(100, WaveType.SQUARE, 0.2f, 0.5f);
	}

	public void playMove() {
		// Very quiet click
		// playTone(50, WaveType.SINE, 0.05f, 0.1f);
	}

	public void playItem() {
		// Play a simple 2-note sequence
		// Note 1
		playTone(600, WaveType.SINE, 0.3f, 0.3f);
		
		// Note 2 (Delayed via thread to avoid blocking main thread, but lightweight)
		new Thread(() -> {
			try { Thread.sleep(100); } catch (Exception e) {}
			playTone(800, WaveType.SINE, 0.3f, 0.3f);
		}).start();
	}

	public void playLevelUp() {
		// playTone(400, WaveType.SINE, 0.2f, 0.5f);
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

		@Override
		public void run() {
			while (running) {
				for (int i = 0; i < BUFFER_SIZE; i++) mixBuffer[i] = 0;

				// Mixing Voices
				Iterator<Voice> it = activeVoices.iterator();
				boolean hasSound = false;
				while (it.hasNext()) {
					Voice v = it.next();
					boolean alive = v.process(mixBuffer, BUFFER_SIZE);
					if (!alive) it.remove();
					else hasSound = true;
				}

				// Output
				if (hasSound) {
					for (int i = 0; i < BUFFER_SIZE; i++) {
						float val = mixBuffer[i];
						if (val > 1f) val = 1f; if (val < -1f) val = -1f;
						outBuffer[i] = (short) (val * 32767);
					}
					device.writeSamples(outBuffer, 0, BUFFER_SIZE);
				} else {
					// Output silence or sleep to save CPU when no sound
					// Writing silence is better to keep audio device buffer fed?
					// But sleep is fine if latency isn't critical for "silence".
					// SynthAudio used sleep(10).
					try { Thread.sleep(10); } catch (InterruptedException e) {}
				}
			}
		}
	}
}
