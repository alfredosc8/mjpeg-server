package com.vzhilin;

public class FrequesncyCounter {
	private long prev = -1;
	private float factor;
	private float fps = 0;
	
	public FrequesncyCounter() {
		factor = 0.9f;
	}
	
	public void tick(long time) {
		if (prev != -1) {
			long delta = time - prev;
			
			if (delta != 0) {
				float v = 1000.0f / delta;
				fps = v * factor + (1 - factor) * fps;
			}
		}
		
		prev = time;
	}
	
	public float get() {
		return fps;
	}
}
