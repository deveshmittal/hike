package com.bsb.hike.voip;

import com.bsb.hike.utils.Logger;

public class SpeexDSPWrapper {
	
	private static long resampler = 0;

	private native long resampler_create(int inputRate, int outputRate, int quality);
	private native void resampler_destroy(long resampler);
	private native int resampler_resample(long resampler, byte[] input, int inputLength, byte[] output, int outputLength);
	
	static {
		System.loadLibrary("speexdsp");
	}
	
	public void createResampler(int inputRate, int outputRate, int quality) {
		resampler = resampler_create(inputRate, outputRate, quality);
		if (resampler == 0) {
			Logger.w(VoIPConstants.TAG, "Resampler create error.");
		}
	}
	
	public void destroyResampler() {
		if (resampler == 0) {
			Logger.w(VoIPConstants.TAG, "Resampler not created. Cannot destroy.");
			return;
		}
		
		resampler_destroy(resampler);
	}
	
	public void resample(byte[] input, int inputLength, byte[] output, int outputLength) {
		if (resampler == 0) {
			Logger.w(VoIPConstants.TAG, "Resampler not created.");
			return;
		}
		
		resampler_resample(resampler, input, inputLength, output, outputLength);
		
	}
	
}
