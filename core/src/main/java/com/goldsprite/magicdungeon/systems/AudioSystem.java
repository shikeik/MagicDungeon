package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;

public class AudioSystem {

	public void playTone(float freq, String type, float duration) {
		new Thread(() -> {
			AudioDevice device = Gdx.audio.newAudioDevice(44100, true);
			int samples = (int) (44100 * duration);
			float[] buffer = new float[samples];

			for (int i = 0; i < samples; i++) {
				float t = (float) i / 44100.0f;
				float wave = 0;

				if (type.equals("sine")) {
					wave = MathUtils.sin(2 * MathUtils.PI * freq * t);
				} else if (type.equals("square")) {
					wave = MathUtils.sin(2 * MathUtils.PI * freq * t) > 0 ? 1 : -1;
				} else if (type.equals("sawtooth")) {
					wave = (float) ((2 * (t * freq - Math.floor(t * freq + 0.5))) * 2); // Approx
				}

				// Simple envelope (Attack/Decay)
				float vol = 0.5f;
				if (i > samples - 1000) {
					vol *= (samples - i) / 1000.0f;
				}

				buffer[i] = wave * vol;
			}

			device.writeSamples(buffer, 0, samples);
			device.dispose();
		}).start();
	}

	public void playAttack() {
		playTone(150, "sawtooth", 0.1f);
	}

	public void playHit() {
		playTone(100, "square", 0.2f);
	}

	public void playMove() {
		// Very quiet click
		// playTone(50, "sine", 0.05f);
	}

	public void playItem() {
		new Thread(() -> {
			playTone(600, "sine", 0.3f);
			try { Thread.sleep(100); } catch (Exception e) {}
			playTone(800, "sine", 0.3f);
		}).start();
	}

	public void playLevelUp() {
		// playTone(400, "sine", 0.2); ...
	}
}
