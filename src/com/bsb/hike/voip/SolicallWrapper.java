package com.bsb.hike.voip;

import com.bsb.hike.utils.Logger;

public class SolicallWrapper {
	
	private native int packageInit();
	private native int AECInit(int CpuNR, int CpuAEC, short AECMinOutputPercentageDuringEcho, 
			short AECTypeParam, short ComfortNoisePercent);
	private native int processSpeakerFrame(byte[] stream);
	private native int processMicFrame(byte[] input, byte[] output);
	private native int terminate();
	
	public static final int SOLICALL_FRAME_SIZE = 960; 

	static {
		System.loadLibrary("solicall");
	}
	
	public void init() {
		int init = packageInit();
		Logger.d(VoIPConstants.TAG, "AEC packageInit: " + init);

		init = AECInit(2, 2, (short)100, (short)8, (short)100);
		Logger.d(VoIPConstants.TAG, "AEC init: " + init);
	}
	
	public void destroy() {
		int ret = terminate();
		Logger.d(VoIPConstants.TAG, "AEC terminate: " + ret);
	}
	
	public void processSpeaker(byte[] frame) {
		int ret = 0;
		synchronized (this) {
			ret = processSpeakerFrame(frame);
		}
	}
	
	public int processMic(byte[] frame) {
		int ret = 0;
		synchronized (this) {
			ret = processMicFrame(frame, null);
		}
		return ret;
	}
}
